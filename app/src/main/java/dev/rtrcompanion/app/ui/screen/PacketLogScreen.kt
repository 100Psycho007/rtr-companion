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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rtrcompanion.blecore.model.RtrDevice
import dev.rtrcompanion.protocol.RawPacket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live packet log screen.
 *
 * Each packet row shows:
 *  - Timestamp + length
 *  - Raw hex dump
 *  - Inline decoded annotation (message type label + any partially decoded fields)
 *
 * Annotations are best-effort from confirmed/hypothesis data in PROTOCOL_STATUS.md.
 * Unknown message IDs are labelled "?" to make them easy to spot.
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Packet type breakdown summary
                val summary = buildTypeSummary(packets)
                Text(
                    "${packets.size} pkts  $summary",
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

// ─────────────────────────────────────────────────────────────────────────────
// Packet annotation logic
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a human-readable annotation for a packet based on its message ID.
 *
 * Confidence levels match PROTOCOL_STATUS.md:
 *  - CONFIRMED fields are labelled directly
 *  - HYPOTHESIS fields are labelled with "?"
 *  - UNRESOLVED fields are labelled "?"
 */
private data class PacketAnnotation(
    val label: String,        // e.g. "0x10  Odometer/Fuel?"
    val detail: String,       // e.g. "chk=PASS  odo?=0xF624DB  fuel?=0xCA"
    val isKnown: Boolean,     // false = grey, true = coloured
    val checksumOk: Boolean,
)

private fun annotate(packet: RawPacket): PacketAnnotation {
    val b = packet.bytes
    if (b.size != 20) {
        return PacketAnnotation(
            label = "BAD LENGTH (${b.size}B)",
            detail = "",
            isKnown = false,
            checksumOk = false,
        )
    }

    val frameType = b[0]
    val msgId = b[1]

    // Checksum formula: (C - sum(B0..B17)) mod 256
    // C values authoritative from 733-packet btsnoop capture 2026-08-16.
    val checksumConstant: Int? = when (msgId) {
        0x10.toByte() -> 0x49   // 351-sample confirmed
        0x11.toByte() -> 0xDD   // 5-sample confirmed
        0x12.toByte() -> 0x59   // 351-sample confirmed
        0x7D.toByte() -> 0x29   // consistent both captures
        0x42.toByte() -> 0x82   // 5-sample confirmed
        else          -> null   // 0x5F: frame counter dependency — UNRESOLVED
    }
    val checksumOk = if (checksumConstant != null) {
        val sum = b.slice(0..17).sumOf { it.toInt() and 0xFF }
        val expected = ((checksumConstant - sum) and 0xFF).toByte()
        b[18] == expected
    } else false

    val chkLabel = when {
        checksumConstant == null -> "chk=?"
        checksumOk               -> "chk=✓"
        else                     -> "chk=✗"
    }

    val frameLabel = when (frameType) {
        0x5A.toByte() -> "DATA"
        0x5B.toByte() -> "CTRL"
        0x9A.toByte() -> "AUTH"  // Challenge or response frame (Jupiter hypothesis)
        else          -> "0x%02X".format(frameType)
    }

    return when (msgId) {

        // ── 0x10 Odometer / Fuel (Jupiter RE — field positions UNVERIFIED on RTR 310) ──
        0x10.toByte() -> {
            // Jupiter: odo = B3-B5 big-endian UInt24 / 10.0, fuel = lower nibble B6
            val odoRaw = ((b[3].toInt() and 0xFF) shl 16) or
                         ((b[4].toInt() and 0xFF) shl 8) or
                          (b[5].toInt() and 0xFF)
            val fuelLo = b[6].toInt() and 0x0F
            val fuelHi = (b[6].toInt() and 0xF0) shr 4
            PacketAnnotation(
                label  = "[$frameLabel] 0x10  Odometer/Fuel?",
                detail = "$chkLabel  odo?=${odoRaw} (÷10=${odoRaw / 10.0}km?)  fuel?=lo:$fuelLo hi:$fuelHi  dyn:B13=0x%02X".format(b[13]),
                isKnown = true,
                checksumOk = checksumOk,
            )
        }

        // ── 0x11 Service Reminder (Jupiter RE — UNVERIFIED on RTR 310) ──
        0x11.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0x11  Service?",
            detail = "$chkLabel  B4=0x%02X(svc?)  dyn:B10=0x%02X".format(b[4], b[10]),
            isKnown = true,
            checksumOk = checksumOk,
        )

        // ── 0x12 Unknown static (RTR 310 specific) ──
        0x12.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0x12  Unknown (static)",
            detail = "$chkLabel  B2-B7=0x%02X %02X %02X %02X %02X %02X".format(
                b[2], b[3], b[4], b[5], b[6], b[7]),
            isKnown = false,
            checksumOk = checksumOk,
        )

        // ── 0x5F Live Telemetry (dynamic, checksum UNRESOLVED) ──
        0x5F.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0x5F  Live Telemetry",
            detail = "chk=?  fcnt?=0x%02X  dyn:B8-B13=%02X %02X %02X %02X %02X %02X  B17=0x%02X".format(
                b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[17]),
            isKnown = true,
            checksumOk = false,
        )

        // ── 0x7D Unknown static dense (RTR 310 specific) ──
        0x7D.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0x7D  Unknown (dense static)",
            detail = "$chkLabel  no 0xEA bytes  B2-B9=%02X %02X %02X %02X %02X %02X %02X %02X".format(
                b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9]),
            isKnown = false,
            checksumOk = checksumOk,
        )

        // ── 0x42 Control heartbeat ──
        0x42.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0x42  Heartbeat?",
            detail = "$chkLabel  B12=0x%02X".format(b[12]),
            isKnown = true,
            checksumOk = checksumOk,
        )

        // ── 0xF2 Auth challenge (Jupiter hypothesis, NOT observed on RTR 310) ──
        0xF2.toByte() -> PacketAnnotation(
            label  = "[$frameLabel] 0xF2  Auth Challenge (Jupiter?)",
            detail = "chk=?  challenge=B2-B17",
            isKnown = true,
            checksumOk = false,
        )

        // ── Unknown ──
        else -> PacketAnnotation(
            label  = "[$frameLabel] 0x%02X  UNKNOWN".format(msgId),
            detail = "B2-B5=0x%02X %02X %02X %02X".format(b[2], b[3], b[4], b[5]),
            isKnown = false,
            checksumOk = false,
        )
    }
}

