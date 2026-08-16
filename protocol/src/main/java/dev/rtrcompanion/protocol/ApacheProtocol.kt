package dev.rtrcompanion.protocol

import timber.log.Timber

/**
 * Protocol parser for the TVS Apache RTR 310 SmartXonnect BLE protocol.
 *
 * ## Responsibilities
 *  - Parse raw 20-byte notification packets into [ApacheFrame] structures
 *  - Validate packet length and terminator byte
 *  - Verify per-message-type checksums (where constants are confirmed)
 *  - Log validation warnings for packets that fail structural checks
 *
 * ## What this class does NOT do
 *  - Decode individual payload fields (Sprint 5+)
 *  - Write to CHAR_WRITE (that is `RtrGattManager`'s responsibility)
 *  - Make decisions about authentication or keep-alive
 *
 * ## Safety rule
 * Fields are extracted only from byte positions confirmed in `docs/BLE-Protocol.md`.
 * No field decoding is added without documented, hardware-verified evidence.
 *
 * ## Checksum formula
 * `checksum = (C − sum(B0..B17)) mod 256`
 *
 * C values confirmed from RTR 310 shutdown capture (2026-08-08):
 * | Msg ID | C    | Evidence |
 * |--------|------|----------|
 * | 0x10   | 0x31 | Single sample; consistent |
 * | 0x11   | 0xC3 | Cross-verified (2 variants) |
 * | 0x12   | 0x0B | Single sample; consistent |
 * | 0x7D   | 0x99 | Single sample; consistent |
 * | 0x42   | 0x34 | Single sample; consistent |
 * | 0x5F   | N/A  | UNRESOLVED — frame counter disrupts formula |
 *
 * Source: `docs/protocol/capture-20260808-150945.md`, `docs/KNOWN_FACTS.md`
 */
object ApacheProtocol {

    // -------------------------------------------------------------------------
    // Structural constants
    // -------------------------------------------------------------------------

    /** Every valid Apache BLE packet is exactly this many bytes. */
    const val PACKET_LENGTH = 20

    /** Byte index of the frame type / start byte. */
    const val IDX_FRAME_TYPE = 0

    /** Byte index of the message ID. */
    const val IDX_MSG_ID = 1

    /** First byte index of the payload region. */
    const val IDX_PAYLOAD_START = 2

    /** Last byte index of the payload region (inclusive). */
    const val IDX_PAYLOAD_END = 17

    /** Byte index of the checksum byte. */
    const val IDX_CHECKSUM = 18

    /** Byte index of the terminator byte. */
    const val IDX_TERMINATOR = 19

    /** Expected value of the terminator byte in every valid packet. */
    const val TERMINATOR_VALUE: Byte = 0xFF.toByte()

    /** Empty/null field value used in inbound (Bike → Phone) packets. */
    const val INBOUND_NULL: Byte = 0xEA.toByte()

    // -------------------------------------------------------------------------
    // Per-message-type checksum constants (confirmed from hardware capture)
    // -------------------------------------------------------------------------

