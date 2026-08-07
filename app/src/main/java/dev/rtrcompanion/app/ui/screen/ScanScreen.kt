package dev.rtrcompanion.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rtrcompanion.blecore.model.ConnectionState
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.blecore.model.ScanState

/**
 * Scan + connect screen (Sprint 1).
 *
 * Shows:
 * - Scan / Stop button
 * - List of discovered RTR devices
 * - Current connection status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    scanState: ScanState,
    connectionState: ConnectionState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (RtrDevice) -> Unit,
    onDisconnect: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("RTR Companion") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            // Connection status banner
            ConnectionStatusBanner(connectionState, onDisconnect)

            Spacer(modifier = Modifier.height(12.dp))

            // Scan controls
            ScanControls(scanState, onStartScan, onStopScan)

            Spacer(modifier = Modifier.height(16.dp))

            // Device list
            val devices: List<RtrDevice> = when (scanState) {
                is ScanState.Scanning -> scanState.found
                is ScanState.Stopped  -> scanState.found
                else                  -> emptyList()
            }

            if (devices.isEmpty() && scanState is ScanState.Scanning) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Searching...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Text(
                    text = "Devices found: ${devices.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.address }) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = connectionState is ConnectionState.Connecting,
                            onClick = { onConnect(device) },
                        )
                    }
                }
            }

            if (scanState is ScanState.Failed) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan failed: ${scanState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ScanControls(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    val isScanning = scanState is ScanState.Scanning
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onStartScan,
            enabled = !isScanning,
        ) {
            Text("Scan")
        }
        OutlinedButton(
            onClick = onStopScan,
            enabled = isScanning,
        ) {
            Text("Stop")
        }
    }
}

@Composable
private fun ConnectionStatusBanner(
    state: ConnectionState,
    onDisconnect: () -> Unit,
) {
    val label = when (state) {
        is ConnectionState.Disconnected       -> return // show nothing
        is ConnectionState.Connecting         -> "Connecting..."
        is ConnectionState.Connected          -> "Connected to ${state.device.name}"
        is ConnectionState.DiscoveringServices-> "Discovering services..."
        is ConnectionState.Ready              -> "Ready"
        is ConnectionState.Error              -> "Error: ${state.message}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (state !is ConnectionState.Disconnected && state !is ConnectionState.Error) {
                OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: RtrDevice,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
