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
import dev.rtrcompanion.blecore.ProtocolMode
import dev.rtrcompanion.blecore.auth.HandshakeManager
import dev.rtrcompanion.blecore.model.ConnectionState
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.blecore.ping.PingPacketBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
 * ## Responsibilities
 *  - Connect to a given [BluetoothDevice]
 *  - Discover services and log them
 *  - Enable notifications on [BleConstants.CHAR_NOTIFY]
 *  - Capture all raw notification bytes via [packetFlow]
 *
 * ## Protocol Mode
 *
 * This manager operates in one of two modes controlled by [ProtocolMode]:
 *
 * **PASSIVE (default):** Only enables CCCD notifications. No writes to CHAR_WRITE.
 * HandshakeManager and PingPacketBuilder are NOT invoked.
 * A warning is logged at startup.
 *
 * **EXPERIMENTAL:** Enables the authentication handshake and keep-alive ping.
 * Only use after verifying the Jupiter AES key and ping format on RTR 310 hardware
 * via btsnoop HCI log. See docs/security/BLE_WRITE_AUDIT.md.
 *
 * Caller must hold BLUETOOTH_CONNECT permission before calling [connect].
 */
@SuppressLint("MissingPermission") // Caller is responsible for runtime permission
class RtrGattManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val protocolMode: ProtocolMode = ProtocolMode.PASSIVE,
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
    private var pingJob: Job? = null
    private var handshakeComplete = false

    /**
     * Time to wait after enabling notifications before starting ping,
     * even if no challenge packet is received.
     *
     * Some units may not send a challenge but still require the ping
     * to maintain the connection.
     */
    private val HANDSHAKE_TIMEOUT_MS = 3_000L

    // -------------------------------------------------------------------------
    // GATT Callback
    // -------------------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        /**
         * Called when the GATT connection state changes.
         *
         * On STATE_CONNECTED: schedules service discovery after a stabilisation delay.
         * On STATE_DISCONNECTED: releases all resources and resets state.
         * On error: transitions to [ConnectionState.Error].
         */
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

                    // Short delay recommended by Android BLE documentation before
                    // requesting service discovery to ensure a stable connection.
                    scope.launch {
                        delay(BleConstants.SERVICE_DISCOVERY_DELAY_MS)
                        Timber.i("Starting service discovery...")
                        _connectionState.value = ConnectionState.DiscoveringServices(device)
                        gatt.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.i("Disconnected")
                    stopPing()
                    closeGatt()
                    handshakeComplete = false
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        /**
         * Called when service discovery completes.
         *
         * Logs all discovered services and characteristics, then enables notifications
         * on CHAR_NOTIFY. After a timeout, starts the ping loop if handshake hasn't
         * completed (handles bikes that don't send a challenge).
         */
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

            // Log protocol mode warning
            if (protocolMode == ProtocolMode.PASSIVE) {
                Timber.w("⚠️ Experimental protocol writes disabled (PASSIVE mode). " +
                    "App is passive: scan → connect → discover → enable notifications → capture. " +
                    "No writes to CHAR_WRITE (0x5352).")
            } else {
                Timber.w("⚠️ EXPERIMENTAL protocol mode active. " +
                    "Handshake and ping writes are ENABLED. " +
                    "Jupiter AES key is UNVERIFIED on RTR 310.")
            }

            // Start ping after timeout only in EXPERIMENTAL mode
            if (protocolMode == ProtocolMode.EXPERIMENTAL) {
                scope.launch {
                    delay(HANDSHAKE_TIMEOUT_MS)
                    if (!handshakeComplete) {
                        Timber.i("No challenge received — starting ping without handshake")
                        startPing()
                    }
                }
            }
        }

        /**
         * Called when a characteristic changes (API < 33, deprecated path).
         *
         * Delegates to [handlePacket]. Both this and the API 33+ override are
         * implemented per ADR-003 to support all API levels from 29 to 35.
         */
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

        /**
         * Called when a characteristic changes (API 33+).
         *
         * The value is delivered as a separate parameter, avoiding the race
         * condition present in the deprecated API. See ADR-003.
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handlePacket(value)
        }

        /**
         * Called after a descriptor write completes.
         *
         * A successful CCCD write confirms notifications are enabled on CHAR_NOTIFY.
         */
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

        /**
         * Called after a characteristic write completes.
         *
         * Logs success/failure of handshake response and ping writes.
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Write success on %s",
                    characteristic.uuid.toString().substring(4, 8).uppercase())
            } else {
                Timber.e("Write failed on %s: status=%d",
                    characteristic.uuid.toString().substring(4, 8).uppercase(), status)
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
        stopPing()
        gatt?.disconnect()
        // closeGatt() is called in onConnectionStateChange when STATE_DISCONNECTED fires
    }

    // -------------------------------------------------------------------------
    // Packet handling
    // -------------------------------------------------------------------------

    /**
     * Processes a raw notification packet received from CHAR_NOTIFY.
     *
     * In PASSIVE mode: emits all packets to [packetFlow] without any writes.
     * In EXPERIMENTAL mode: if this is an authentication challenge, builds and
     * sends the response; otherwise emits the packet to [packetFlow].
     */
    private fun handlePacket(bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        Timber.d("PKT [%d] %s", bytes.size, hex)

        if (protocolMode == ProtocolMode.EXPERIMENTAL && HandshakeManager.isChallenge(bytes)) {
            Timber.i("HandshakeManager: challenge received, sending response (EXPERIMENTAL mode)")
            val response = HandshakeManager.buildResponse(bytes)
            if (response != null) {
                writeToCharWrite(response)
                handshakeComplete = true
                startPing()
            } else {
                Timber.e("HandshakeManager: failed to build response")
            }
            return
        }

        scope.launch { _packetFlow.emit(bytes) }
    }

    // -------------------------------------------------------------------------
    // Write helpers
    // -------------------------------------------------------------------------

    /**
     * Writes [data] to CHAR_WRITE (0x5352).
     *
     * All writes go through this single method to make auditing straightforward.
     * See `docs/security/BLE_WRITE_AUDIT.md`.
     *
     * In [ProtocolMode.PASSIVE] mode this method is a hard no-op — it logs a
     * warning and returns immediately without writing anything.
     */
    private fun writeToCharWrite(data: ByteArray) {
        if (protocolMode == ProtocolMode.PASSIVE) {
            Timber.w("writeToCharWrite: BLOCKED — app is in PASSIVE mode. No writes to CHAR_WRITE.")
            return
        }

        val currentGatt = gatt ?: run {
            Timber.w("writeToCharWrite: no active GATT connection")
            return
        }

        val service = currentGatt.getService(BleConstants.SERVICE_TVS_PROPRIETARY) ?: run {
            Timber.e("writeToCharWrite: TVS service not found")
            return
        }

        val characteristic = service.getCharacteristic(BleConstants.CHAR_WRITE) ?: run {
            Timber.e("writeToCharWrite: CHAR_WRITE not found")
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            currentGatt.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            )
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            currentGatt.writeCharacteristic(characteristic)
        }
    }

    // -------------------------------------------------------------------------
    // Keep-alive ping
    // -------------------------------------------------------------------------

    /**
     * Starts the periodic keep-alive ping coroutine.
     *
     * Sends a [PingPacketBuilder] packet to CHAR_WRITE every
     * [BleConstants.PING_INTERVAL_MS]. Safe to call multiple times
     * (stops any existing ping job first).
     */
    private fun startPing() {
        stopPing()
        Timber.i("Ping: starting keep-alive at %dms interval", BleConstants.PING_INTERVAL_MS)
        pingJob = scope.launch {
            while (true) {
                val ping = PingPacketBuilder.build()
                writeToCharWrite(ping)
                delay(BleConstants.PING_INTERVAL_MS)
            }
        }
    }

    /** Stops the keep-alive ping coroutine. */
    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    // -------------------------------------------------------------------------
    // Notification enable
    // -------------------------------------------------------------------------

    /**
     * Enables BLE notifications on CHAR_NOTIFY by writing the CCCD descriptor.
     *
     * This is the only CCCD write in the codebase — it enables passive listening
     * and does not send any data to the bike's application layer.
     */
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

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun handleError(message: String, status: Int) {
        Timber.e("%s (status=%d)", message, status)
        stopPing()
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
        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0)            parts += "READ"
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0)           parts += "WRITE"
        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)          parts += "NOTIFY"
        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)        parts += "INDICATE"
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) parts += "WRITE_NR"
        return if (parts.isEmpty()) "UNKNOWN" else parts.joinToString("|")
    }
}
