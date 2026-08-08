package dev.rtrcompanion.blecore

import java.util.UUID

/**
 * Single source of truth for all confirmed BLE identifiers for the TVS Apache RTR 310 SmartXonnect TFT.
 *
 * **Source:** nRF Connect sessions documented in `docs/sessions/` and `docs/BLE-Protocol.md`.
 * **Cross-reference:** TVS Jupiter uses the identical service UUID and characteristics.
 * See `docs/BLE-Protocol.md` for full protocol documentation.
 *
 * **Rule:** Do NOT add entries here without confirmed hardware evidence.
 */
object BleConstants {

    // -------------------------------------------------------------------------
    // Device advertising
    // -------------------------------------------------------------------------

    /** Prefix that RTR 310 devices advertise. Used to filter scan results. */
    const val DEVICE_NAME_PREFIX = "TVSRTR310"

    // -------------------------------------------------------------------------
    // Standard BLE services (confirmed via nRF Connect)
    // -------------------------------------------------------------------------

    /** Standard Generic Access service. Present on all BLE devices. */
    val SERVICE_GENERIC_ACCESS: UUID        = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")

    /** Standard Generic Attribute service. Present on all BLE devices. */
    val SERVICE_GENERIC_ATTRIBUTE: UUID     = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")

    /** Standard Device Information service. May contain manufacturer name and firmware version. */
    val SERVICE_DEVICE_INFORMATION: UUID    = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Proprietary TVS service
    // -------------------------------------------------------------------------

    /**
     * Primary TVS proprietary service UUID.
     *
     * The UUID is experimentally confirmed.
     * Any textual interpretation is speculative and intentionally omitted until verified.
     * Cross-reference: identical on TVS Jupiter (github.com/overclock98/JupiterRideCompanion).
     */
    val SERVICE_TVS_PROPRIETARY: UUID = UUID.fromString("5456534d-5647-5341-5342-454e544f5251")

    // -------------------------------------------------------------------------
    // Characteristics (within SERVICE_TVS_PROPRIETARY)
    // -------------------------------------------------------------------------

    /**
     * WRITE characteristic — Phone → Bike commands.
     *
     * **Safety rule:** Do NOT write to this characteristic without a confirmed,
     * documented packet format in `docs/BLE-Protocol.md` and an approved ADR.
     *
     * Currently used for:
     *  - Authentication handshake response (`0x9A 0xF1`) — see [HandshakeManager]
     *  - Keep-alive ping (`0x5B 0x4A`) — see [PingPacketBuilder]
     */
    val CHAR_WRITE: UUID = UUID.fromString("00005352-0000-1000-8000-00805f9b34fb")

