package dev.rtrcompanion.blecore

import java.util.UUID

/**
 * Single source of truth for all confirmed BLE identifiers for the TVS Apache RTR 310 SmartXonnect TFT.
 *
 * **Source:** nRF Connect sessions documented in `docs/sessions/` and `docs/BLE-Protocol.md`.
 *
 * **Rule:** Do NOT add entries here without confirmed hardware evidence.
 * All UUIDs must have a corresponding entry in `docs/KNOWN_FACTS.md` under Confirmed.
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
     * This constant is defined for documentation and future use only.
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
     * notifications. This is the only write operation performed during Sprint 1–3.
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
     * Delay in milliseconds after [BluetoothProfile.STATE_CONNECTED] before calling
     * [android.bluetooth.BluetoothGatt.discoverServices].
     *
     * The Android BLE documentation recommends a short delay after connection
     * before service discovery to allow the connection to stabilise.
     */
    const val SERVICE_DISCOVERY_DELAY_MS = 600L
}
