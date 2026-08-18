package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the hex encoding logic in [RawPacket] and [ApacheFrame].
 *
 * These use real RTR 310 captured bytes to verify the formatter produces
 * the expected uppercase, space-separated output.
 */
class HexEncoderTest {

    @get:Rule
    val timber = TimberTestRule()

    @Test
    fun `RawPacket hex - all zeros`() {
        val packet = RawPacket(ByteArray(4) { 0x00 })
        assertEquals("00 00 00 00", packet.hex)
    }

    @Test
    fun `RawPacket hex - all 0xFF`() {
        val packet = RawPacket(ByteArray(4) { 0xFF.toByte() })
        assertEquals("FF FF FF FF", packet.hex)
    }

    @Test
    fun `RawPacket hex - single byte`() {
        val packet = RawPacket(byteArrayOf(0xAB.toByte()))
        assertEquals("AB", packet.hex)
    }

    @Test
    fun `RawPacket hex - real captured 0x10 frame`() {
        val packet = RawPacket(CapturedPackets.SAMPLE_0x10)
        // Verify length and correct hex rendering for the known first 4 bytes
        assertEquals(20, packet.bytes.size)
        assertEquals("5A", packet.hex.substring(0, 2))
        assertEquals("5A 10", packet.hex.substring(0, 5))
    }

    @Test
    fun `RawPacket hex - empty packet`() {
        val packet = RawPacket(ByteArray(0))
        assertEquals("", packet.hex)
    }

    @Test
    fun `ApacheFrame hex - matches raw bytes`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x11)
        val apachePkt = ApacheProtocol.parseFrame(raw)
        val frame = apachePkt.frame!!
        assertEquals(raw.hex, frame.hex)
    }

    @Test
    fun `RawPacket hex uses uppercase`() {
        val packet = RawPacket(byteArrayOf(0xab.toByte(), 0xcd.toByte()))
        assertEquals("AB CD", packet.hex)
    }

    @Test
    fun `CapturedPackets toHex extension formats correctly`() {
        val hex = with(CapturedPackets) { CapturedPackets.SAMPLE_0x10.toHex() }
        assertEquals("5A 10", hex.substring(0, 5))
    }
}
