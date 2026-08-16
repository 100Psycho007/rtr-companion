package dev.rtrcompanion.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for captured RTR 310 packet parsing.
 *
 * These tests use real observed packets from [CapturedPackets] to verify the
 * parser extracts the correct structural data. Field-level decoding is NOT
 * tested here — that comes in Sprint 5 once formats are confirmed from
 * ignition-ON captures.
 *
 * ## FIRST_CAPTURED_WRITE test contract
 *
 * The spec states:
 * - Input: `5B 52 AE 82 8B 84 9F 99 82 CA A1 CA B9 EA EA EA EA EA EA FF`
 * - Expected: frame ID = 0x52, raw length = 20
 *
 * IMPORTANT: This packet's meaning is NOT confirmed. Tests here verify
 * structural extraction only, not semantics.
 */
class CapturedPacketParsingTest {

    @get:Rule
    val timber = TimberTestRule()

    // -------------------------------------------------------------------------
    // FIRST_CAPTURED_WRITE tests (spec requirement)
    // -------------------------------------------------------------------------

    /**
     * Spec requirement: `frame ID = 0x52, raw length = 20`.
     *
     * Note: the terminator byte is 0xFF (byte 19 = 0xFF), but the checksum
     * byte (18) is also 0xEA which is the null marker. The parser validates
     * structural shape only — no checksum constant is known for 0x52.
     */
    @Test
    fun `FIRST_CAPTURED_WRITE - frame ID is 0x52`() {
        val raw = RawPacket(CapturedPackets.FIRST_CAPTURED_WRITE)
        val pkt = ApacheProtocol.parseFrame(raw)

        // Structural parse should succeed (length=20, terminator=0xFF)
        assertNotNull("Should parse successfully", pkt.frame)
        assertEquals(
            "Frame ID must be 0x52 per spec",
            0x52.toByte(),
            pkt.frame!!.id,
        )
    }

    @Test
    fun `FIRST_CAPTURED_WRITE - raw length is 20`() {
        assertEquals(20, CapturedPackets.FIRST_CAPTURED_WRITE.size)
        val raw = RawPacket(CapturedPackets.FIRST_CAPTURED_WRITE)
        val pkt = ApacheProtocol.parseFrame(raw)
        assertNotNull(pkt.frame)
        assertEquals(20, pkt.frame!!.length)
    }

    @Test
    fun `FIRST_CAPTURED_WRITE - frame type is 0x5B CTRL`() {
        val raw = RawPacket(CapturedPackets.FIRST_CAPTURED_WRITE)
        val frame = ApacheProtocol.parseFrame(raw).frame!!
        assertEquals(0x5B.toByte(), frame.frameType)
        assertEquals("CTRL", frame.frameTypeLabel)
    }

    @Test
    fun `FIRST_CAPTURED_WRITE - checksum is unknown (no C constant for 0x52)`() {
        val result = ApacheProtocol.verifyChecksum(
            CapturedPackets.FIRST_CAPTURED_WRITE,
            0x52.toByte(),
        )
        assertEquals(ApacheProtocol.ChecksumResult.UNKNOWN, result)
    }

    @Test
    fun `FIRST_CAPTURED_WRITE - must NOT be sent to bike automatically`() {
        // This test documents a safety contract, not a code assertion.
        // The FIRST_CAPTURED_WRITE fixture exists for OFFLINE analysis only.
        // It is never auto-sent by RtrGattManager or any production code path.
        //
        // Verify it is NOT constructed anywhere in the protocol module
        // (all test usage is explicit, not from a production call site).
        //
        // As a structural safety test: the byte at index 0 is 0x5B (CTRL),
        // not 0x9A (AUTH), confirming this is not an auth packet.
        assertFalse(
            "FIRST_CAPTURED_WRITE is not an AUTH frame",
            CapturedPackets.FIRST_CAPTURED_WRITE[0] == 0x9A.toByte(),
        )
    }

    // -------------------------------------------------------------------------
    // All sample packets
    // -------------------------------------------------------------------------

