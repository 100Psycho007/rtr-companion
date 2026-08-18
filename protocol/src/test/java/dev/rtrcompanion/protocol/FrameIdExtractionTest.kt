package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [ApacheProtocol.parseFrame] frame-ID extraction and [ApacheFrame] fields.
 *
 * Uses real RTR 310 captured packets from [CapturedPackets] to verify the parser
 * extracts the correct frame type, message ID, and structural fields.
 */
class FrameIdExtractionTest {

    @get:Rule
    val timber = TimberTestRule()

    @Test
    fun `parseFrame - 0x10 frame type and id`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull("Frame should be parsed", pkt.frame)
        assertEquals(0x5A.toByte(), pkt.frame!!.frameType)
        assertEquals(0x10.toByte(), pkt.frame!!.id)
        assertEquals(ApacheFrame.Direction.RX, pkt.frame!!.direction)
    }

    @Test
    fun `parseFrame - 0x11 frame type and id`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x11)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull(pkt.frame)
        assertEquals(0x5A.toByte(), pkt.frame!!.frameType)
        assertEquals(0x11.toByte(), pkt.frame!!.id)
    }

    @Test
    fun `parseFrame - 0x12 frame type and id`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x12)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull(pkt.frame)
        assertEquals(0x5A.toByte(), pkt.frame!!.frameType)
        assertEquals(0x12.toByte(), pkt.frame!!.id)
    }

    @Test
    fun `parseFrame - 0x5F data frame`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x5F)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull(pkt.frame)
        assertEquals(0x5A.toByte(), pkt.frame!!.frameType)
        assertEquals(0x5F.toByte(), pkt.frame!!.id)
    }

    @Test
    fun `parseFrame - 0x7D frame`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x7D)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull(pkt.frame)
        assertEquals(0x7D.toByte(), pkt.frame!!.id)
    }

    @Test
    fun `parseFrame - 0x42 control frame uses 0x5B prefix`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x42)
        val pkt = ApacheProtocol.parseFrame(raw)

        assertNotNull(pkt.frame)
        assertEquals(0x5B.toByte(), pkt.frame!!.frameType)
        assertEquals(0x42.toByte(), pkt.frame!!.id)
        assertEquals("CTRL", pkt.frame!!.frameTypeLabel)
    }

    @Test
    fun `parseFrame - frameTypeLabel for 0x5A is DATA`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10)
        val pkt = ApacheProtocol.parseFrame(raw)
        assertEquals("DATA", pkt.frame!!.frameTypeLabel)
    }

    @Test
    fun `extractMessageId - returns correct byte`() {
        assertEquals(0x10.toByte(), ApacheProtocol.extractMessageId(CapturedPackets.SAMPLE_0x10))
        assertEquals(0x11.toByte(), ApacheProtocol.extractMessageId(CapturedPackets.SAMPLE_0x11))
        assertEquals(0x5F.toByte(), ApacheProtocol.extractMessageId(CapturedPackets.SAMPLE_0x5F))
    }

    @Test
    fun `extractMessageId - returns null for empty array`() {
        assertNull(ApacheProtocol.extractMessageId(ByteArray(0)))
    }

    @Test
    fun `extractMessageId - returns null for single byte array`() {
        assertNull(ApacheProtocol.extractMessageId(byteArrayOf(0x5A)))
    }

    @Test
    fun `ApacheFrame payload is bytes 2 through 17`() {
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10)
        val frame = ApacheProtocol.parseFrame(raw).frame!!
        assertEquals(16, frame.payload.size)
        // Verify payload starts at index 2 of raw bytes
        assertTrue(frame.raw[2] == frame.payload[0])
        assertTrue(frame.raw[17] == frame.payload[15])
    }

    @Test
    fun `ApacheFrame terminator is always 0xFF`() {
        CapturedPackets.ALL_SAMPLES.forEach { bytes ->
            val frame = ApacheProtocol.parseFrame(RawPacket(bytes)).frame!!
            assertEquals(0xFF.toByte(), frame.terminator)
        }
    }

    @Test
    fun `ApacheFrame length is 20 for all samples`() {
        CapturedPackets.ALL_SAMPLES.forEach { bytes ->
            val frame = ApacheProtocol.parseFrame(RawPacket(bytes)).frame!!
            assertEquals(20, frame.length)
        }
    }
}
