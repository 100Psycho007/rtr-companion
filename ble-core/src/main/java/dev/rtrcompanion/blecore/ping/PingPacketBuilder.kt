package dev.rtrcompanion.blecore.ping

import dev.rtrcompanion.blecore.BleConstants
import java.util.Calendar

/**
 * Builds the TVS SmartXonnect keep-alive ping packet (Data ID `0x4A`).
 *
 * ## Status: CONFIRMED from btsnoop HCI capture (2026-08-16)
 *
 * Format confirmed by XOR-decoding real captured ping packets:
 * raw `5B 4A DF 92 EA EA EE C4 CD EB EA EF FA E2 F0 EA EA EA EA FF`
 * XOR each byte with `0xEA` → `B1 A0 35 78 00 00 04 2E 27 01 00 05 10 08 1A 00 00 00 00 15`
 * Decoded: sig=3 bat=5 h=4 m=46 s=39 PM net=0x05 day=16 mon=8 yr=2026
 *
 * ## Encoding
 *
 * **Every byte including the frame header (B0, B1) is XOR-encoded with `0xEA`.**
 * The terminator `0xFF` is also XOR-encoded (0xFF XOR 0xEA = 0x15 on the wire).
 *
 * This is different from the name packets (0x52, 0x43) where the header bytes
 * are NOT XOR-encoded.
 *
 * ## Packet Layout (20 bytes — decoded values, before XOR)
 *
 * ```
 * Byte  0  : 0x5B (FRAME_CONTROL)
 * Byte  1  : 0x4A (MSG_PING)
 * Byte  2  : Signal/Battery — upper nibble: signal bars (0–5), lower nibble: battery bars (0–5)
 * Byte  3  : 0x78 (padding — observed constant in capture)
 * Byte  4  : Temperature °C + 40 (0x00 = not populated)
 * Byte  5  : 0x00 (padding)
 * Byte  6  : Hour (12-hour format, 1–12)
 * Byte  7  : Minute (0–59)
 * Byte  8  : Second (0–59) — increments each packet
 * Byte  9  : AM/PM — 0x00 = AM, 0x01 = PM
 * Byte 10  : 0x00 (padding)
 * Byte 11  : Network type (0x05 observed — possibly "5G" or network code)
 * Byte 12  : Day (1–31)
 * Byte 13  : Month (1–12)
 * Byte 14  : Year mod 100 (e.g. 2026 = 0x1A = 26)
 * Byte 15  : 0x00 (padding)
 * Byte 16  : 0x00 (padding)
 * Byte 17  : Find Me — 0x01 = flash lights + beep, 0x00 = off
 * Byte 18  : 0x00 (checksum decoded = 0x00 in all observed pings)
 * Byte 19  : 0xFF (PACKET_END)
 * ```
 *
 * All decoded values are XOR-encoded with `0xEA` before transmission.
 *
 * Source: `docs/protocol/capture-20260816-btsnoop.md`
 */
object PingPacketBuilder {

    /**
     * Builds a ping packet with current system time and the provided phone status.
     * All bytes are XOR-encoded with [BleConstants.XOR_KEY] before transmission.
     *
     * @param signalBars    Phone signal bars (0–5). Shown on cluster display.
     * @param batteryBars   Phone battery bars (0–5). Shown on cluster display.
     * @param temperatureC  Ambient temperature in Celsius. 0 = not populated.
     * @param findMe        Triggers the bike's Find Me feature (lights flash + beep).
     * @return A 20-byte XOR-encoded packet ready to write to CHAR_WRITE.
     */
    fun build(
        signalBars: Int = 0,
        batteryBars: Int = 0,
        temperatureC: Int = 0,
        findMe: Boolean = false,
    ): ByteArray {
        val cal = Calendar.getInstance()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = when {
            hour24 == 0  -> 12
            hour24 > 12  -> hour24 - 12
            else         -> hour24
        }
        val isAfternoon = hour24 >= 12

        // Build decoded (plain) packet first
        val decoded = ByteArray(20)
        decoded[0]  = BleConstants.FRAME_CONTROL                               // 0x5B
        decoded[1]  = BleConstants.MSG_PING                                    // 0x4A
        decoded[2]  = encodeSignalBattery(signalBars, batteryBars)
        decoded[3]  = 0x78.toByte()                                            // constant observed in capture
        decoded[4]  = if (temperatureC == 0) 0x00 else (temperatureC + 40).coerceIn(0, 255).toByte()
        decoded[5]  = 0x00
        decoded[6]  = hour12.toByte()
        decoded[7]  = cal.get(Calendar.MINUTE).toByte()
        decoded[8]  = cal.get(Calendar.SECOND).toByte()
        decoded[9]  = if (isAfternoon) 0x01 else 0x00
        decoded[10] = 0x00
        decoded[11] = 0x05.toByte()                                            // network code observed
        decoded[12] = cal.get(Calendar.DAY_OF_MONTH).toByte()
        decoded[13] = (cal.get(Calendar.MONTH) + 1).toByte()                   // Calendar.MONTH is 0-based
        decoded[14] = (cal.get(Calendar.YEAR) % 100).toByte()
        decoded[15] = 0x00
        decoded[16] = 0x00
        decoded[17] = if (findMe) 0x01 else 0x00
        decoded[18] = 0x00                                                     // checksum = 0x00 in all observed pings
        decoded[19] = BleConstants.PACKET_END                                  // 0xFF

        // XOR-encode every byte with 0xEA before transmission
        return ByteArray(20) { i ->
            (decoded[i].toInt() xor BleConstants.XOR_KEY.toInt()).toByte()
        }
    }

    private fun encodeSignalBattery(signalBars: Int, batteryBars: Int): Byte {
        val signal  = signalBars.coerceIn(0, 5)
        val battery = batteryBars.coerceIn(0, 5)
        return ((signal shl 4) or battery).toByte()
    }
}
