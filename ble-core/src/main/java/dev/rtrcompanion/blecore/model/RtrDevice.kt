package dev.rtrcompanion.blecore.model

/**
 * Represents a discovered RTR 310 BLE peripheral before a GATT connection is established.
 *
 * Produced by [dev.rtrcompanion.blecore.scanner.RtrScanner] during a scan session and
 * used by [dev.rtrcompanion.blecore.connection.RtrGattManager] to initiate a connection.
 *
 * @param address  Hardware MAC address (e.g. "AA:BB:CC:DD:EE:FF"). Stable per device.
 * @param name     Advertised device name (e.g. "TVSRTR310FKB0925"). Matches [BleConstants.DEVICE_NAME_PREFIX].
 * @param rssi     Signal strength in dBm at discovery time. Lower (more negative) = weaker signal.
 */
data class RtrDevice(
    val address: String,
    val name: String,
    val rssi: Int,
)