/** Builds a compact type-count summary string, e.g. "10×5F  3×10  3×11  3×12". */
private fun buildTypeSummary(packets: List<RawPacket>): String {
    if (packets.isEmpty()) return ""
    return packets
        .filter { it.bytes.size == 20 }
        .groupingBy { it.bytes[1] }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(6)
        .joinToString("  ") { (id, cnt) -> "${cnt}×%02X".format(id) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composables
// ─────────────────────────────────────────────────────────────────────────────

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

@Composable
private fun PacketRow(packet: RawPacket) {
    val ann = annotate(packet)

    val cardColor = when {
        !ann.isKnown                           -> MaterialTheme.colorScheme.surfaceVariant
        ann.checksumOk                         -> MaterialTheme.colorScheme.surface
        else                                   -> MaterialTheme.colorScheme.surface
    }
    val labelColor = when {
        !ann.isKnown                           -> MaterialTheme.colorScheme.onSurfaceVariant
        packet.bytes.size == 20 &&
            packet.bytes[1] == 0x5F.toByte()   -> MaterialTheme.colorScheme.primary
        ann.checksumOk                         -> MaterialTheme.colorScheme.primary
        else                                   -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Top row: timestamp / size / annotation label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    timeFmt.format(Date(packet.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    ann.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                Text(
                    "${packet.bytes.size}B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Hex dump
            Text(
                packet.hex,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            // Decoded detail
            if (ann.detail.isNotEmpty()) {
                Text(
                    ann.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
