package dev.rtrcompanion.blecore.model

/**
 * Represents the current BLE scan lifecycle and accumulated results.
 *
 * State transitions:
 * ```
 * Idle → Scanning → Stopped  (normal completion or user stop)
 * Idle → Scanning → Failed   (hardware or system error)
 * ```
 *
 * Emitted by [dev.rtrcompanion.blecore.scanner.RtrScanner.scanState].
 */
sealed class ScanState {

    /** Scan has not been started or was explicitly stopped. Initial state. */
    data object Idle : ScanState()

    /**
     * BLE scan is actively running.
     *
     * @param found Devices discovered so far, deduplicated by MAC address.
     *              Updates incrementally as new devices are found.
     */
    data class Scanning(val found: List<RtrDevice> = emptyList()) : ScanState()

    /**
     * Scan completed (timeout or user stop) with accumulated results.
     *
     * @param found All devices found during the scan session.
     */
    data class Stopped(val found: List<RtrDevice>) : ScanState()

    /**
     * Scan failed before or during execution.
     *
     * @param errorCode  Android [android.bluetooth.le.ScanCallback] error code.
     * @param message    Human-readable description.
     */
    data class Failed(val errorCode: Int, val message: String) : ScanState()
}
