package dev.rtrcompanion.protocol

/**
 * A parsed Apache BLE frame extracted from a raw 20-byte notification packet.
 *
 * ## Frame structure (every inbound packet is exactly 20 bytes):
 * ```
 * B0   : Frame type — 0x5A (data) or 0x5B (control)
 * B1   : Message ID — identifies the data type
 * B2–17: Payload — 16 bytes; 0xEA = empty/null field
 * B18  : Checksum — (C − sum(B0..B17)) mod 256, C is per-message-type constant
 * B19  : Terminator — always 0xFF
 * ```
 *
 * ## What goes here NOW vs later
 *
 * **Now (confirmed):** frame type, message ID, raw bytes, length, timestamp, direction.
 * **Later (Sprint 5+):** individual decoded fields once real ignition-ON captures confirm
 * field positions. See `docs/BLE-Protocol.md`.
 *
 * ## Safety rule
 * Do NOT add field decodings here without confirmed evidence from hardware captures.
 * Unknown frames stay as [ApacheFrame] with raw bytes only.
 *
 * @param frameType  Byte 0 — frame category (`0x5A`=data, `0x5B`=control, `0x9A`=auth).
 * @param id         Byte 1 — message ID that identifies the data type.
 * @param raw        The full 20-byte packet as received from CHAR_NOTIFY.
 * @param timestamp  Wall-clock epoch milliseconds when this frame was received.
 * @param direction  Whether this frame was received or transmitted.
 */
data class ApacheFrame(
    val frameType: Byte,
    val id: Byte,
    val raw: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: Direction = Direction.RX,
) {
    /** Number of bytes in the raw packet. Should always be 20 for valid frames. */
    val length: Int get() = raw.size

    /** The 16-byte payload region (bytes 2–17). */
    val payload: ByteArray get() = raw.copyOfRange(2, minOf(18, raw.size))

    /** Checksum byte (byte 18), or 0 if packet is too short. */
    val checksum: Byte get() = if (raw.size > 18) raw[18] else 0

    /** Terminator byte (byte 19), expected to be 0xFF. */
    val terminator: Byte get() = if (raw.size > 19) raw[19] else 0

    /** Full hex dump of the raw packet, e.g. `"5A 10 EA EA ..."`. */
    val hex: String get() = raw.joinToString(" ") { "%02X".format(it) }

    /** Short readable frame type label for logging. */
    val frameTypeLabel: String get() = when (frameType) {
        0x5A.toByte() -> "DATA"
        0x5B.toByte() -> "CTRL"
        0x9A.toByte() -> "AUTH"
        else          -> "0x%02X".format(frameType)
    }

    override fun toString(): String =
        "ApacheFrame(type=$frameTypeLabel id=0x%02X len=$length dir=$direction ts=$timestamp)".format(id)

    // ByteArray requires explicit equals/hashCode for correct data class behaviour
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApacheFrame) return false
        return frameType == other.frameType
            && id == other.id
            && timestamp == other.timestamp
            && direction == other.direction
            && raw.contentEquals(other.raw)
    }

    override fun hashCode(): Int {
        var result = frameType.toInt()
        result = 31 * result + id.toInt()
        result = 31 * result + raw.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }

    /**
     * Packet direction — used for both RX (bike → phone) and future TX (phone → bike).
     */
    enum class Direction {
        /** Received from the bike via CHAR_NOTIFY (`0x5354`). */
        RX,
        /** Transmitted to the bike via CHAR_WRITE (`0x5352`). Reserved for Sprint 5+. */
        TX,
    }
}
