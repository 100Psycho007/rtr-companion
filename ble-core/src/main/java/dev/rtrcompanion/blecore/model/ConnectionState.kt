package dev.rtrcompanion.blecore.model

/**
 * Lifecycle states for a single BLE GATT connection to an RTR 310 device.
 *
 * State machine:
 * ```
 * Disconnected → Connecting → [Bonding →] Connected → DiscoveringServices → Ready
 * Any state    → Disconnected  (user disconnect or peripheral disconnect)
 * Any state    → Error         (GATT error; caller should disconnect and reset)
 * ```
 *
 * The [Bonding] state is only entered on first connection to an unbonded device.
 * Android pops a system pairing dialog (passkey display/entry) and the GATT stack
 * pauses until bonding completes. On subsequent connections the device is already
 * bonded and this state is skipped entirely.
 *
 * Source: rtr_auth_logcat.txt (2026-08-15) — `BluetoothPairingDialog` launched with
 * `android.bluetooth.device.action.PAIRING_REQUEST` via nRF Connect connecting to
 * the RTR 310 for the first time. Device was already in `getBondedDevices` for all
 * subsequent connections throughout the log.
 *
 * Emitted by [dev.rtrcompanion.blecore.connection.RtrGattManager.connectionState].
 */
sealed class ConnectionState {

    /** No active connection. Initial state and the state after a clean disconnect. */
    data object Disconnected : ConnectionState()

    /**
     * [android.bluetooth.BluetoothDevice.connectGatt] has been called.
     * Waiting for [android.bluetooth.BluetoothGattCallback.onConnectionStateChange].
     */
    data object Connecting : ConnectionState()

    /**
     * The device is not yet bonded. Android has initiated BLE SMP pairing.
     * A system passkey dialog is being shown to the user.
     *
     * This state is only reached on **first-ever connection** to this bike.
     * Once bonded, Android skips pairing silently on all subsequent connections.
     *
     * The app should display a message like:
     * "Accept the pairing request shown by Android to connect to your bike."
     *
     * GATT service discovery will proceed automatically once the OS confirms bonding.
     *
     * @param device The device currently being bonded.
     */
    data class Bonding(val device: RtrDevice) : ConnectionState()

    /**
     * GATT STATE_CONNECTED received. Preparing to discover services.
     *
     * @param device The device that was connected.
     */
    data class Connected(val device: RtrDevice) : ConnectionState()

    /**
     * [android.bluetooth.BluetoothGatt.discoverServices] has been called.
     * Waiting for [android.bluetooth.BluetoothGattCallback.onServicesDiscovered].
     *
     * @param device The connected device.
     */
    data class DiscoveringServices(val device: RtrDevice) : ConnectionState()

    /**
     * Services discovered and CCCD notifications enabled on [BleConstants.CHAR_NOTIFY].
     *
     * The SDK is ready to receive packets. [dev.rtrcompanion.blecore.connection.RtrGattManager.packetFlow]
     * will now emit notification bytes as they arrive.
     *
     * @param device     The connected device.
     * @param serviceIds Short 4-hex-character UUIDs of all discovered services (e.g. "1800", "TVSM").
     */
    data class Ready(
        val device: RtrDevice,
        val serviceIds: List<String>,
    ) : ConnectionState()

    /**
     * An unrecoverable GATT error occurred.
     *
     * The caller should call [dev.rtrcompanion.blecore.connection.RtrGattManager.disconnect]
     * to release resources and then allow the user to retry.
     *
     * @param message    Human-readable error description.
     * @param gattStatus Raw GATT status code from the callback (0 = unknown/not applicable).
     */
    data class Error(
        val message: String,
        val gattStatus: Int = 0,
    ) : ConnectionState()
}