    /**
     * Checksum constant C for each confirmed message ID.
     *
     * Only IDs with hardware-verified C values are listed.
     * `null` means the checksum formula is unresolved for this message type.
     *
     * Formula: `checksum = (C − sum(B0..B17)) mod 256`
     * Derivation: `C = (checksum_byte + sum_B0_17) mod 256`
     *
     * Sources:
     *  - `captures/rtr-capture-20260808-150945.txt` (21 packets, single-sample)
     *  - `captures/rtr-capture-20260816-btsnoop.txt` (733 packets, **authoritative**)
     *
     * The 2026-08-16 btsnoop capture provides 351 samples for 0x10 and 0x12,
     * making those constants highly reliable. The 2026-08-08 single-sample
     * values were wrong for 0x10/0x11/0x12/0x42 — corrected here.
     * 0x7D is consistent across both captures (C=0x29).
     */
    private val CHECKSUM_CONSTANTS: Map<Byte, Int> = mapOf(
        0x10.toByte() to 0x49,   // ApacheSpeedOMeter1 — 351 samples (btsnoop 2026-08-16)
        0x11.toByte() to 0xDD,   // ApacheSpeedOMeter2 — 5 samples (btsnoop 2026-08-16)
        0x12.toByte() to 0x59,   // ApacheSpeedOMeter3 — 351 samples (btsnoop 2026-08-16)
        0x7D.toByte() to 0x29,   // Device identity    — consistent across both captures
        0x42.toByte() to 0x82,   // Heartbeat          — 5 samples (btsnoop 2026-08-16)
        // 0x5F: Two variants with frame counter B7=0x1E/0x1F yield C=0xF5/0x11.
        //        Not a fixed constant — frame counter feeds the checksum.
        //        Status: UNRESOLVED. Needs live-ride capture with more B7 values.
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses a [RawPacket] into an [ApachePacket].
     *
     * If the packet passes structural validation (length, terminator), an
     * [ApacheFrame] is constructed and attached. If validation fails, the
     * returned [ApachePacket] will have `frame == null`.
     *
     * Checksum validation is advisory — a bad checksum is logged as a warning
     * but does not prevent the frame from being constructed.
     *
     * @param raw    A raw notification packet from CHAR_NOTIFY.
     * @param direction Whether this packet was received (RX) or transmitted (TX).
     * @return An [ApachePacket] with either a parsed frame or null if malformed.
     */
    fun parseFrame(
        raw: RawPacket,
        direction: ApacheFrame.Direction = ApacheFrame.Direction.RX,
    ): ApachePacket {
        val bytes = raw.bytes

        // Structural validation: length
        if (bytes.size != PACKET_LENGTH) {
            Timber.w(
                "[PROTOCOL] Unexpected packet length: expected %d, got %d — %s",
                PACKET_LENGTH, bytes.size, raw.hex
            )
            return ApachePacket(raw, frame = null)
        }

        // Structural validation: terminator byte
        if (bytes[IDX_TERMINATOR] != TERMINATOR_VALUE) {
            Timber.w(
                "[PROTOCOL] Invalid terminator 0x%02X at B19 — expected 0xFF — %s",
                bytes[IDX_TERMINATOR], raw.hex
            )
            return ApachePacket(raw, frame = null)
        }

        val frameType = bytes[IDX_FRAME_TYPE]
        val msgId = bytes[IDX_MSG_ID]

        // Advisory: checksum verification (does not block parsing)
        val checksumResult = verifyChecksum(bytes, msgId)
        if (checksumResult == ChecksumResult.FAIL) {
            Timber.w(
                "[PROTOCOL] Checksum mismatch for msg 0x%02X — %s",
                msgId, raw.hex
            )
        }

        val frame = ApacheFrame(
            frameType = frameType,
            id = msgId,
            raw = bytes,
            timestamp = raw.timestamp,
            direction = direction,
        )

        return ApachePacket(raw, frame)
    }

    /**
     * Extracts the message ID (byte 1) from a raw byte array without full parsing.
     *
     * Useful for quick classification without allocating an [ApachePacket].
     *
     * @param bytes Raw packet bytes.
     * @return The message ID byte, or null if the packet is too short.
     */
    fun extractMessageId(bytes: ByteArray): Byte? =
        if (bytes.size > IDX_MSG_ID) bytes[IDX_MSG_ID] else null

    /**
     * Returns a short label string for a message ID, suitable for log output.
     *
     * Unknown IDs are displayed as `"0xXX"`.
     */
    fun messageLabel(msgId: Byte): String = when (msgId) {
        0x10.toByte() -> "0x10 SpeedOMeter1"
        0x11.toByte() -> "0x11 SpeedOMeter2"
        0x12.toByte() -> "0x12 SpeedOMeter3"
        0x16.toByte() -> "0x16 LapTiming"
        0x18.toByte() -> "0x18 EngineDiag"
        0x19.toByte() -> "0x19 Economy"
        0x29.toByte() -> "0x29 WifiPassword"
        0x42.toByte() -> "0x42 Heartbeat"
        0x4A.toByte() -> "0x4A Ping (TX)"
        0x5F.toByte() -> "0x5F LiveTelemetry"
        0x7D.toByte() -> "0x7D DeviceIdentity"
        0xF1.toByte() -> "0xF1 AuthResponse (TX)"
        0xF2.toByte() -> "0xF2 AuthChallenge"
        else          -> "0x%02X UNKNOWN".format(msgId)
    }

    /**
     * Verifies the checksum at byte 18 of the packet.
     *
     * Formula: `(C − sum(B0..B17)) mod 256`
     *
     * @return [ChecksumResult.PASS] if verified, [ChecksumResult.FAIL] if mismatch,
     *         [ChecksumResult.UNKNOWN] if no C constant is available for this message ID.
     */
    fun verifyChecksum(bytes: ByteArray, msgId: Byte): ChecksumResult {
        val c = CHECKSUM_CONSTANTS[msgId] ?: return ChecksumResult.UNKNOWN
        val sum = bytes.slice(0..17).sumOf { it.toInt() and 0xFF }
        val expected = ((c - sum) and 0xFF).toByte()
        return if (bytes[IDX_CHECKSUM] == expected) ChecksumResult.PASS else ChecksumResult.FAIL
    }

    /**
     * Computes the expected checksum byte for an outbound packet using the
     * Jupiter formula: `255 − (sum(B0..B17) mod 256)`.
     *
     * **Note:** This formula is for *outbound* packets (Phone → Bike).
     * Inbound checksum constants differ per message type — use [verifyChecksum].
     *
     * Source: Jupiter RE documentation. Unverified for RTR 310 outbound packets.
     */
    fun computeOutboundChecksum(bytes: ByteArray): Byte {
        require(bytes.size >= 18) { "Packet must be at least 18 bytes to compute checksum" }
        val sum = bytes.slice(0..17).sumOf { it.toInt() and 0xFF }
        return (255 - (sum % 256)).toByte()
    }

    // -------------------------------------------------------------------------
    // Result types
    // -------------------------------------------------------------------------

    /**
     * Result of a checksum verification attempt.
     */
    enum class ChecksumResult {
        /** Checksum matches the expected value for this message type. */
        PASS,
        /** Checksum does not match — possible corruption or wrong formula. */
        FAIL,
        /** No C constant is available for this message ID — cannot verify. */
        UNKNOWN,
    }
}
