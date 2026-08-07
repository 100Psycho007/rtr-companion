package dev.rtrcompanion.blecore.model

/**
 * Lifecycle states for a single BLE connection attempt.
 *
 * State machine:
 *   Disconnected → Connecting → Connected → DiscoveringServices
 *                                         → Ready
 *   Any state    → Disconnected (on error or user disconnect)
 *                → Error
 */
sealed class ConnectionState {

    /** No active connection. Initial and terminal state. */
    data object Disconnected : ConnectionState()

    /** GATT connect() has been called; waiting for callback. */
    data object Connecting : ConnectionState()

    /** GATT connected; preparing to discover services. */
    data class Connected(val device: RtrDevice) : ConnectionState()

    /** Service discovery in progress. */
    data class DiscoveringServices(val device: RtrDevice) : ConnectionState()

    /**
     * Services discovered and notifications enabled.
     * The SDK is ready to receive packets.
     *
     * @param device     Connected device info.
     * @param serviceIds Short UUIDs of discovered services for display.
     */
    data class Ready(
        val device: RtrDevice,
        val serviceIds: List<String>,
    ) : ConnectionState()

    /**
     * Unrecoverable error. Caller should disconnect and reset.
     *
     * @param message Human-readable description.
     * @param gattStatus Raw GATT status code if available (0 = unknown).
     */
    data class Error(
        val message: String,
        val gattStatus: Int = 0,
    ) : ConnectionState()
}
