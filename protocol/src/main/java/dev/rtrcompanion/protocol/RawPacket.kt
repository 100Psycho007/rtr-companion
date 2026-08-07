package dev.rtrcompanion.protocol

/**
 * A raw, uninterpreted BLE notification packet received from the RTR 310.
 *
 * The packet format is not yet reverse-engineered. This class is a
 * placeholder that carries bytes through the pipeline until Sprint 3
 * adds actual parsing.
 *
 * @param bytes     Raw payload exactly as received from CHAR_NOTIFY.
 * @param timestamp Wall-clock epoch ms when the packet was received.
 */
data class RawPacket(
    val bytes: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
) {
    /** Hex representation for logging. e.g. "AA BB CC DD" */
    val hex: String get() = bytes.joinToString(" ") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawPacket) return false
        return timestamp == other.timestamp && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + timestamp.hashCode()

    override fun toString(): String = "RawPacket(len=${bytes.size}, hex=$hex)"
}
