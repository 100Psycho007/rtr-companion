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

Last audited: 2026-08-08

---

## Findings

### File: `ble-core/.../connection/RtrGattManager.kt`

| # | Function | Pattern | Characteristic | Purpose | Classification | Action |
|---|----------|---------|----------------|---------|----------------|--------|
| 1 | `enableNotifications()` | `gatt.writeDescriptor(descriptor, ENABLE_NOTIFICATION_VALUE)` (API ≥ 33) | CCCD on CHAR_NOTIFY (0x2902) | Enables BLE notifications on CHAR_NOTIFY. Descriptor write only. | **SAFE — CCCD only** | Keep |
| 2 | `enableNotifications()` | `descriptor.value = ...` + `gatt.writeDescriptor(descriptor)` (API < 33) | CCCD on CHAR_NOTIFY (0x2902) | Same as #1, deprecated API path for API < 33. | **SAFE — CCCD only** | Keep |
| 3 | `writeToCharWrite()` | `gatt.writeCharacteristic(characteristic, data, ...)` (API ≥ 33) | CHAR_WRITE (0x5352) | Sends handshake response and keep-alive ping. See entries below. | **CONDITIONAL — see #4, #5** | Keep |
| 4 | `writeToCharWrite()` via `handlePacket()` | Calls `writeToCharWrite(response)` when challenge detected | CHAR_WRITE (0x5352) | Authentication handshake response (`0x9A 0xF1`). Required to unlock telemetry. Documented in ADR-004. | **SAFE — documented, ADR-004** | Keep |
| 5 | `writeToCharWrite()` via `startPing()` | Calls `writeToCharWrite(ping)` on interval | CHAR_WRITE (0x5352) | Keep-alive ping (`0x5B 0x4A`). Maintains connection, updates cluster display. Documented in `docs/BLE-Protocol.md`. | **SAFE — documented, ADR-004** | Keep |

---

## Summary

| Characteristic | UUID | Write Type | Count | Status |
|----------------|------|-----------|-------|--------|
| CCCD on CHAR_NOTIFY | 0x2902 | Descriptor write | 2 (API split) | ✅ Required — enables passive notifications |
| CHAR_WRITE | 0x5352 | Characteristic write | 2 (handshake + ping) | ✅ Documented — ADR-004, `docs/BLE-Protocol.md` |

---

## Writes to CHAR_WRITE — Detail

### Handshake Response (`0x9A 0xF1`)
- **When:** Only when bike sends `0x9A 0xF2` challenge packet
- **Content:** AES-128-CTR encrypted response to bike challenge
- **Purpose:** Authenticate the app so bike streams live telemetry
- **ADR:** ADR-004
- **Key status:** Jupiter key used as initial attempt — unverified on RTR 310

### Keep-Alive Ping (`0x5B 0x4A`)
- **When:** Every 1000ms after handshake (or after 3s timeout if no challenge)
- **Content:** Phone status (time, battery, signal) — no bike state modification
- **Purpose:** Maintain BLE connection, update cluster display with phone info
- **ADR:** ADR-004
- **Find Me flag:** Byte 17 = `0x00` by default. `0x01` triggers indicator flash — NOT implemented in current code (future feature)

---

## Safety Rules (Standing)

1. **CHAR_WRITE writes are permitted only** for the handshake response and keep-alive ping as documented in ADR-004.
2. Any new CHAR_WRITE command requires a new ADR and must be added to this table before merging.
3. CCCD writes on CHAR_NOTIFY are always permitted — they enable passive notification.
4. Re-run this audit whenever any write operation is added or modified.

---

## Re-Audit Trigger

Re-run whenever:
- Any `writeCharacteristic`, `writeDescriptor`, or `setValue` call is added
- A new BLE characteristic is interacted with
- Sprint 5+ implements new command types
