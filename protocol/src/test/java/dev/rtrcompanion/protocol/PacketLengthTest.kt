package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for packet length handling in [ApacheProtocol.parseFrame] and [ApachePacket].
 *
 * The RTR 310 protocol mandates exactly 20 bytes per packet. These tests verify
 * the parser correctly handles short, long, and correctly-sized packets.
 */
class PacketLengthTest {

    @get:Rule
    val timber = TimberTestRule()

    @Test
    fun `parseFrame - valid 20-byte packet returns non-null frame`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10)
        val pkt = ApacheProtocol.parseFrame(raw)
        assertTrue(pkt.isParsed)
        assertTrue(pkt.isValidLength)
    }

    @Test
    fun `parseFrame - short packet returns null frame`() {
        val raw = RawPacket(ByteArray(19) { 0xEA.toByte() })
        val pkt = ApacheProtocol.parseFrame(raw)
        assertNull(pkt.frame)
        assertFalse(pkt.isParsed)
        assertFalse(pkt.isValidLength)
    }

    @Test
    fun `parseFrame - long packet returns null frame`() {
        val raw = RawPacket(ByteArray(21) { 0xEA.toByte() })
        val pkt = ApacheProtocol.parseFrame(raw)
        assertNull(pkt.frame)
        assertFalse(pkt.isValidLength)
    }

    @Test
    fun `parseFrame - empty packet returns null frame`() {
        val raw = RawPacket(ByteArray(0))
        val pkt = ApacheProtocol.parseFrame(raw)
        assertNull(pkt.frame)
    }

    @Test
    fun `parseFrame - 1-byte packet returns null frame`() {
        val raw = RawPacket(byteArrayOf(0x5A))
        val pkt = ApacheProtocol.parseFrame(raw)
        assertNull(pkt.frame)
    }

    @Test
    fun `parseFrame - packet without 0xFF terminator returns null frame`() {
        // Valid structure but last byte is not 0xFF
        val bytes = CapturedPackets.SAMPLE_0x10.copyOf()
        bytes[19] = 0x00
        val pkt = ApacheProtocol.parseFrame(RawPacket(bytes))
        assertNull("Non-0xFF terminator must not parse", pkt.frame)
    }

    @Test
    fun `ApachePacket isValidLength - true for 20 bytes`() {
        val raw = RawPacket(ByteArray(20))
        val pkt = ApachePacket(raw, frame = null)
        assertTrue(pkt.isValidLength)
    }

    @Test
    fun `ApachePacket isValidLength - false for 19 bytes`() {
        val raw = RawPacket(ByteArray(19))
        val pkt = ApachePacket(raw, frame = null)
        assertFalse(pkt.isValidLength)
    }

    @Test
    fun `all sample captured packets are 20 bytes`() {
        CapturedPackets.ALL_SAMPLES.forEachIndexed { i, bytes ->
            assertEquals("Sample $i must be 20 bytes", 20, bytes.size)
        }
    }

    @Test
    fun `FIRST_CAPTURED_WRITE is 20 bytes`() {
        assertEquals(20, CapturedPackets.FIRST_CAPTURED_WRITE.size)
    }

    @Test
    fun `ApachePacket - timestamp delegates from RawPacket`() {
        val ts = 1723000000000L
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10, ts)
        val pkt = ApacheProtocol.parseFrame(raw)
        assertEquals(ts, pkt.timestamp)
    }
}
