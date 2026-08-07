package dev.rtrcompanion.app

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import dev.rtrcompanion.app.export.PacketExporter
import dev.rtrcompanion.app.ui.RtrCompanionApp
import dev.rtrcompanion.app.ui.theme.RtrCompanionTheme
import dev.rtrcompanion.app.viewmodel.MainViewModel
import timber.log.Timber

/**
 * Single-activity host. Handles BLE runtime permissions then hands off
 * entirely to Compose.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    // -------------------------------------------------------------------------
    // Permission launcher
    // -------------------------------------------------------------------------

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        Timber.i("Permission results: %s", results)
        if (allGranted) {
            viewModel.onPermissionsGranted()
        } else {
            viewModel.onPermissionsDenied()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RtrCompanionTheme {
                RtrCompanionApp(
                    viewModel = viewModel,
                    onRequestPermissions = ::requestBlePermissions,
                    onOpenSettings = ::openAppSettings,
                    onExportLog = ::exportPacketLog,
                )
            }
        }

        // Kick off permission check immediately so the UI can react
        requestBlePermissions()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun requestBlePermissions() {
        val required = blePermissions()
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.onPermissionsGranted()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    /**
     * Builds a share intent from the current packet log and launches the
     * Android share sheet. No-op if the log is empty.
     */
    private fun exportPacketLog() {
        val intent = PacketExporter.createShareIntent(this, viewModel.packetLogger)
        if (intent == null) {
            Timber.w("exportPacketLog: nothing to export")
            return
        }
        startActivity(Intent.createChooser(intent, "Share capture"))
    }
}

/**
 * Returns the set of BLE permissions needed for the running API level.
 */
fun blePermissions(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
} else {
    listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
}
