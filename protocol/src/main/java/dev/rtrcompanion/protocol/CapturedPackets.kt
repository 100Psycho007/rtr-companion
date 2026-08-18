package dev.rtrcompanion.protocol

/**
 * Development-only captured packet fixtures.
 *
 * These are real packets observed during hardware sessions with the RTR 310.
 * They are provided for offline analysis and testing ONLY.
 *
 * ## Safety rules
 *
 * 1. NONE of these packets should be sent to the bike automatically.
 * 2. They are only usable via the offline developer analysis screen.
 * 3. Meanings are labelled with their confidence level — do not upgrade
 *    a hypothesis to confirmed without hardware evidence.
 * 4. Do NOT add new entries here without documenting the source session.
 *
 * Source sessions: `docs/protocol/capture-20260808-150945.md`
 */
object CapturedPackets {

    // -------------------------------------------------------------------------
    // FIRST_CAPTURED_WRITE
    // -------------------------------------------------------------------------

    /**
     * The first application-level write observed in the HCI capture from a Lava
     * Android phone connecting to the RTR 310.
     *
     * **Source:** HCI bugreport capture, session 2026-08-02 (Lava phone).
     * **Characteristic:** CHAR_WRITE `00005352-0000-1000-8000-00805f9b34fb`
     * **Direction:** Phone → Bike (TX)
     * **Length:** 20 bytes
     *
     * ## ⚠️ IMPORTANT — Meaning NOT confirmed
     *
     * The full purpose of this packet is NOT yet confirmed. It may be:
     *  - A keep-alive ping packet (0x5B frame type, similar to 0x5B 0x4A)
     *  - An authentication response (unlikely — 0x5B not 0x9A frame type)
     *  - A device configuration packet
     *
     * **Do NOT treat this as an authentication credential or universal reusable packet.**
     * Captured authentication packets contain session-specific data.
     *
     * This packet is exposed for offline comparison and analysis ONLY.
     * See [ApacheProtocol.parseFrame] and the packet analysis screen.
     *
     * ## Partial analysis
     *
     * - Byte 0 = `0x5B` → CTRL frame type (not DATA `0x5A`, not AUTH `0x9A`)
     * - Byte 1 = `0x52` → message ID (unknown — not in current APK frame map)
     * - Bytes 2–17 = mixed data — not all `0xEA` null bytes, so payload is non-trivial
     * - Byte 18 = `0xFF` — if this is the checksum, the constant would be unusual
     * - Byte 19 = `0xFF` — standard terminator, or checksum doubled with terminator?
     *
     * The 0x52 message ID is not documented in `ApacheIncomingFrameIdentifier`.
     * It may be an outbound-only message type.
     */
    val FIRST_CAPTURED_WRITE: ByteArray = byteArrayOf(
        0x5B, 0x52.toByte(), 0xAE.toByte(), 0x82.toByte(), 0x8B.toByte(), 0x84.toByte(),
        0x9F.toByte(), 0x99.toByte(), 0x82.toByte(), 0xCA.toByte(), 0xA1.toByte(), 0xCA.toByte(),
        0xB9.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
        0xEA.toByte(), 0xFF.toByte(),
    )

    // -------------------------------------------------------------------------
    // RTR 310 captures — real inbound packets
    // -------------------------------------------------------------------------

    /**
     * Sample inbound packets. Primary source is the 2026-08-16 btsnoop HCI capture
     * (733 packets), which supersedes the 2026-08-08 shutdown burst (21 packets).
     * All are 20 bytes with verified checksums (C constants confirmed).
     *
     * Sources:
     *  - `captures/rtr-capture-20260816-btsnoop.txt` — 0x10, 0x11, 0x12, 0x42
     *  - `captures/rtr-capture-20260808-150945.txt`  — 0x5F, 0x7D
     */
    // 5A 10 EA EA F5 59 B0 F5 EA 64 E5 EA D8 CA EA EA EA EA B1 FF  (C=0x49)
    val SAMPLE_0x10: ByteArray = byteArrayOf(
        0x5A, 0x10, 0xEA.toByte(), 0xEA.toByte(), 0xF5.toByte(), 0x59,
        0xB0.toByte(), 0xF5.toByte(), 0xEA.toByte(), 0x64, 0xE5.toByte(), 0xEA.toByte(),
        0xD8.toByte(), 0xCA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
        0xB1.toByte(), 0xFF.toByte(),
    )

