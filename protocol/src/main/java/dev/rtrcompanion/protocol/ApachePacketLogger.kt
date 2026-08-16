package dev.rtrcompanion.protocol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Accumulates [ApachePacket]s (parsed form of raw BLE notifications) for display
 * and export.
 *
 * This is the protocol-layer counterpart of [PacketLogger]. While [PacketLogger]
 * stores raw bytes, [ApachePacketLogger] stores frames that have been run through
 * [ApacheProtocol.parseFrame] and can report per-type statistics, checksum pass
 * rates, and identified frame types.
 *
 * ## Usage
 *
 * Feed this logger from the same coroutine that feeds [PacketLogger]:
 * ```kotlin
 * gattManager.packetFlow.collect { bytes ->
 *     val raw = RawPacket(bytes)
 *     packetLogger.record(raw)                           // raw bytes logger
 *     apachePacketLogger.record(ApacheProtocol.parseFrame(raw))  // parsed logger
 * }
 * ```
 *
 * ## Thread safety
 *
 * All state mutations use [MutableStateFlow] value assignment. Safe to call from
 * a single coroutine (viewModelScope dispatcher).
 *
 * @param maxEntries Maximum parsed packets held in memory (oldest dropped first).
 */
class ApachePacketLogger(private val maxEntries: Int = 500) {

    private val _log = MutableStateFlow<List<ApachePacket>>(emptyList())

    /** Live, observable list of parsed packets. Newest entry is last. */
    val log: StateFlow<List<ApachePacket>> = _log.asStateFlow()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Records a parsed [ApachePacket]. Oldest entry is dropped when [maxEntries]
     * is exceeded.
     *
     * Logs a structured line per packet at DEBUG level using the format:
     * `[RX] 0x10 SpeedOMeter1  20B  chk=PASS`
     */
    fun record(packet: ApachePacket) {
        val frame = packet.frame
        if (frame != null) {
            val chk = ApacheProtocol.verifyChecksum(frame.raw, frame.id).name
            Timber.d(
                "[%s] %s  %dB  chk=%s",
                frame.direction.name,
                ApacheProtocol.messageLabel(frame.id),
                frame.length,
                chk,
            )
        } else {
            Timber.w("[PROTOCOL] Unparsed packet: %s", packet.hex)
        }

        val current = _log.value
        val updated = if (current.size >= maxEntries) {
            current.drop(1) + packet
        } else {
            current + packet
        }
        _log.value = updated
    }

    /** Clears all stored packets. */
    fun clear() {
        _log.value = emptyList()
    }

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    /**
     * Returns a map of message ID → count for all parsed packets in the current log.
     *
     * Unparsed packets (where `frame == null`) are counted under the key `null`.
     *
     * Example:
     * ```
     * {0x10 → 5, 0x11 → 5, 0x5F → 10, null → 1}
     * ```
     */
    fun messageTypeCounts(): Map<Byte?, Int> =
        _log.value.groupingBy { it.frame?.id }.eachCount()

    /**
     * Returns the number of packets in the current log that have a known,
     * confirmed message ID (i.e. present in the protocol documentation).
     */
    fun knownTypeCount(): Int =
        _log.value.count { it.frame != null }

    /**
     * Returns the number of packets with a confirmed-passing checksum.
     *
     * Only packets with a known C constant are counted as passing; packets with
     * `ChecksumResult.UNKNOWN` are neither passing nor failing.
     */
    fun checksumPassCount(): Int = _log.value.count { pkt ->
        val frame = pkt.frame ?: return@count false
        ApacheProtocol.verifyChecksum(frame.raw, frame.id) == ApacheProtocol.ChecksumResult.PASS
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    /**
     * Exports the current log as a newline-separated text format suitable for
     * saving to a file or sharing.
     *
     * Each line: `<timestamp> <direction> <msgLabel> <hex>`
     *
     * Example:
     * ```
     * 1723120931001 RX 0x10 SpeedOMeter1 5A 10 EA ...
     * 1723120931050 RX 0x11 SpeedOMeter2 5A 11 EA ...
     * ```
     */
    fun export(): String = buildString {
        _log.value.forEach { pkt ->
            val frame = pkt.frame
            if (frame != null) {
                appendLine(
                    "${pkt.timestamp} ${frame.direction.name} " +
                        "${ApacheProtocol.messageLabel(frame.id)} ${frame.hex}"
                )
            } else {
                appendLine("${pkt.timestamp} RX UNPARSED ${pkt.hex}")
            }
        }
    }.trimEnd()
}
