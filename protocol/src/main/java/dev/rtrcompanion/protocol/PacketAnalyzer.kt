package dev.rtrcompanion.protocol

/**
 * Skeleton for future packet analysis.
 *
 * The RTR 310 packet format is not yet reverse-engineered.
 * This class is a placeholder that will be populated in Sprint 5
 * once sufficient capture data is available to identify message types.
 *
 * ## What goes here (Sprint 5+)
 *
 * When a packet structure is confirmed:
 * 1. Add a `ParsedPacket` sealed class with subtypes for each known message
 * 2. Implement `analyze(RawPacket): ParsedPacket?` — returns null for unknown packets
 * 3. Document every field in [docs/BLE-Protocol.md]
 * 4. Update [docs/KNOWN_FACTS.md] — move hypotheses to confirmed
 *
 * ## Safety rule
 *
 * Never invent packet formats. All fields documented here must be verified
 * from real capture data. Unknown packets must remain as [RawPacket].
 */
object PacketAnalyzer {

    /**
     * Attempt to interpret a raw packet.
     *
     * Currently returns null for every input — no packet format is confirmed.
     * Callers must treat null as "unknown packet, log raw bytes only".
     *
     * @param packet A raw notification received from CHAR_NOTIFY (0x5354).
     * @return A parsed packet if the format is recognised, or null.
     */
    fun analyze(packet: RawPacket): ParsedPacket? {
        // TODO Sprint 5 — implement once packet format is reverse-engineered
        return null
    }

    /**
     * Returns a human-readable summary of a packet's contents.
     * Falls back to raw hex when the format is unknown.
     */
    fun describe(packet: RawPacket): String {
        val parsed = analyze(packet)
        return parsed?.describe() ?: "UNKNOWN [${packet.bytes.size}B] ${packet.hex}"
    }
}

/**
 * A successfully parsed BLE notification packet from the RTR 310.
 *
 * This sealed class is empty until Sprint 5.
 * Subtypes will be added for each confirmed message type, e.g.:
 *
 * ```kotlin
 * data class VehicleState(val speedKmh: Int, val rpmx100: Int) : ParsedPacket()
 * ```
 *
 * Do NOT add subtypes until the packet format is experimentally confirmed.
 */
sealed class ParsedPacket {
    /** Short human-readable description of this packet's payload. */
    abstract fun describe(): String
}
