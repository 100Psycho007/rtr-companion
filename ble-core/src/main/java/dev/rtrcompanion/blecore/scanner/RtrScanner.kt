package dev.rtrcompanion.blecore.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import dev.rtrcompanion.blecore.BleConstants
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.blecore.model.ScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Scans for RTR 310 BLE devices and exposes results as a [StateFlow].
 *
 * Thread-safety: all state mutations are done on the coroutine scope provided.
 * Caller is responsible for ensuring BLUETOOTH_SCAN permission is granted
 * before calling [startScan].
 *
 * Usage:
 * ```kotlin
 * val scanner = RtrScanner(context, viewModelScope)
 * scanner.scanState.collect { state -> /* update UI */ }
 * scanner.startScan()
 * ```
 */
@SuppressLint("MissingPermission") // Caller is responsible for runtime permission
class RtrScanner(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    /** Observable scan state. Collect from UI layer. */
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Deduplicated device map: address → RtrDevice
    private val discovered = mutableMapOf<String, RtrDevice>()

    private var timeoutJob: Job? = null

    private val bluetoothLeScanner by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter?.bluetoothLeScanner
    }

    // -------------------------------------------------------------------------
    // ScanCallback
    // -------------------------------------------------------------------------

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            if (!name.startsWith(BleConstants.DEVICE_NAME_PREFIX)) return

            val device = RtrDevice(
                address = result.device.address,
                name = name,
                rssi = result.rssi,
            )

            if (!discovered.containsKey(device.address)) {
                Timber.i("RTR Found: %s (%s) RSSI=%d", device.name, device.address, device.rssi)
                discovered[device.address] = device
                _scanState.value = ScanState.Scanning(discovered.values.toList())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val message = "Scan failed with error code $errorCode"
            Timber.e(message)
            cancelTimeout()
            _scanState.value = ScanState.Failed(errorCode, message)
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Starts a BLE scan filtered for RTR 310 devices.
     * Automatically stops after [BleConstants.SCAN_TIMEOUT_MS].
     *
     * Does nothing if a scan is already running.
     */
    fun startScan() {
        if (_scanState.value is ScanState.Scanning) {
            Timber.d("Scan already running, ignoring startScan()")
            return
        }

        val scanner = bluetoothLeScanner
        if (scanner == null) {
            Timber.e("BluetoothLeScanner unavailable — is Bluetooth enabled?")
            _scanState.value = ScanState.Failed(-1, "Bluetooth not available")
            return
        }

        discovered.clear()
        Timber.i("Searching...")
        _scanState.value = ScanState.Scanning(emptyList())

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // We rely on name prefix filtering in onScanResult rather than
        // ScanFilter because Android may not filter by partial name on all OEMs.
        val filters = listOf<ScanFilter>()

        scanner.startScan(filters, settings, scanCallback)

        timeoutJob = scope.launch {
            delay(BleConstants.SCAN_TIMEOUT_MS)
            Timber.i("Scan timeout reached")
            stopScan()
        }
    }

    /**
     * Stops a running scan and emits [ScanState.Stopped] with collected devices.
     * Safe to call when not scanning.
     */
    fun stopScan() {
        cancelTimeout()
        runCatching { bluetoothLeScanner?.stopScan(scanCallback) }
        Timber.i("Scan stopped. Found %d device(s).", discovered.size)
        _scanState.value = ScanState.Stopped(discovered.values.toList())
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
