package dev.rtrcompanion.blecore

import java.util.UUID

/**
 * All confirmed BLE identifiers for the TVS Apache RTR 310 SmartXonnect TFT.
 *
 * Source: nRF Connect sessions documented in docs/BLE-Protocol.md
 * Do NOT add entries here without confirmed hardware evidence.
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

    val SERVICE_GENERIC_ACCESS: UUID        = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val SERVICE_GENERIC_ATTRIBUTE: UUID     = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
    val SERVICE_DEVICE_INFORMATION: UUID    = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Proprietary TVS service
    // -------------------------------------------------------------------------

    /**
     * Primary TVS proprietary service UUID.
     * ASCII decode: "TVSMVGSABENTORQ" — appears to be a TVS branding string.
     */
    val SERVICE_TVS_PROPRIETARY: UUID = UUID.fromString("5456534d-5647-5341-5342-454e544f5251")

    // -------------------------------------------------------------------------
    // Characteristics (within SERVICE_TVS_PROPRIETARY)
    // -------------------------------------------------------------------------

    /**
     * WRITE characteristic — Phone → Bike commands.
     * Do NOT write to this without a confirmed, documented packet format.
     */
    val CHAR_WRITE: UUID = UUID.fromString("00005352-0000-1000-8000-00805f9b34fb")

    /**
     * NOTIFY characteristic — Bike → Phone data.
     * Enable notifications on this to receive all bike data.
     */
    val CHAR_NOTIFY: UUID = UUID.fromString("00005354-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Standard BLE descriptor
    // -------------------------------------------------------------------------

    /** Client Characteristic Configuration Descriptor — used to enable notifications. */
    val DESCRIPTOR_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // -------------------------------------------------------------------------
    // Timing constants
    // -------------------------------------------------------------------------

    /** BLE scan timeout in milliseconds. */
    const val SCAN_TIMEOUT_MS = 15_000L

    /** GATT operation timeout in milliseconds. */
    const val GATT_TIMEOUT_MS = 10_000L

    /** Delay after connection before discovering services (recommended by Android BLE guide). */
    const val SERVICE_DISCOVERY_DELAY_MS = 600L
}