    /**
     * NOTIFY characteristic — Bike → Phone data.
     *
     * Enable notifications on this characteristic via the [DESCRIPTOR_CCCD] to receive
     * all packets from the bike. This is the sole data source for the entire app.
     */
    val CHAR_NOTIFY: UUID = UUID.fromString("00005354-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Standard BLE descriptor
    // -------------------------------------------------------------------------

    /**
     * Client Characteristic Configuration Descriptor (CCCD).
     *
     * Writing [android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE]
     * to this descriptor on [CHAR_NOTIFY] instructs the peripheral to start sending
     * notifications.
     */
    val DESCRIPTOR_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Timing constants
    // -------------------------------------------------------------------------

    /** BLE scan timeout in milliseconds. Scan stops automatically after this duration. */
    const val SCAN_TIMEOUT_MS = 15_000L

    /** GATT operation timeout in milliseconds. Reserved for future timeout enforcement. */
    const val GATT_TIMEOUT_MS = 10_000L

    /**
     * Delay in milliseconds after [android.bluetooth.BluetoothProfile.STATE_CONNECTED]
     * before calling [android.bluetooth.BluetoothGatt.discoverServices].
     *
     * The Android BLE documentation recommends a short delay after connection
     * before service discovery to allow the connection to stabilise.
     */
    const val SERVICE_DISCOVERY_DELAY_MS = 600L

    /**
     * Interval in milliseconds between keep-alive ping packets sent to the bike.
     *
     * The TVS SmartXonnect protocol requires continuous pings to maintain the
     * connection and update the cluster display. Based on Jupiter RE documentation.
     */
    const val PING_INTERVAL_MS = 1_000L

    // -------------------------------------------------------------------------
    // Protocol constants
    // -------------------------------------------------------------------------

    /** Start byte for data frames (Bike → Phone inbound, most common). */
    const val FRAME_DATA: Byte = 0x5A.toByte()

    /** Start byte for control/null frames. */
    const val FRAME_CONTROL: Byte = 0x5B.toByte()

    /** Packet terminator — last byte of every packet. */
    const val PACKET_END: Byte = 0xFF.toByte()

    /**
     * Null/empty field value used in **inbound** (Bike → Phone) packets.
     * Fields carrying no data are padded with this value.
     * Source: observed in RTR 310 shutdown capture (2026-08-08).
     */
    const val INBOUND_NULL: Byte = 0xEA.toByte()

    // -------------------------------------------------------------------------
    // Message IDs (inbound, Bike → Phone)
    // -------------------------------------------------------------------------

    /**
     * Odometer and fuel level.
     * - Bytes 3–5: Odometer UInt24 big-endian, divide by 10.0 for km
     * - Byte 6: Fuel (lower nibble = bars 0–5, upper nibble = reserve flag)
     * - Byte 13: Call command button press (1=Answer, 2=Reject)
     * Source: Jupiter RE cross-reference, confirmed present on RTR 310.
     */
    const val MSG_ODOMETER_FUEL: Byte = 0x10.toByte()

    /**
     * Service reminder indicator.
     * - Byte 4: service reminder flag
     * Source: Jupiter RE cross-reference, confirmed present on RTR 310.
     */
    const val MSG_SERVICE: Byte = 0x11.toByte()

    /**
     * Unknown — observed in RTR 310 capture, not in Jupiter docs.
     * Static in capture. Pending analysis.
     */
    const val MSG_UNKNOWN_12: Byte = 0x12.toByte()

    /**
     * Fuel economy and distance-to-empty (from Jupiter RE — not yet observed on RTR 310 with ignition ON).
     * - Byte 8: Average fuel economy in km/L
     * - Bytes 11–12: Distance to empty (short)
     */
    const val MSG_ECONOMY: Byte = 0x19.toByte()

    /**
     * Live telemetry — observed as dynamic in RTR 310 shutdown capture.
     * Multiple bytes change between occurrences. Byte 7 acts as frame counter.
     * Fields not yet decoded. Requires capture during active ride.
     */
    const val MSG_LIVE_TELEMETRY: Byte = 0x5F.toByte()

    /**
     * Unknown fully-packed message — observed in RTR 310 capture.
     * No null (0xEA) bytes — carries dense data. Static across capture.
     * Not present in Jupiter docs — may be RTR 310 specific.
     */
    const val MSG_UNKNOWN_7D: Byte = 0x7D.toByte()

    // -------------------------------------------------------------------------
    // Message IDs (outbound, Phone → Bike)
    // -------------------------------------------------------------------------

    /**
     * Keep-alive ping / Mobile Data packet.
     * Must be sent every [PING_INTERVAL_MS] to maintain the connection and update
     * the cluster display with phone status (time, battery, signal, etc.).
     *
     * Also used to trigger the Find Me feature (byte 17 = 0x01).
     * Source: Jupiter RE documentation.
     */
    const val MSG_PING: Byte = 0x4A.toByte()

    // -------------------------------------------------------------------------
    // Authentication handshake message IDs
    // -------------------------------------------------------------------------

    /**
     * Authentication challenge packet ID — sent by bike on NOTIFY after connection.
     * Full message: [FRAME_AUTH] [MSG_AUTH_CHALLENGE] + 16 random bytes + checksum + [PACKET_END]
     * Source: Jupiter RE documentation. **NOT YET OBSERVED on RTR 310.**
     */
    const val MSG_AUTH_CHALLENGE: Byte = 0xF2.toByte()

    /**
     * Authentication response packet ID — sent by phone on WRITE.
     * Full message: [0x9A] [MSG_AUTH_RESPONSE] + AES-128-CTR encrypted challenge bytes + checksum + [PACKET_END]
     * Source: Jupiter RE documentation. **UNVERIFIED on RTR 310.**
     * Writes using this ID are DISABLED in PASSIVE protocol mode.
     */
    const val MSG_AUTH_RESPONSE: Byte = 0xF1.toByte()

    /**
     * Authentication frame start byte — used for both challenge and response.
     * Different from the standard [FRAME_DATA] / [FRAME_CONTROL] bytes.
     * Source: Jupiter RE documentation. **UNVERIFIED on RTR 310.**
     */
    const val FRAME_AUTH: Byte = 0x9A.toByte()
}
