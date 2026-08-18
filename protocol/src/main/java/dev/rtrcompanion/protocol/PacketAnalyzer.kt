package dev.rtrcompanion.protocol

/**
 * Entry point for protocol analysis of raw BLE notification packets.
 *
 * In Sprint 3–4 this is still largely a stub. The structural parsing
 * ([ApacheProtocol.parseFrame], [ApacheFrame]) is implemented and working;
 * field-level decoding of individual message types is reserved for Sprint 5.
 *
 * ## What is implemented
 *  - Packet structure validation (length, terminator byte)
 *  - Frame type + message ID extraction
 *  - Per-message-type checksum verification
 *  - [describe] — human-readable summary of any packet
 *
 * ## What goes here in Sprint 5
 *
 * When a packet structure is confirmed for a message type:
 * 1. Add a [ParsedPacket] subtype for that message (e.g. `SpeedometerData`)
 * 2. Implement `analyze(RawPacket): ParsedPacket?` returning the decoded value
 * 3. Document every field in `docs/BLE-Protocol.md` with confirmed source
 * 4. Add a unit test in `protocol/src/test/`
 *
 * ## Safety rule
 * Never invent packet formats. All fields documented here must be verified
 * from real capture data. Unknown packets must remain as [RawPacket].
 */
object PacketAnalyzer {

    /**
     * Attempt to interpret a raw packet into a [ParsedPacket].
     *
     * Currently returns null for every input — no field-level decoding is
     * implemented until Sprint 5. Use [describe] for a structural summary.
     *
     * @param packet A raw notification received from CHAR_NOTIFY (0x5354).
     * @return A typed parsed packet, or null if the format is not yet decoded.
     */
    fun analyze(packet: RawPacket): ParsedPacket? {
        // TODO Sprint 5 — implement field decoders per confirmed message type:
        //  0x10 → ApacheSpeedOMeter1 (odometer, fuel, RPM) — awaiting ignition-ON capture
        //  0x11 → ApacheSpeedOMeter2 (gear, battery voltage, service reminder)
        //  0x12 → ApacheSpeedOMeter3 (lean angle, cruising range)
        //  0x5F → Unknown live telemetry — checksum UNRESOLVED, needs live capture
        //  0x7D → Device identity (VIN) — XOR 0xEA decode already documented
        //  0x42 → Keep-alive heartbeat
        return null
    }

    /**
     * Returns a human-readable summary of a packet's contents.
     *
     * Unlike [analyze], this always returns a non-null string. For packets with
     * known structure it provides structural information (frame type, message ID,
     * checksum status). For entirely unknown packets it falls back to raw hex.
     *
     * This is the function used by [PacketLogScreen] annotations.
     */
    fun describe(packet: RawPacket): String {
        val parsed = analyze(packet)
        if (parsed != null) return parsed.describe()

        // Use the protocol parser for structural description
        val apachePacket = ApacheProtocol.parseFrame(packet)
        val frame = apachePacket.frame ?: return "MALFORMED [${packet.bytes.size}B] ${packet.hex}"

        val chk = ApacheProtocol.verifyChecksum(frame.raw, frame.id)
        val chkLabel = when (chk) {
            ApacheProtocol.ChecksumResult.PASS    -> "chk=✓"
            ApacheProtocol.ChecksumResult.FAIL    -> "chk=✗"
            ApacheProtocol.ChecksumResult.UNKNOWN -> "chk=?"
        }

        return "[${frame.frameTypeLabel}] ${ApacheProtocol.messageLabel(frame.id)}  $chkLabel  ${frame.length}B"
    }
}

/**
 * A successfully decoded BLE notification packet from the RTR 310.
 *
 * This sealed class is empty until Sprint 5.
 *
 * Subtypes will be added for each confirmed message type once field positions
 * are verified from live ignition-ON captures:
 *
 * ```kotlin
 * // Example — DO NOT add until confirmed:
 * data class SpeedometerData(
 *     val speedKmh: Int,
 *     val odoKm: Double,
 *     val fuelBars: Int,
 *     val rpmx100: Int,
 * ) : ParsedPacket() {
 *     override fun describe() = "Speed=${speedKmh}km/h  Odo=${odoKm}km  Fuel=$fuelBars  RPM=${rpmx100 * 100}"
 * }
 * ```
 *
 * Do NOT add subtypes until the packet format is hardware-confirmed.
 */
sealed class ParsedPacket {
    /** Short human-readable description of this packet's decoded payload. */
    abstract fun describe(): String
}
