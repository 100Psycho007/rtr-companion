package dev.rtrcompanion.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rtrcompanion.app.ui.screen.PacketLogScreen
import dev.rtrcompanion.app.ui.screen.PermissionScreen
import dev.rtrcompanion.app.ui.screen.ScanScreen
import dev.rtrcompanion.app.viewmodel.MainViewModel
import dev.rtrcompanion.app.viewmodel.PermissionState
import dev.rtrcompanion.blecore.model.ConnectionState

/**
 * Root composable. Routes between screens based on app state.
 *
 * Navigation logic (no NavController needed for Sprint 1–3):
 *   permissions not granted → PermissionScreen
 *   permissions granted, not ready → ScanScreen
 *   ready (notifications enabled) → PacketLogScreen
 *
 * @param onExportLog Called when the user taps "Export" on the packet log screen.
 *                    The Activity handles the actual share Intent so it can call
 *                    [android.app.Activity.startActivity].
 */
@Composable
fun RtrCompanionApp(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportLog: () -> Unit = {},
) {
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val packetLog by viewModel.packetLog.collectAsStateWithLifecycle()

    when {
        permissionState == PermissionState.DENIED -> {
            PermissionScreen(
                onRequestPermissions = onRequestPermissions,
                onOpenSettings = onOpenSettings,
            )
        }

        connectionState is ConnectionState.Ready -> {
            PacketLogScreen(
                device = (connectionState as ConnectionState.Ready).device,
                packets = packetLog,
                onDisconnect = viewModel::disconnect,
                onClearLog = viewModel::clearLog,
                onExport = onExportLog,
            )
        }

        else -> {
            ScanScreen(
                scanState = scanState,
                connectionState = connectionState,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
            )
        }
    }
}