    // 5A 11 EA 2A EA EA E6 EE EA EA CA EA EA D8 E1 EA EA FA BD FF  (C=0xDD)
    val SAMPLE_0x11: ByteArray = byteArrayOf(
        0x5A, 0x11, 0xEA.toByte(), 0x2A, 0xEA.toByte(), 0xEA.toByte(),
        0xE6.toByte(), 0xEE.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xCA.toByte(), 0xEA.toByte(),
        0xEA.toByte(), 0xD8.toByte(), 0xE1.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xFA.toByte(),
        0xBD.toByte(), 0xFF.toByte(),
    )

    // 5A 12 EA EA 16 EA E6 EA EC 3A EC F1 F6 40 63 BA EB EA 1E FF  (C=0x59)
    val SAMPLE_0x12: ByteArray = byteArrayOf(
        0x5A, 0x12, 0xEA.toByte(), 0xEA.toByte(), 0x16, 0xEA.toByte(),
        0xE6.toByte(), 0xEA.toByte(), 0xEC.toByte(), 0x3A, 0xEC.toByte(), 0xF1.toByte(),
        0xF6.toByte(), 0x40, 0x63, 0xBA.toByte(), 0xEB.toByte(), 0xEA.toByte(),
        0x1E, 0xFF.toByte(),
    )

    // 5A 5F EA EA 96 98 0D 1E 7A C4 F5 A9 EB AB EA EA EA EA F1 FF  (C=0xF5 for fcnt=0x1E)
    val SAMPLE_0x5F: ByteArray = byteArrayOf(
        0x5A, 0x5F, 0xEA.toByte(), 0xEA.toByte(), 0x96.toByte(), 0x98.toByte(),
        0x0D, 0x1E, 0x7A, 0xC4.toByte(), 0xF5.toByte(), 0xA9.toByte(),
        0xEB.toByte(), 0xAB.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
        0xF1.toByte(), 0xFF.toByte(),
    )

    // 5A 7D A7 AE DC D9 D2 A9 B9 DB B2 BE DB AB D8 DD DC D8 DA FF  (C=0x29)
    val SAMPLE_0x7D: ByteArray = byteArrayOf(
        0x5A, 0x7D, 0xA7.toByte(), 0xAE.toByte(), 0xDC.toByte(), 0xD9.toByte(),
        0xD2.toByte(), 0xA9.toByte(), 0xB9.toByte(), 0xDB.toByte(), 0xB2.toByte(), 0xBE.toByte(),
        0xDB.toByte(), 0xAB.toByte(), 0xD8.toByte(), 0xDD.toByte(), 0xDC.toByte(), 0xD8.toByte(),
        0xDA.toByte(), 0xFF.toByte(),
    )

    // 5B 42 EA EA EA EA EA B2 3B B0 16 C8 C7 EA EA EA EA EA 7F FF  (C=0x82)
    val SAMPLE_0x42: ByteArray = byteArrayOf(
        0x5B, 0x42, 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
        0xEA.toByte(), 0xB2.toByte(), 0x3B, 0xB0.toByte(), 0x16, 0xC8.toByte(),
        0xC7.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
        0x7F, 0xFF.toByte(),
    )

    /**
     * All six sample packets as a list for batch testing.
     */
    val ALL_SAMPLES: List<ByteArray> = listOf(
        SAMPLE_0x10,
        SAMPLE_0x11,
        SAMPLE_0x12,
        SAMPLE_0x5F,
        SAMPLE_0x7D,
        SAMPLE_0x42,
    )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Converts a [ByteArray] to an uppercase hex string, e.g. `"5A 10 EA ..."`. */
    fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
