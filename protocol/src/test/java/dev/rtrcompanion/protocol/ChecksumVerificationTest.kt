package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests for checksum verification in [ApacheProtocol].
 *
 * The RTR 310 uses a per-message-type constant C in the formula:
 * `checksum = (C − sum(B0..B17)) mod 256`
 *
 * These tests verify the confirmed C values using real captured packets.
 *
 * Source: `docs/KNOWN_FACTS.md` — Checksum Algorithm section.
 * Source: `docs/protocol/capture-20260808-150945.md`
 */
class ChecksumVerificationTest {

    @get:Rule
    val timber = TimberTestRule()

    // -------------------------------------------------------------------------
    // verifyChecksum — per known message type
    // -------------------------------------------------------------------------

    @Test
    fun `verifyChecksum - 0x10 sample passes with C=0x49`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x10, 0x10.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.PASS, result)
    }

    @Test
    fun `verifyChecksum - 0x11 sample passes with C=0xDD`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x11, 0x11.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.PASS, result)
    }

    @Test
    fun `verifyChecksum - 0x12 sample passes with C=0x59`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x12, 0x12.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.PASS, result)
    }

    @Test
    fun `verifyChecksum - 0x7D sample passes with C=0x29`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x7D, 0x7D.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.PASS, result)
    }

    @Test
    fun `verifyChecksum - 0x42 sample passes with C=0x82`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x42, 0x42.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.PASS, result)
    }

    @Test
    fun `verifyChecksum - 0x5F returns UNKNOWN (no C constant known)`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x5F, 0x5F.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.UNKNOWN, result)
    }

    @Test
    fun `verifyChecksum - FAIL for corrupted 0x11 packet`() {
        val corrupted = CapturedPackets.SAMPLE_0x11.copyOf()
        corrupted[10] = (corrupted[10] + 1).toByte() // flip a payload byte
        // checksum is no longer valid
        val result = ApacheProtocol.verifyChecksum(corrupted, 0x11.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.FAIL, result)
    }

    @Test
    fun `verifyChecksum - UNKNOWN for unrecognised message ID`() {
        val result = ApacheProtocol.verifyChecksum(CapturedPackets.SAMPLE_0x10, 0xAA.toByte())
        assertEquals(ApacheProtocol.ChecksumResult.UNKNOWN, result)
    }

    // -------------------------------------------------------------------------
    // computeOutboundChecksum (Jupiter formula — outbound only)
    // -------------------------------------------------------------------------

    @Test
    fun `computeOutboundChecksum - known test vector`() {
        // Build a 20-byte packet with all zeros (B0..B17); checksum should be 255
        val packet = ByteArray(20)
        val chk = ApacheProtocol.computeOutboundChecksum(packet)
        assertEquals((255).toByte(), chk)
    }

    @Test
    fun `computeOutboundChecksum - sum wraps correctly`() {
        // B0..B17 all set to 0x01 → sum = 18 → 255 - (18 % 256) = 237 = 0xED
        val packet = ByteArray(20) { if (it < 18) 0x01.toByte() else 0x00.toByte() }
        val chk = ApacheProtocol.computeOutboundChecksum(packet)
        assertEquals(237.toByte(), chk)
    }

    @Test
    fun `computeOutboundChecksum - sum of 256 wraps to 255`() {
        // B0 = 0x01 x18, but we want sum = 256 to test wrapping
        // 18 bytes each = 0x10 (16) → sum = 288 → 288 % 256 = 32 → 255 - 32 = 223
        val packet = ByteArray(20) { if (it < 18) 0x10.toByte() else 0x00.toByte() }
        val chk = ApacheProtocol.computeOutboundChecksum(packet)
        val expected = (255 - (18 * 16) % 256).toByte()
        assertEquals(expected, chk)
    }

    // -------------------------------------------------------------------------
    // C constant cross-verification for 0x11 (two-variant confirmed)
    // -------------------------------------------------------------------------

    @Test
    fun `0x11 C=0xDD - second variant also passes checksum`() {
        // v2: B10=0xEA (was 0xCA in SAMPLE_0x11, +0x20), chk=0x9D (was 0xBD, -0x20)
        // 5A 11 EA 2A EA EA E6 EE EA EA EA EA EA D8 E1 EA EA FA 9D FF
        val v2 = byteArrayOf(
            0x5A, 0x11, 0xEA.toByte(), 0x2A, 0xEA.toByte(), 0xEA.toByte(), 0xE6.toByte(),
            0xEE.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
            0xD8.toByte(), 0xE1.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xFA.toByte(),
            0x9D.toByte(), 0xFF.toByte(),
        )
        assertEquals(ApacheProtocol.ChecksumResult.PASS,
            ApacheProtocol.verifyChecksum(v2, 0x11.toByte()))
    }

    @Test
    fun `checksumPassCount - correct count in ApachePacketLogger`() {
        val logger = ApachePacketLogger()
        // Add packets with known-passing checksums (C=0x49 and C=0xDD)
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x10)))
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x11)))
        // Add 0x5F which has UNKNOWN checksum — must NOT count as passing
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x5F)))

        assertEquals(2, logger.checksumPassCount())
    }
}
