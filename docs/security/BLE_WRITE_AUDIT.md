# BLE Write Audit

> **Purpose:** Track every write operation to every BLE characteristic.
> This document must be updated before any new write operation is added.
> See `docs/SECURITY.md` for the safety policy.

---

## Audit Scope

All `.kt` source files searched for:
- `writeCharacteristic(`
- `characteristic.setValue(`
- `BluetoothGattCharacteristic.setValue(`
- `writeDescriptor(`
- Any helper that eventually performs a write

Last audited: 2026-08-08 (updated by protocol-integrity correction pass)

---

## Default Protocol Mode: PASSIVE

As of 2026-08-08, the app defaults to `ProtocolMode.PASSIVE`.
In PASSIVE mode:
- **No writes to CHAR_WRITE (0x5352).** HandshakeManager and PingPacketBuilder
  are isolated and not invoked.
- CCCD descriptor write on CHAR_NOTIFY remains enabled — this is required for
  passive packet capture.

A warning is logged at startup: `"Experimental protocol writes disabled (PASSIVE mode)."`

---

## Findings

### File: `ble-core/.../connection/RtrGattManager.kt`

| # | Function | Pattern | Characteristic | Purpose | Classification | Action |
|---|----------|---------|----------------|---------|----------------|--------|
| 1 | `enableNotifications()` | `gatt.writeDescriptor(descriptor, ENABLE_NOTIFICATION_VALUE)` (API ≥ 33) | CCCD on CHAR_NOTIFY (0x2902) | Enables BLE notifications on CHAR_NOTIFY. Passive listening only. | **SAFE — CCCD only** | **KEEP — enabled in all modes** |
| 2 | `enableNotifications()` | `descriptor.value = ...` + `gatt.writeDescriptor(descriptor)` (API < 33) | CCCD on CHAR_NOTIFY (0x2902) | Same as #1, deprecated API path for API < 33. | **SAFE — CCCD only** | **KEEP — enabled in all modes** |
| 3 | `writeToCharWrite()` | `gatt.writeCharacteristic(...)` (API ≥ 33) | CHAR_WRITE (0x5352) | Writes handshake response or ping. **DISABLED in PASSIVE mode.** | **CONDITIONAL — EXPERIMENTAL only** | **GATED behind ProtocolMode.EXPERIMENTAL** |
| 4 | `writeToCharWrite()` via `handlePacket()` | Challenge response when `0x9A 0xF2` detected | CHAR_WRITE (0x5352) | Auth handshake response. **DISABLED in PASSIVE mode.** | **CONDITIONAL — EXPERIMENTAL only** | **GATED — Jupiter AES key UNVERIFIED on RTR 310** |
| 5 | `writeToCharWrite()` via `startPing()` | Keep-alive ping on interval | CHAR_WRITE (0x5352) | Ping packet. **DISABLED in PASSIVE mode.** | **CONDITIONAL — EXPERIMENTAL only** | **GATED — Ping format UNVERIFIED on RTR 310** |

---

## Summary

| Characteristic | UUID | Write Type | Count | Mode | Status |
|----------------|------|-----------|-------|------|--------|
| CCCD on CHAR_NOTIFY | 0x2902 | Descriptor write | 2 (API split) | ALL modes | ✅ Required — enables passive notifications |
| CHAR_WRITE | 0x5352 | Characteristic write | 2 (handshake + ping) | EXPERIMENTAL only | ⚠️ DISABLED in PASSIVE mode (default) |

---

## Writes to CHAR_WRITE — Detail

### Handshake Response (`0x9A 0xF1`) — DISABLED IN PASSIVE MODE
- **Status: DISABLED** — gated behind `ProtocolMode.EXPERIMENTAL`
- **Reason:** Jupiter AES-128-CTR key is **UNVERIFIED** on RTR 310.
  Sending an incorrect response may cause the bike to ignore the app or disconnect.
- **When re-enable:** After btsnoop log from TVS Connect confirms the challenge/response
  packets and key on RTR 310. Update KNOWN_FACTS.md and ADR-004 first.
- **ADR:** ADR-004 (pending hardware verification)

### Keep-Alive Ping (`0x5B 0x4A`) — DISABLED IN PASSIVE MODE
- **Status: DISABLED** — gated behind `ProtocolMode.EXPERIMENTAL`
- **Reason:** Ping packet format derived from Jupiter RE. Not verified on RTR 310.
  Content includes: time, date, battery, signal, network type, temperature.
- **When re-enable:** After ping format and necessity confirmed on RTR 310.
- **ADR:** ADR-004 (pending hardware verification)

---

## Safety Rules (Standing)

1. **CHAR_WRITE writes are disabled by default** (PASSIVE mode). No writes to 0x5352
   unless explicitly enabled via `ProtocolMode.EXPERIMENTAL`.
2. Any change to enable EXPERIMENTAL mode requires explicit opt-in code change and
   must be re-audited.
3. CCCD writes on CHAR_NOTIFY are always permitted — they enable passive notification only.
4. Re-run this audit whenever any write operation is added or modified.
5. Jupiter-derived protocol information must not be sent to the RTR 310 until
   independently verified via btsnoop HCI log capture.

---

## UI Warning (Required)

When `ProtocolMode.PASSIVE` is active (default), the following must be logged:
```
⚠️ Experimental protocol writes disabled (PASSIVE mode).
   App is passive: scan → connect → discover → enable notifications → capture.
   No writes to CHAR_WRITE (0x5352).
```

---

## Re-Audit Trigger

Re-run whenever:
- Any `writeCharacteristic`, `writeDescriptor`, or `setValue` call is added
- A new BLE characteristic is interacted with
- Protocol mode is changed from PASSIVE to EXPERIMENTAL
- Sprint 5+ implements new command types
