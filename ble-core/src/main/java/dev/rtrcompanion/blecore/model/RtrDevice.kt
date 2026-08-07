package dev.rtrcompanion.blecore.model

/**
 * Represents a discovered RTR 310 BLE device before connection.
 *
 * @param address  Hardware MAC address (e.g. "AA:BB:CC:DD:EE:FF").
 * @param name     Advertised device name (e.g. "TVSRTR310FKB0925").
 * @param rssi     Signal strength in dBm at discovery time.
 */
data class RtrDevice(
    val address: String,
    val name: String,
    val rssi: Int,
)
