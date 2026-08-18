package dev.rtrcompanion.app.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.rtrcompanion.blecore.ProtocolMode
import dev.rtrcompanion.blecore.connection.RtrGattManager
import dev.rtrcompanion.blecore.model.ConnectionState
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.blecore.model.ScanState
import dev.rtrcompanion.blecore.scanner.RtrScanner
import dev.rtrcompanion.protocol.PacketLogger
import dev.rtrcompanion.protocol.RawPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main ViewModel — bridges the BLE SDK and the Compose UI.
 *
 * State exposed:
 *  - [scanState]        : live scan results
 *  - [connectionState]  : GATT lifecycle
 *  - [packetLog]        : accumulated raw packets
 *  - [permissionState]  : whether BLE permissions are granted
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // -------------------------------------------------------------------------
    // SDK instances
    // -------------------------------------------------------------------------

    private val scanner = RtrScanner(application, viewModelScope)
    private val gattManager = RtrGattManager(
        context = application,
        scope = viewModelScope,
        protocolMode = ProtocolMode.ACTIVE,
        userName = "RTR Companion",
        vehicleName = "RTR 310",
    )

    /**
     * Exposed so [dev.rtrcompanion.app.export.PacketExporter] can read the log
     * from the Activity when the user taps "Export".
     */
    val packetLogger = PacketLogger()

    // -------------------------------------------------------------------------
    // Permission state
    // -------------------------------------------------------------------------

    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // -------------------------------------------------------------------------
    // Delegated state from SDK
    // -------------------------------------------------------------------------

    val scanState: StateFlow<ScanState> = scanner.scanState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScanState.Idle)

    val connectionState: StateFlow<ConnectionState> = gattManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val packetLog: StateFlow<List<RawPacket>> = packetLogger.log
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // -------------------------------------------------------------------------
    // Init — collect packets from GATT and feed the logger
    // -------------------------------------------------------------------------

    init {
        viewModelScope.launch {
            gattManager.packetFlow.collect { bytes ->
                packetLogger.record(RawPacket(bytes))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Permission callbacks (called by MainActivity)
    // -------------------------------------------------------------------------

    fun onPermissionsGranted() {
        Timber.i("BLE permissions granted")
        _permissionState.value = PermissionState.GRANTED
    }

    fun onPermissionsDenied() {
        Timber.w("BLE permissions denied")
        _permissionState.value = PermissionState.DENIED
    }

    // -------------------------------------------------------------------------
    // User actions
    // -------------------------------------------------------------------------

    /** Start scanning for RTR 310 devices. No-op if permissions not granted. */
    fun startScan() {
        if (_permissionState.value != PermissionState.GRANTED) {
            Timber.w("Cannot scan — permissions not granted")
            return
        }
        scanner.startScan()
    }

    /** Stop an active scan. */
    fun stopScan() = scanner.stopScan()

    /**
     * Connect to a discovered device.
     * Looks up the [BluetoothDevice] from the Android BluetoothManager using
     * the address carried in [RtrDevice].
     */
    fun connect(device: RtrDevice) {
        val bluetoothManager =
            getApplication<Application>().getSystemService(BluetoothManager::class.java)
        val btDevice = bluetoothManager?.adapter?.getRemoteDevice(device.address)
        if (btDevice == null) {
            Timber.e("Could not get BluetoothDevice for %s", device.address)
            return
        }
        gattManager.connect(btDevice)
    }

    /** Disconnect from current device. */
    fun disconnect() = gattManager.disconnect()

    /** Clear the packet log. */
    fun clearLog() = packetLogger.clear()

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
        gattManager.disconnect()
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                MainViewModel(app)
            }
        }
    }
}

enum class PermissionState { UNKNOWN, GRANTED, DENIED }
