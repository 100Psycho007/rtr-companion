package dev.rtrcompanion.blecore

/**
 * Controls what the app writes to the RTR 310 BLE device.
 *
 * ## PASSIVE (default)
 *
 * The app only listens. After connecting, it:
 *  1. Discovers services
 *  2. Enables CCCD notifications on [BleConstants.CHAR_NOTIFY]
 *  3. Captures every incoming packet via [BleConstants.CHAR_NOTIFY]
 *
 * No writes are made to [BleConstants.CHAR_WRITE] (0x5352).
 *
 * Use this for pure capture sessions when you don't want the cluster display
 * to change at all.
 *
 * ## ACTIVE (recommended for live telemetry)
 *
 * Confirmed safe from btsnoop HCI capture analysis (2026-08-16).
 *
 * After CCCD enable, sends:
 *  1. [BleConstants.MSG_USER_ID] (`0x52`) — user display name
 *  2. [BleConstants.MSG_VEHICLE_NAME] (`0x43`) — vehicle display name
 *  3. [BleConstants.MSG_PING] (`0x4A`) keep-alive loop every [BleConstants.PING_INTERVAL_MS]
 *
 * These are **display-only** writes. They set text on the cluster screen and
 * maintain the connection. They have no effect on engine, safety, or hardware state.
 * Disconnecting instantly reverts the cluster to standalone mode.
 *
 * This is the mode that should trigger live telemetry streaming (speed, RPM, etc.)
 * from the bike. The observed TVS Connect session used exactly this sequence.
 *
 * ## EXPERIMENTAL
 *
 * Adds the unverified AES-128-CTR authentication handshake (Jupiter RE, unconfirmed
 * on RTR 310) on top of ACTIVE mode.
 *
 * Do NOT use unless you are specifically researching whether the RTR 310 sends
 * a `0x9A 0xF2` challenge. Based on the 2026-08-16 capture no such challenge was
 * observed — ACTIVE mode appears sufficient.
 *
 * See `docs/security/BLE_WRITE_AUDIT.md`.
 */
enum class ProtocolMode {

    /**
     * Passive listening only. No writes to CHAR_WRITE.
     * Use for pure capture/research sessions.
     */
    PASSIVE,

    /**
     * Active mode — safe registration + keep-alive writes.
     *
     * Sends: user name (0x52) → vehicle name (0x43) → ping loop (0x4A).
     * Confirmed from real TVS Connect btsnoop session (2026-08-16).
     * This is the mode that should unlock live telemetry streaming.
     */
    ACTIVE,

    /**
     * Experimental — adds AES handshake on top of ACTIVE.
     * Only use if investigating `0x9A 0xF2` challenge packets.
     * Based on current evidence this is NOT needed for RTR 310.
     */
    EXPERIMENTAL,
}
