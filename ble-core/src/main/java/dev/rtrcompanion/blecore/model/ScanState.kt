package dev.rtrcompanion.blecore.model

/**
 * Represents the current BLE scan lifecycle and its results.
 */
sealed class ScanState {

    /** Scan has not been started or was explicitly stopped. */
    data object Idle : ScanState()

    /**
     * Actively scanning.
     * @param found Devices discovered so far (deduplicated by address).
     */
    data class Scanning(val found: List<RtrDevice> = emptyList()) : ScanState()

    /**
     * Scan completed (timeout or stopped), with accumulated results.
     * @param found All devices found during the scan session.
     */
    data class Stopped(val found: List<RtrDevice>) : ScanState()

    /**
     * Scan failed before or during execution.
     * @param errorCode  Android BluetoothLeScanner error code.
     * @param message    Human-readable description.
     */
    data class Failed(val errorCode: Int, val message: String) : ScanState()
}
