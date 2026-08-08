package dev.rtrcompanion.blecore.auth

import dev.rtrcompanion.blecore.BleConstants
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages the TVS SmartXonnect BLE authentication handshake.
 *
 * ## Protocol (from Jupiter RE cross-reference — UNVERIFIED on RTR 310)
 *
 * After CCCD notification enable on CHAR_NOTIFY, the bike MAY send a challenge:
 * ```
 * Bike → Phone (NOTIFY):  0x9A 0xF2 [16 random bytes] [checksum] 0xFF
 * Phone → Bike (WRITE):   0x9A 0xF1 [AES-128-CTR encrypted response] [checksum] 0xFF
 * ```
 *
 * **This entire handshake sequence has NOT been observed on the RTR 310.**
 * It is documented in the Jupiter RE project which shares the same service UUID.
 * See `docs/research/JUPITER_CROSS_REFERENCE.md`.
 *
 * ## AES Key — UNVERIFIED on RTR 310
 *
 * The Jupiter AES-128-CTR key is:
 * `7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4`
 *
 * This key MUST NOT be sent to the RTR 310 until confirmed via btsnoop HCI log
 * from a TVS Connect session connecting to the RTR 310 with ignition ON.
 *
 * ## Safety — DISABLED IN PASSIVE MODE
 *
 * This class is ONLY invoked when [dev.rtrcompanion.blecore.connection.RtrGattManager]
 * is in [dev.rtrcompanion.blecore.ProtocolMode.EXPERIMENTAL] mode.
 * The default mode is PASSIVE — no writes to CHAR_WRITE occur.
 *
 * See `docs/security/BLE_WRITE_AUDIT.md` and `docs/protocol/PROTOCOL_STATUS.md`.
 */
object HandshakeManager {

    /**
     * AES-128 key used by TVS Jupiter SmartXonnect protocol.
     *
     * **STATUS: HYPOTHESIS — unverified on RTR 310.**
     * Source: JupiterRideCompanion RE report (github.com/overclock98/JupiterRideCompanion).
     * Must be confirmed via btsnoop log from TVS Connect connecting to RTR 310.
     */
    private val JUPITER_AES_KEY = byteArrayOf(
        0x7A.toByte(), 0xA3.toByte(), 0x20.toByte(), 0x4D.toByte(),
        0x16.toByte(), 0x1D.toByte(), 0xB5.toByte(), 0x33.toByte(),
        0xF4.toByte(), 0xEB.toByte(), 0x20.toByte(), 0x4F.toByte(),
        0xBC.toByte(), 0xD7.toByte(), 0x3D.toByte(), 0xD4.toByte(),
    )

    /**
     * Returns true if this notification packet is an authentication challenge
     * from the bike.
     *
     * A challenge packet starts with [BleConstants.FRAME_AUTH] (`0x9A`) followed
     * by [BleConstants.MSG_AUTH_CHALLENGE] (`0xF2`).
     *
     * @param packet Raw notification bytes received from CHAR_NOTIFY.
     */
    fun isChallenge(packet: ByteArray): Boolean {
        return packet.size >= 2
            && packet[0] == BleConstants.FRAME_AUTH
            && packet[1] == BleConstants.MSG_AUTH_CHALLENGE
    }

    /**
     * Builds the authentication response packet to send to CHAR_WRITE.
     *
     * Extracts the 16-byte challenge from the packet, encrypts it using
     * AES-128-CTR with the TVS key, and wraps it in the response frame.
     *
     * @param challengePacket The raw challenge packet received from the bike.
     * @return The response packet to write to CHAR_WRITE, or null if the
     *         challenge packet is malformed or encryption fails.
     */
    fun buildResponse(challengePacket: ByteArray): ByteArray? {
        // Challenge packet: [0x9A][0xF2][16 challenge bytes][checksum][0xFF]
        // Minimum valid length: 2 (header) + 16 (challenge) + 1 (checksum) + 1 (end) = 20
        if (challengePacket.size < 20) {
            Timber.e("HandshakeManager: challenge packet too short (%d bytes)", challengePacket.size)
            return null
        }

        val challengeBytes = challengePacket.copyOfRange(2, 18)
        Timber.d("HandshakeManager: challenge = %s", challengeBytes.toHex())

        val encrypted = encryptAes128Ctr(challengeBytes, JUPITER_AES_KEY) ?: return null
        Timber.d("HandshakeManager: encrypted = %s", encrypted.toHex())

        return buildResponsePacket(encrypted)
    }

    /**
     * Assembles the full 20-byte response packet:
     * `[0x9A][0xF1][16 encrypted bytes][checksum][0xFF]`
     */
    private fun buildResponsePacket(encryptedPayload: ByteArray): ByteArray {
        val packet = ByteArray(20)
        packet[0] = BleConstants.FRAME_AUTH          // 0x9A
        packet[1] = BleConstants.MSG_AUTH_RESPONSE   // 0xF1

        // Copy encrypted payload into bytes 2–17
        val payloadLen = minOf(encryptedPayload.size, 16)
        encryptedPayload.copyInto(packet, destinationOffset = 2, endIndex = payloadLen)

        // Checksum: 255 - (sum(bytes[0..17]) % 256)
        packet[18] = computeChecksum(packet, 0, 18)
        packet[19] = BleConstants.PACKET_END         // 0xFF

        return packet
    }

    /**
     * Encrypts [data] using AES-128-CTR with a zero IV.
     *
     * AES-CTR with a zero IV and the shared key is the algorithm documented
     * in the Jupiter RE report. The IV is all zeros (no padding mode).
     *
     * @return Encrypted bytes, or null on failure.
     */
    private fun encryptAes128Ctr(data: ByteArray, key: ByteArray): ByteArray? {
        return try {
            val keySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(ByteArray(16)) // zero IV
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            Timber.e(e, "HandshakeManager: AES encryption failed")
            null
        }
    }

    /**
     * Computes the TVS SmartXonnect checksum for a packet.
     *
     * Formula: `255 - (sum(bytes[fromIndex until toIndex]) % 256)`
     *
     * **Note:** This formula is used by Jupiter RE. RTR 310 inbound checksums use
     * a different constant C per message type: `(C - sum(B0..B17)) mod 256`.
     * The constants differ from 0xFF (Jupiter). This outbound formula has NOT been
     * verified on RTR 310. Used here for building EXPERIMENTAL outbound packets only.
     *
     * @param packet The packet bytes.
     * @param fromIndex Start index (inclusive).
     * @param toIndex End index (exclusive).
     */
    fun computeChecksum(packet: ByteArray, fromIndex: Int, toIndex: Int): Byte {
        var sum = 0
        for (i in fromIndex until toIndex) {
            sum += packet[i].toInt() and 0xFF
        }
        return (255 - (sum % 256)).toByte()
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}
