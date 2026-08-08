package dev.rtrcompanion.blecore.ping

import dev.rtrcompanion.blecore.BleConstants
import dev.rtrcompanion.blecore.auth.HandshakeManager
import java.util.Calendar

/**
 * Builds the TVS SmartXonnect keep-alive ping packet (Data ID `0x4A`).
 *
 * ## Status: HYPOTHESIS — UNVERIFIED on RTR 310
 *
 * This packet format is derived entirely from the JupiterRideCompanion RE report.
 * It has NOT been observed or confirmed on the RTR 310.
 * This class is ONLY invoked when [dev.rtrcompanion.blecore.connection.RtrGattManager]
 * is in [dev.rtrcompanion.blecore.ProtocolMode.EXPERIMENTAL] mode.
 * The default mode is PASSIVE — this class is never called.
 *
 * See `docs/research/JUPITER_CROSS_REFERENCE.md` and
 * `docs/security/BLE_WRITE_AUDIT.md` before enabling.
 *
 * ## Packet Layout (20 bytes, Phone → Bike) — from Jupiter RE
 *
 * ```
 * Byte  0  : 0x5B (FRAME_CONTROL)
 * Byte  1  : 0x4A (MSG_PING)
 * Byte  2  : Signal/Battery — upper nibble: signal bars (0–5),
 *                              lower nibble: battery bars (0–5)
 * Byte  3  : 0x00 (padding)
 * Byte  4  : Temperature — ambient °C + 40 (e.g. 25°C = 0x41)
 * Byte  5  : 0x00 (padding)
 * Byte  6  : Hour (12-hour format, 1–12)
 * Byte  7  : Minute (0–59)
 * Byte  8  : Second (0–59)
 * Byte  9  : AM/PM — 0x00 = AM, 0x01 = PM
 * Byte 10  : 0x00 (padding)
 * Byte 11  : Network type — 0x04 = LTE/4G
 * Byte 12  : Day (1–31)
 * Byte 13  : Month (1–12)
 * Byte 14  : Year mod 100 (e.g. 2026 = 26)
 * Byte 15  : 0x00 (padding)
 * Byte 16  : 0x00 (padding)
 * Byte 17  : Find Me — 0x01 = flash lights + beep, 0x00 = off
 * Byte 18  : Checksum — formula from Jupiter RE (UNVERIFIED on RTR 310)
 * Byte 19  : 0xFF (PACKET_END)
 * ```
 *
 * Source: JupiterRideCompanion RE report (same TVS SmartXonnect protocol).
 * Cross-reference: `docs/research/JUPITER_CROSS_REFERENCE.md`
 */
object PingPacketBuilder {

    /** Network type value for LTE/4G. */
    private const val NETWORK_LTE: Byte = 0x04

    /**
     * Builds a ping packet with current system time and the provided phone status.
     *
     * @param signalBars    Phone signal bars (0–5).
     * @param batteryBars   Phone battery bars (0–5).
     * @param temperatureC  Ambient temperature in Celsius (-40 to 85 range).
     * @param findMe        Whether to trigger the bike's Find Me feature.
     * @return A 20-byte packet ready to write to CHAR_WRITE.
     */
    fun build(
        signalBars: Int = 0,
        batteryBars: Int = 0,
        temperatureC: Int = 25,
        findMe: Boolean = false,
    ): ByteArray {
        val cal = Calendar.getInstance()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else        -> hour24
        }
        val isAfternoon = hour24 >= 12

        val packet = ByteArray(20)

        packet[0]  = BleConstants.FRAME_CONTROL                               // 0x5B
        packet[1]  = BleConstants.MSG_PING                                    // 0x4A
        packet[2]  = encodeSignalBattery(signalBars, batteryBars)
        packet[3]  = 0x00
        packet[4]  = (temperatureC + 40).coerceIn(0, 255).toByte()
        packet[5]  = 0x00
        packet[6]  = hour12.toByte()
        packet[7]  = cal.get(Calendar.MINUTE).toByte()
        packet[8]  = cal.get(Calendar.SECOND).toByte()
        packet[9]  = if (isAfternoon) 0x01 else 0x00
        packet[10] = 0x00
        packet[11] = NETWORK_LTE
        packet[12] = cal.get(Calendar.DAY_OF_MONTH).toByte()
        packet[13] = (cal.get(Calendar.MONTH) + 1).toByte()                   // Calendar.MONTH is 0-based
        packet[14] = (cal.get(Calendar.YEAR) % 100).toByte()
        packet[15] = 0x00
        packet[16] = 0x00
        packet[17] = if (findMe) 0x01 else 0x00
        packet[18] = HandshakeManager.computeChecksum(packet, 0, 18)
        packet[19] = BleConstants.PACKET_END                                  // 0xFF

        return packet
    }

    /**
     * Encodes signal bars (upper nibble) and battery bars (lower nibble)
     * into a single byte.
     *
     * Both values are clamped to 0–5.
     */
    private fun encodeSignalBattery(signalBars: Int, batteryBars: Int): Byte {
        val signal  = signalBars.coerceIn(0, 5)
        val battery = batteryBars.coerceIn(0, 5)
        return ((signal shl 4) or battery).toByte()
    }
}
