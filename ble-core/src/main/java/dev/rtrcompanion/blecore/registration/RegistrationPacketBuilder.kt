package dev.rtrcompanion.blecore.registration

import dev.rtrcompanion.blecore.BleConstants

/**
 * Builds the registration packets that identify the phone app to the bike cluster.
 *
 * ## What these packets do
 *
 * These are **display-only** packets. They tell the cluster what name to show
 * for the connected phone/user and the vehicle profile. They have no effect on
 * engine control, safety systems, or any hardware state.
 *
 * The cluster will display the names you send. If you send wrong data, the
 * cluster shows wrong text — disconnect and it reverts immediately.
 *
 * ## Protocol (confirmed from btsnoop HCI capture, 2026-08-16)
 *
 * Observed sequence after CCCD notification enable:
 * ```
 * 1. Write 0x52 — user display name
 * 2. Write 0x43 — vehicle display name (sent multiple times)
 * 3. Begin 0x4A ping loop (every 2 seconds)
 * 4. Bike begins streaming telemetry
 * ```
 *
 * ## Encoding
 *
 * All bytes (including header bytes 0 and 1) are XOR-encoded with `0xEA`
 * before transmission. The terminator `0xFF` is written as-is (0xFF XOR 0xEA = 0x15,
 * but the observed packets show `0xFF` raw at byte 19 — the terminator is NOT XOR'd).
 *
 * Actually: looking at the real capture bytes:
 * - Raw `0xB1` XOR `0xEA` = `0x5B` = FRAME_CONTROL ✓
 * - Raw `0xA0` XOR `0xEA` = `0x4A` = MSG_PING ✓
 * - Raw packet ends with `0xFF` which is `0x15` XOR `0xEA`... but observed as `0xFF`
 *
 * Wait — the terminator observed is `0xFF` raw. `0xFF` XOR `0xEA` = `0x15`.
 * In the decoded view byte 19 = `0x15` — so the terminator IS also XOR'd
 * and the physical byte on the wire is `0x15` for ping packets.
 *
 * But for 0x52 and 0x43, the raw captured bytes end in `0xFF` (unencoded).
 * This means: **0x52 and 0x43 are NOT XOR encoded** — they are plain ASCII name
 * bytes placed directly without XOR. Only the ping (0x4A) uses XOR encoding.
 *
 * Source verification:
 * - `5B 52 AE 82 8B 84 9F 99 82 CA A1 CA B9 EA EA EA EA EA EA FF`
 *   B0=0x5B (plain), B1=0x52 (plain), B2-B12 = name bytes (XOR'd with 0xEA),
 *   B13-B17 = 0xEA (null), B18=0xEA (null/no checksum?), B19=0xFF (terminator)
 * - The name bytes ARE XOR'd: `0xAE XOR 0xEA = 0x44 = 'D'`
 * - So: frame header (B0, B1) are NOT XOR'd, payload (B2-B17) IS XOR'd, terminator NOT XOR'd
 *
 * ## Checksum
 *
 * Both 0x52 and 0x43 have `0xEA` at byte 18. After XOR decode that's `0x00`.
 * The checksum field appears to be `0x00` (no checksum) or uses a different formula.
 * In the capture the raw byte 18 is `0xEA` for both — we replicate this.
 *
 * Source: `docs/protocol/capture-20260816-btsnoop.md`
 */
object RegistrationPacketBuilder {

    /**
     * Maximum UTF-8 encoded name length that fits in the 16-byte payload.
     * Names longer than this are truncated.
     */
    const val MAX_NAME_LENGTH = 16

    /**
     * Default user display name shown on the cluster.
     * Used when no user name is configured.
     */
    const val DEFAULT_USER_NAME = "RTR Companion"

    /**
     * Default vehicle display name shown on the cluster.
     * Used when no vehicle name is configured.
     */
    const val DEFAULT_VEHICLE_NAME = "RTR 310"

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds the user ID packet (`0x52`) — sends the user's display name to the cluster.
     *
     * The name will appear on the bike's TFT cluster display. Max 16 characters.
     * Names longer than [MAX_NAME_LENGTH] are silently truncated.
     *
     * @param name User display name (e.g. "RTR Companion"). ASCII recommended.
     * @return A 20-byte packet ready to write to CHAR_WRITE.
     */
    fun buildUserIdPacket(name: String = DEFAULT_USER_NAME): ByteArray =
        buildNamePacket(BleConstants.MSG_USER_ID, name)

    /**
     * Builds the vehicle name packet (`0x43`) — sends the vehicle display name to the cluster.
     *
     * The name will appear on the bike's TFT cluster display. Max 16 characters.
     * Names longer than [MAX_NAME_LENGTH] are silently truncated.
     *
     * @param name Vehicle display name (e.g. "RTR 310"). ASCII recommended.
     * @return A 20-byte packet ready to write to CHAR_WRITE.
     */
    fun buildVehicleNamePacket(name: String = DEFAULT_VEHICLE_NAME): ByteArray =
        buildNamePacket(BleConstants.MSG_VEHICLE_NAME, name)

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a 20-byte name packet:
     * ```
     * B0   : FRAME_CONTROL (0x5B) — plain, not XOR'd
     * B1   : msgId — plain, not XOR'd
     * B2–17: name bytes XOR'd with 0xEA; unused bytes = 0xEA (null)
     * B18  : 0xEA (checksum field = null — matches observed capture)
     * B19  : 0xFF (terminator — plain, not XOR'd)
     * ```
     */
    private fun buildNamePacket(msgId: Byte, name: String): ByteArray {
        val packet = ByteArray(20)

        // Header — plain (not XOR encoded)
        packet[0] = BleConstants.FRAME_CONTROL  // 0x5B
        packet[1] = msgId

        // Payload — name bytes XOR encoded with 0xEA, rest padded with 0xEA (null)
        val nameBytes = name.toByteArray(Charsets.UTF_8).take(MAX_NAME_LENGTH)
        for (i in 0..15) {
            val nameByte = if (i < nameBytes.size) nameBytes[i] else 0x00.toByte()
            packet[2 + i] = (nameByte.toInt() xor BleConstants.XOR_KEY.toInt()).toByte()
        }

        // Checksum field — 0xEA as observed in capture (decodes to 0x00)
        packet[18] = BleConstants.XOR_KEY  // 0xEA

        // Terminator — plain (not XOR encoded)
        packet[19] = BleConstants.PACKET_END  // 0xFF

        return packet
    }
}
