package dev.rtrcompanion.protocol

/**
 * Represents a single Apache BLE packet in the analysis pipeline.
 *
 * [ApachePacket] wraps a [RawPacket] and adds structured fields extracted by
 * [ApacheProtocol.parseFrame]. It is the bridge between raw bytes and the
 * typed [ApacheFrame] model.
 *
 * ## Relationship to other types
 * ```
 * RawPacket           ← raw bytes + timestamp (from BLE notification)
 *      │
 *      ▼  ApacheProtocol.parseFrame()
 * ApachePacket        ← raw + structured frame (this class)
 *      │
 *      ▼  Sprint 5: field decoders per message ID
 * ParsedPacket        ← typed decoded data (speed, RPM, etc.)
 * ```
 *
 * An [ApachePacket] with `frame == null` means the packet was not parseable
 * (wrong length, unexpected terminator, etc.). The [raw] bytes are always
 * available for further investigation.
 *
 * @param raw   The original [RawPacket] from CHAR_NOTIFY.
 * @param frame A parsed [ApacheFrame] if the packet structure was valid, or null.
 */
data class ApachePacket(
    val raw: RawPacket,
    val frame: ApacheFrame?,
) {
    /** Convenience — wall clock timestamp delegated from [raw]. */
    val timestamp: Long get() = raw.timestamp

    /** True when the packet was successfully parsed into a [ApacheFrame]. */
    val isParsed: Boolean get() = frame != null

    /** True when the packet has the expected 20-byte length. */
    val isValidLength: Boolean get() = raw.bytes.size == EXPECTED_LENGTH

    /** Hex dump of the raw bytes. */
    val hex: String get() = raw.hex

    override fun toString(): String = if (frame != null) {
        "ApachePacket[$frame]"
    } else {
        "ApachePacket[UNPARSED len=${raw.bytes.size} hex=${raw.hex}]"
    }

    companion object {
        /** Expected packet length for all Apache BLE frames. */
        const val EXPECTED_LENGTH = 20
    }
}
