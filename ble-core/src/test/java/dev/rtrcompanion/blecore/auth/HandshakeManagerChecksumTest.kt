package dev.rtrcompanion.blecore.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [HandshakeManager.computeChecksum].
 *
 * The checksum formula used for outbound packets (Phone → Bike) is the
 * Jupiter formula: `255 − (sum(B0..B17) mod 256)`.
 *
 * These tests verify the arithmetic is correct before it is used to build
 * outbound packets in EXPERIMENTAL mode.
 *
 * Note: RTR 310 *inbound* checksum validation uses per-message-type constants C,
 * tested in `ChecksumVerificationTest` in the `protocol` module.
 */
class HandshakeManagerChecksumTest {

    @Test
    fun `computeChecksum - all-zero bytes returns 255`() {
        val packet = ByteArray(20) { 0x00 }
        val checksum = HandshakeManager.computeChecksum(packet, 0, 18)
        assertEquals(255.toByte(), checksum)
    }

    @Test
    fun `computeChecksum - single 0x01 returns 254`() {
        val packet = ByteArray(20) { 0x00 }
        packet[0] = 0x01
        val checksum = HandshakeManager.computeChecksum(packet, 0, 18)
        assertEquals(254.toByte(), checksum)
    }

    @Test
    fun `computeChecksum - sum equal to 255 returns 0`() {
        // sum of B0..B17 = 255 → 255 - (255 % 256) = 0
        val packet = ByteArray(20) { 0x00 }
        packet[0] = 0xFF.toByte() // sum = 255
        val checksum = HandshakeManager.computeChecksum(packet, 0, 18)
        assertEquals(0.toByte(), checksum)
    }

    @Test
    fun `computeChecksum - sum equal to 256 wraps correctly`() {
        // sum of B0..B17 = 256 → 256 % 256 = 0 → 255 - 0 = 255
        val packet = ByteArray(20) { 0x00 }
        packet[0] = 0xFF.toByte()
        packet[1] = 0x01.toByte() // sum = 256
        val checksum = HandshakeManager.computeChecksum(packet, 0, 18)
        assertEquals(255.toByte(), checksum)
    }

    @Test
    fun `computeChecksum - real AUTH response packet structure`() {
        // Build a known AUTH response frame: 0x9A 0xF1 + 16 zeros + checksum + 0xFF
        // sum(B0..B17) = 0x9A + 0xF1 = 0x18B = 395
        // 395 % 256 = 139
        // 255 - 139 = 116 = 0x74
        val packet = ByteArray(20) { 0x00 }
        packet[0] = 0x9A.toByte()
        packet[1] = 0xF1.toByte()
        // B2..B17 = 0x00
        val checksum = HandshakeManager.computeChecksum(packet, 0, 18)
        val expected = (255 - ((0x9A + 0xF1) % 256)).toByte()
        assertEquals(expected, checksum)
    }

    @Test
    fun `computeChecksum - fromIndex and toIndex respected`() {
        // Only sum bytes 2..4 (3 bytes, each 0x10 = 16 → sum = 48 → 255 - 48 = 207)
        val packet = ByteArray(20) { 0x10.toByte() }
        val checksum = HandshakeManager.computeChecksum(packet, 2, 5)
        assertEquals((255 - (3 * 16)).toByte(), checksum)
    }

    @Test
    fun `computeChecksum - empty range returns 255`() {
        val packet = ByteArray(20) { 0xFF.toByte() }
        val checksum = HandshakeManager.computeChecksum(packet, 5, 5)
        assertEquals(255.toByte(), checksum)
    }

    @Test
    fun `computeChecksum - is symmetric over 256 boundary`() {
        // Two packets where one has sum +256 more than the other should produce the same checksum
        val p1 = ByteArray(20) { 0x00 }
        p1[0] = 0x01

        val p2 = ByteArray(20) { 0x00 }
        p2[0] = 0x01.toByte()
        p2[1] = 0xFF.toByte()
        p2[2] = 0x01.toByte() // net delta = +256 from p1

        val chk1 = HandshakeManager.computeChecksum(p1, 0, 3)  // sum = 1
        val chk2 = HandshakeManager.computeChecksum(p2, 0, 3)  // sum = 1 + 255 + 1 = 257 → mod256=1

        assertEquals(
            "sum mod 256 identical → checksum must match",
            chk1, chk2
        )
    }
}
