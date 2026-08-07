package dev.rtrcompanion.blecore.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import dev.rtrcompanion.blecore.BleConstants
import dev.rtrcompanion.blecore.model.ConnectionState
import dev.rtrcompanion.blecore.model.RtrDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages a single BLE GATT connection to an RTR 310 device.
 *
 * Responsibilities (Sprint 1):
 *  - Connect to a given [BluetoothDevice]
 *  - Discover services and log them
 *  - Enable notifications on [BleConstants.CHAR_NOTIFY]
 *  - Emit raw notification bytes via [packetFlow]
 *
 * Sprint 2+ will add packet parsing via the `protocol` module.
 *
 * Caller must hold BLUETOOTH_CONNECT permission before calling [connect].
 */
@SuppressLint("MissingPermission") // Caller is responsible for runtime permission
class RtrGattManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /** Observable connection lifecycle. Collect from UI layer. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Raw notification packets from the bike (CHAR_NOTIFY).
     * Each emission is a single raw [ByteArray] as received from BLE.
     * Replay = 0 so only new packets reach collectors.
     */
    private val _packetFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val packetFlow: SharedFlow<ByteArray> = _packetFlow.asSharedFlow()

    private var gatt: BluetoothGatt? = null

    // -------------------------------------------------------------------------
    // GATT Callback
    // -------------------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.e("Connection state change failed: status=%d newState=%d", status, newState)
                handleError("GATT error on connection change", status)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val device = rtrDeviceFrom(gatt.device)
                    Timber.i("Connected to %s", device.name)
                    _connectionState.value = ConnectionState.Connected(device)

                    // Slight delay recommended by Android BLE documentation before
                    // requesting service discovery to ensure stable connection.
                    scope.launch {
                        delay(BleConstants.SERVICE_DISCOVERY_DELAY_MS)
                        Timber.i("Starting service discovery...")
                        _connectionState.value = ConnectionState.DiscoveringServices(device)
                        gatt.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.i("Disconnected")
                    closeGatt()
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleError("Service discovery failed", status)
                return
            }

            val serviceIds = gatt.services.map { service ->
                val shortUuid = service.uuid.toString().substring(4, 8).uppercase()
                Timber.i("Service: %s", shortUuid)
                shortUuid
            }

            // Log characteristics within the TVS proprietary service
            gatt.getService(BleConstants.SERVICE_TVS_PROPRIETARY)?.characteristics?.forEach { char ->
                val shortChar = char.uuid.toString().substring(4, 8).uppercase()
                val props = buildPropertyString(char.properties)
                Timber.i("  Characteristic: %s [%s]", shortChar, props)
            }

            val device = rtrDeviceFrom(gatt.device)
            _connectionState.value = ConnectionState.Ready(device, serviceIds)
            Timber.i("Ready. Enabling notifications on CHAR_NOTIFY...")

            enableNotifications(gatt)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            characteristic.value?.let { bytes ->
                handlePacket(bytes)
            }
        }

        // API 33+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handlePacket(value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.i("Notifications enabled on %s",
                    descriptor.characteristic.uuid.toString().substring(4, 8).uppercase())
            } else {
                Timber.e("Failed to write CCCD descriptor: status=%d", status)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Connect to a discovered RTR 310 device.
     *
     * @param device The [BluetoothDevice] obtained from scan results.
     */
    fun connect(device: BluetoothDevice) {
        if (_connectionState.value !is ConnectionState.Disconnected) {
            Timber.w("Already connecting or connected; ignoring connect()")
            return
        }

        val rtrDevice = RtrDevice(
            address = device.address,
            name = device.name ?: "Unknown",
            rssi = 0,
        )
        Timber.i("Connecting to %s (%s)...", rtrDevice.name, rtrDevice.address)
        _connectionState.value = ConnectionState.Connecting

        // autoConnect = false for direct, fast connection
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /**
     * Disconnect and release all GATT resources.
     * Safe to call in any state.
     */
    fun disconnect() {
        gatt?.disconnect()
        // closeGatt() is called in onConnectionStateChange when STATE_DISCONNECTED fires
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(BleConstants.SERVICE_TVS_PROPRIETARY)
        if (service == null) {
            Timber.e("TVS proprietary service not found after discovery")
            return
        }

        val notifyChar = service.getCharacteristic(BleConstants.CHAR_NOTIFY)
        if (notifyChar == null) {
            Timber.e("CHAR_NOTIFY not found in TVS service")
            return
        }

        val registered = gatt.setCharacteristicNotification(notifyChar, true)
        if (!registered) {
            Timber.e("Failed to register local notification listener")
            return
        }

        val descriptor = notifyChar.getDescriptor(BleConstants.DESCRIPTOR_CCCD)
        if (descriptor == null) {
            Timber.e("CCCD descriptor not found on CHAR_NOTIFY")
            return
        }

        // Write ENABLE_NOTIFICATION_VALUE to the CCCD on the peripheral
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun handlePacket(bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        Timber.d("PKT [%d] %s", bytes.size, hex)
        scope.launch { _packetFlow.emit(bytes) }
    }

    private fun handleError(message: String, status: Int) {
        Timber.e("%s (status=%d)", message, status)
        closeGatt()
        _connectionState.value = ConnectionState.Error(message, status)
    }

    private fun closeGatt() {
        gatt?.close()
        gatt = null
    }

    private fun rtrDeviceFrom(device: BluetoothDevice) = RtrDevice(
        address = device.address,
        name = device.name ?: "Unknown",
        rssi = 0,
    )

    private fun buildPropertyString(props: Int): String {
        val parts = mutableListOf<String>()
        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0)   parts += "READ"
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0)  parts += "WRITE"
        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) parts += "NOTIFY"
        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) parts += "INDICATE"
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) parts += "WRITE_NR"
        return if (parts.isEmpty()) "UNKNOWN" else parts.joinToString("|")
    }
}
