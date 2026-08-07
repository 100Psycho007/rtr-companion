package dev.rtrcompanion.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.protocol.RawPacket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live packet log screen (Sprint 3 update).
 *
 * Displays every raw notification received from CHAR_NOTIFY in
 * chronological order. Auto-scrolls to the latest entry.
 * Adds Export button that shares the session as a plain-text file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketLogScreen(
    device: RtrDevice,
    packets: List<RawPacket>,
    onDisconnect: () -> Unit,
    onClearLog: () -> Unit,
    onExport: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    // Auto-scroll to newest packet
    LaunchedEffect(packets.size) {
        if (packets.isNotEmpty()) {
            listState.animateScrollToItem(packets.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Packet Log")
                        Text(
                            device.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Disconnect")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${packets.size} packets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExport,
                        enabled = packets.isNotEmpty(),
                    ) {
                        Text("Export")
                    }
                    OutlinedButton(onClick = onClearLog) {
                        Text("Clear")
                    }
                }
            }

            // Packet list
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(packets, key = { "${it.timestamp}_${it.hashCode()}" }) { packet ->
                    PacketRow(packet)
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

@Composable
private fun PacketRow(packet: RawPacket) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    timeFmt.format(Date(packet.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${packet.bytes.size}B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                packet.hex,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
