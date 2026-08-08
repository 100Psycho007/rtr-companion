package dev.rtrcompanion.blecore

/**
 * Controls whether the app sends any data to the RTR 310 BLE device.
 *
 * ## PASSIVE (DEFAULT)
 *
 * The app only listens. After connecting, it:
 *  1. Discovers services
 *  2. Enables CCCD notifications on [BleConstants.CHAR_NOTIFY]
 *  3. Captures every incoming packet via [BleConstants.CHAR_NOTIFY]
 *
 * No writes are made to [BleConstants.CHAR_WRITE] (0x5352).
 * [dev.rtrcompanion.blecore.auth.HandshakeManager] and
 * [dev.rtrcompanion.blecore.ping.PingPacketBuilder] exist in the codebase
 * but are NOT invoked in this mode.
 *
 * Use this mode until:
 *  - The authentication challenge/response sequence is confirmed on RTR 310
 *    via btsnoop HCI log from a TVS Connect session
 *  - The Jupiter AES key is verified (or replaced with the correct RTR 310 key)
 *  - The ping packet format is confirmed on RTR 310
 *
 * ## EXPERIMENTAL
 *
 * Enables writes to [BleConstants.CHAR_WRITE]:
 *  - Handshake response [dev.rtrcompanion.blecore.auth.HandshakeManager]
 *    when a `0x9A 0xF2` challenge is received
 *  - Keep-alive ping [dev.rtrcompanion.blecore.ping.PingPacketBuilder]
 *    every [BleConstants.PING_INTERVAL_MS]
 *
 * **WARNING:** Do not enable EXPERIMENTAL mode until the Jupiter AES key has been
 * verified on the RTR 310 via btsnoop log analysis. Sending an unverified
 * handshake response may cause the bike to reject the connection.
 *
 * See `docs/security/BLE_WRITE_AUDIT.md` and `docs/protocol/PROTOCOL_STATUS.md`.
 */
enum class ProtocolMode {

    /**
     * Passive listening only. No writes to CHAR_WRITE.
     * This is the required default until protocol writes are verified.
     */
    PASSIVE,

    /**
     * Experimental — enables handshake + ping writes.
     * ONLY use after verifying the AES key and ping format on RTR 310 hardware.
     */
    EXPERIMENTAL,
}
