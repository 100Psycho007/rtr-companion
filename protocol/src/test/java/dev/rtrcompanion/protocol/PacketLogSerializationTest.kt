package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for packet log serialization via [PacketLogger.export] and
 * [ApachePacketLogger.export].
 *
 * Verifies the text export format used by [PacketExporter] to write capture files.
 */
class PacketLogSerializationTest {

    @get:Rule
    val timber = TimberTestRule()

    // -------------------------------------------------------------------------
    // PacketLogger (raw bytes)
    // -------------------------------------------------------------------------

    @Test
    fun `PacketLogger export - empty log returns empty string`() {
        val logger = PacketLogger()
        assertEquals("", logger.export())
    }

    @Test
    fun `PacketLogger export - single packet contains timestamp and hex`() {
        val logger = PacketLogger()
        val ts = 1723000000000L
        val packet = RawPacket(byteArrayOf(0x5A, 0x10), ts)
        logger.record(packet)

        val exported = logger.export()
        assertTrue("Export must contain timestamp", exported.contains("$ts"))
        assertTrue("Export must contain hex", exported.contains("5A 10"))
    }

    @Test
    fun `PacketLogger export - multiple packets are newline separated`() {
        val logger = PacketLogger()
        logger.record(RawPacket(byteArrayOf(0x5A, 0x10), 1000L))
        logger.record(RawPacket(byteArrayOf(0x5A, 0x11), 2000L))

        val lines = logger.export().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("5A 10"))
        assertTrue(lines[1].contains("5A 11"))
    }

    @Test
    fun `PacketLogger record and size - ring buffer drops oldest at maxEntries`() {
        val maxEntries = 5
        val logger = PacketLogger(maxEntries)
        repeat(7) { i ->
            logger.record(RawPacket(byteArrayOf(i.toByte()), i.toLong()))
        }
        // Should have exactly maxEntries, oldest 2 dropped
        assertEquals(maxEntries, logger.log.value.size)
        // Newest entry should be packet with byte value 6
        assertEquals(6.toByte(), logger.log.value.last().bytes[0])
    }

    @Test
    fun `PacketLogger clear - empties the log`() {
        val logger = PacketLogger()
        logger.record(RawPacket(CapturedPackets.SAMPLE_0x10))
        logger.record(RawPacket(CapturedPackets.SAMPLE_0x11))
        assertEquals(2, logger.log.value.size)

        logger.clear()
        assertEquals(0, logger.log.value.size)
        assertEquals("", logger.export())
    }

    // -------------------------------------------------------------------------
    // ApachePacketLogger
    // -------------------------------------------------------------------------

    @Test
    fun `ApachePacketLogger export - empty log returns empty string`() {
        val logger = ApachePacketLogger()
        assertEquals("", logger.export())
    }

    @Test
    fun `ApachePacketLogger export - parsed packet contains direction and label`() {
        val logger = ApachePacketLogger()
        val raw = RawPacket(CapturedPackets.SAMPLE_0x10, 1723000000000L)
        val pkt = ApacheProtocol.parseFrame(raw)
        logger.record(pkt)

        val exported = logger.export()
        assertTrue("Export must contain RX direction", exported.contains("RX"))
        assertTrue("Export must contain message label", exported.contains("0x10"))
    }

    @Test
    fun `ApachePacketLogger export - unparsed packet is labelled UNPARSED`() {
        val logger = ApachePacketLogger()
        val raw = RawPacket(ByteArray(5) { 0xAB.toByte() }, 999L) // wrong length
        val pkt = ApachePacket(raw, frame = null)
        logger.record(pkt)

        val exported = logger.export()
        assertTrue("Unparsed packet must be labelled UNPARSED", exported.contains("UNPARSED"))
    }

    @Test
    fun `ApachePacketLogger messageTypeCounts - groups correctly`() {
        val logger = ApachePacketLogger()
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x10)))
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x10)))
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x11)))

        val counts = logger.messageTypeCounts()
        assertEquals(2, counts[0x10.toByte()])
        assertEquals(1, counts[0x11.toByte()])
    }

    @Test
    fun `ApachePacketLogger clear - empties log`() {
        val logger = ApachePacketLogger()
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x42)))
        assertEquals(1, logger.log.value.size)

        logger.clear()
        assertEquals(0, logger.log.value.size)
        assertEquals("", logger.export())
    }

    @Test
    fun `ApachePacketLogger knownTypeCount - counts only parsed frames`() {
        val logger = ApachePacketLogger()
        logger.record(ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x10)))
        logger.record(ApachePacket(RawPacket(ByteArray(5)), frame = null))  // unparsed

        assertEquals(1, logger.knownTypeCount())
    }

    // -------------------------------------------------------------------------
    // RawPacket equality and hashCode (required for StateFlow dedup)
    // -------------------------------------------------------------------------

    @Test
    fun `RawPacket equality - same bytes and timestamp are equal`() {
        val ts = 1234567890L
        val a = RawPacket(byteArrayOf(0x5A, 0x10), ts)
        val b = RawPacket(byteArrayOf(0x5A, 0x10), ts)
        assertEquals(a, b)
    }

    @Test
    fun `RawPacket equality - different bytes are not equal`() {
        val ts = 1234567890L
        val a = RawPacket(byteArrayOf(0x5A, 0x10), ts)
        val b = RawPacket(byteArrayOf(0x5A, 0x11), ts)
        assertFalse(a == b)
    }
}
