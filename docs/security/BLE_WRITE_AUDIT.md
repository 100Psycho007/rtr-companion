# BLE Write Audit — Sprint 1 Safety Verification

> **Purpose:** Confirm that the RTR Companion application NEVER sends unsolicited
> packets to CHAR_WRITE (0x5352) during Sprint 1, 2, or 3.
> This document must be updated whenever any write operation is added or changed.

---

## Audit Scope

All `.kt` source files in the repository were searched for the following patterns:

- `writeCharacteristic(`
- `BluetoothGatt.writeCharacteristic(`
- `characteristic.setValue(`
- `BluetoothGattCharacteristic.setValue(`
- `writeDescriptor(`
- Any helper that eventually performs a write to a characteristic

Search executed: 2026-08-08

---

## Findings

### File: `ble-core/src/main/java/dev/rtrcompanion/blecore/connection/RtrGattManager.kt`

| # | Function | Line (approx) | Pattern Found | Purpose | Classification | Action |
|---|----------|---------------|---------------|---------|---------------|--------|
| 1 | `enableNotifications()` | ~222 | `gatt.writeDescriptor(descriptor, ENABLE_NOTIFICATION_VALUE)` (API 33+) | Writes the standard CCCD descriptor to enable BLE notifications on CHAR_NOTIFY (0x5354). This is a write to a **descriptor**, not to a characteristic. | **SAFE — CCCD only** | Keep as-is |
| 2 | `enableNotifications()` | ~227–229 | `descriptor.value = ENABLE_NOTIFICATION_VALUE` + `gatt.writeDescriptor(descriptor)` (API < 33, deprecated path) | Same purpose as #1 via the deprecated API for devices below Android 13. | **SAFE — CCCD only** | Keep as-is |

---

## No Writes Found to CHAR_WRITE (0x5352)

A full-text search across all Kotlin source files found **zero calls** to:

- `writeCharacteristic(...)` — not present anywhere
- `BluetoothGatt.writeCharacteristic(...)` — not present anywhere
- `characteristic.setValue(...)` — not present anywhere
- Any utility that wraps characteristic writes — not present

The constant `BleConstants.CHAR_WRITE` is defined in `BleConstants.kt` but is never
passed to any write operation. It exists solely as a named constant for documentation
and for future use when the protocol is sufficiently understood.

---

## Summary

| Characteristic | UUID | Write Operations Found | Status |
|---|---|---|---|
| CHAR_WRITE | 0x5352 | 0 | ✅ Safe — never written |
| CHAR_NOTIFY | 0x5354 | 0 characteristic writes | ✅ Safe |
| CCCD on CHAR_NOTIFY | 0x2902 | 2 (descriptor writes only) | ✅ Required — enables notifications |

---

## Conclusion

**The application is fully passive with respect to CHAR_WRITE (0x5352) during Sprint 1–3.**

The only write operations present are CCCD descriptor writes on CHAR_NOTIFY (0x5354),
which are the standard Android BLE mechanism for enabling notifications. These writes
target the CCCD descriptor (`00002902-...`), not the characteristic itself, and they
instruct the peripheral to start sending notifications. This is a standard, safe
BLE pattern required for passive monitoring.

---

## Safety Rules (Standing)

1. **Never write to CHAR_WRITE (0x5352)** until the full packet format is documented
   in `docs/BLE-Protocol.md` and approved via ADR.
2. **CCCD writes on CHAR_NOTIFY are permitted and required** — they enable passive
   notification reception with no side effects on the bike.
3. This document must be re-run as an audit whenever any new BLE write is considered.
4. Any characteristic write must first be documented in an ADR and reviewed for safety.

---

## Re-Audit Trigger

Re-run this audit whenever:
- Any `writeCharacteristic`, `writeDescriptor`, or `setValue` call is added
- A new BLE characteristic is interacted with
- Sprint 5+ begins implementing write commands