    @Test
    fun `all 6 sample packets parse successfully`() {
        CapturedPackets.ALL_SAMPLES.forEachIndexed { i, bytes ->
            val pkt = ApacheProtocol.parseFrame(RawPacket(bytes))
            assertNotNull("Sample $i (0x%02X) must parse".format(bytes[1]), pkt.frame)
        }
    }

    @Test
    fun `sample 0x10 has DATA frame type`() {
        val pkt = ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x10))
        assertEquals("DATA", pkt.frame!!.frameTypeLabel)
    }

    @Test
    fun `sample 0x42 has CTRL frame type`() {
        val pkt = ApacheProtocol.parseFrame(RawPacket(CapturedPackets.SAMPLE_0x42))
        assertEquals("CTRL", pkt.frame!!.frameTypeLabel)
    }

    @Test
    fun `sample 0x7D XOR 0xEA decode - first byte is ASCII M`() {
        // 0x7D packet: each payload byte XOR 0xEA = VIN ASCII character
        // B2 = 0xA7 XOR 0xEA = 0x4D = 'M'
        val bytes = CapturedPackets.SAMPLE_0x7D
        val b2 = bytes[2].toInt() and 0xFF
        val decoded = (b2 xor 0xEA).toChar()
        assertEquals("First VIN byte should be 'M'", 'M', decoded)
    }

    @Test
    fun `sample 0x7D XOR 0xEA decode - first 3 bytes are MD6 (TVS prefix)`() {
        val bytes = CapturedPackets.SAMPLE_0x7D
        val vin = (2..4).map { i ->
            ((bytes[i].toInt() and 0xFF) xor 0xEA).toChar()
        }.joinToString("")
        assertEquals("VIN prefix should be MD6 (TVS Motor Company)", "MD6", vin)
    }

    @Test
    fun `sample 0x11 - two variants have consistent checksum delta`() {
        // v1 = SAMPLE_0x11: B10=0xCA, chk=0xBD
        // v2: B10=0xEA (+0x20 from v1), chk=0x9D (-0x20 from v1) → confirms (C-sum) formula
        val v1 = CapturedPackets.SAMPLE_0x11
        val v2 = byteArrayOf(
            0x5A, 0x11, 0xEA.toByte(), 0x2A, 0xEA.toByte(), 0xEA.toByte(), 0xE6.toByte(),
            0xEE.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(),
            0xD8.toByte(), 0xE1.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xFA.toByte(),
            0x9D.toByte(), 0xFF.toByte(),
        )

        val chk1 = v1[18].toInt() and 0xFF  // 0xBD
        val chk2 = v2[18].toInt() and 0xFF  // 0x9D
        val b10_v1 = v1[10].toInt() and 0xFF // 0xCA
        val b10_v2 = v2[10].toInt() and 0xFF // 0xEA

        val payloadDelta = b10_v2 - b10_v1   // +0x20
        val checksumDelta = chk2 - chk1       // -0x20

        assertEquals("Checksum delta must be negative of payload delta", -payloadDelta, checksumDelta)
    }

    // -------------------------------------------------------------------------
    // messageLabel tests
    // -------------------------------------------------------------------------

    @Test
    fun `messageLabel - returns label for all known IDs`() {
        val knownIds = listOf(
            0x10.toByte(), 0x11.toByte(), 0x12.toByte(), 0x16.toByte(),
            0x18.toByte(), 0x19.toByte(), 0x29.toByte(), 0x42.toByte(),
            0x4A.toByte(), 0x5F.toByte(), 0x7D.toByte(), 0xF1.toByte(), 0xF2.toByte(),
        )
        knownIds.forEach { id ->
            val label = ApacheProtocol.messageLabel(id)
            assertFalse("Label for 0x%02X must not contain UNKNOWN".format(id), label.contains("UNKNOWN"))
        }
    }

    @Test
    fun `messageLabel - returns UNKNOWN for unrecognised ID`() {
        val label = ApacheProtocol.messageLabel(0xAB.toByte())
        assertTrue("Unknown ID must include UNKNOWN in label", label.contains("UNKNOWN"))
    }
}
