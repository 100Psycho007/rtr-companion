package dev.rtrcompanion.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Accumulates [RawPacket]s for display and future analysis.
 *
 * This is a Sprint 2 component — it stores all packets received during a
 * session so the UI can show a live log and the packets can later be
 * exported as captures.
 *
 * Sprint 3 will add a parser that replaces this with typed messages.
 *
 * @param maxEntries Maximum packets held in memory (oldest dropped first).
 */
class PacketLogger(private val maxEntries: Int = 500) {

    private val _log = MutableStateFlow<List<RawPacket>>(emptyList())

    /** Live, observable list of received packets. Newest entry is last. */
    val log: StateFlow<List<RawPacket>> = _log.asStateFlow()

    /**
     * Record a new packet. Called from a coroutine that collects
     * [dev.rtrcompanion.blecore.connection.RtrGattManager.packetFlow].
     *
     * Thread-safe via [kotlinx.coroutines.flow.MutableStateFlow] value assignment.
     *
     * @param packet The raw notification packet received from [BleConstants.CHAR_NOTIFY].
     */
    fun record(packet: RawPacket) {
        Timber.d("LOG [%d bytes] %s", packet.bytes.size, packet.hex)
        val current = _log.value
        val updated = if (current.size >= maxEntries) {
            current.drop(1) + packet
        } else {
            current + packet
        }
        _log.value = updated
    }

    /** Clear all stored packets. */
    fun clear() {
        _log.value = emptyList()
    }

    /** Export current log as newline-separated hex strings. */
    fun export(): String = _log.value.joinToString("\n") { pkt ->
        "${pkt.timestamp} ${pkt.hex}"
    }
}
