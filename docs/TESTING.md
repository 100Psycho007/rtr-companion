# Testing Guide

## Overview

RTR Companion is tested at three levels:
1. **Unit tests** — pure logic in `protocol/` and `ble-core/` (no hardware required)
2. **Instrumented tests** — Android components with mocked BLE (no hardware required)
3. **Hardware tests** — real RTR 310 device with nRF Connect or the RTR Companion app

---

## Unit Tests

### Running

```bash
./gradlew testDebugUnitTest
```

### Location

| Module | Test directory |
|--------|---------------|
| `app` | `app/src/test/java/` |
| `ble-core` | `ble-core/src/test/java/` |
| `protocol` | `protocol/src/test/java/` |

### What to Test

- `PacketLogger` — ring buffer overflow, clear, export format
- `RawPacket` — hex property, equals/hashCode with ByteArray
- `PacketAnalyzer` — returns null for all inputs (Sprint 3); packet parsing correctness (Sprint 5+)
- `ScanState` / `ConnectionState` — sealed class transitions

**Note:** No unit tests exist yet (Sprint 3). Writing tests for `PacketLogger` and
`RawPacket` is planned for Sprint 7 per the roadmap.

---

## Android Studio

### Setup

1. Open the project in Android Studio Hedgehog or later.
2. Ensure JDK 17 is configured (File → Project Structure → SDK Location).
3. Use the `app` run configuration to deploy to a connected device.

### Recommended Emulator

The BLE stack is not available on most emulators. Use a physical Android device.
Minimum Android 10 (API 29).

---

## ADB

Useful ADB commands for debugging:

```bash
# View Timber log output in real time
adb logcat -s RTR

# Clear log before a hardware session
adb logcat -c

# Pull a packet capture from internal cache
adb shell run-as dev.rtrcompanion.app ls /data/data/dev.rtrcompanion.app/cache/captures/
adb pull /data/data/dev.rtrcompanion.app/cache/captures/<filename> .
```

---

## BLE Testing with nRF Connect

Use **nRF Connect** (by Nordic Semiconductor) to manually inspect the RTR 310 BLE stack
before or alongside app testing.

### Setup

1. Install nRF Connect from Google Play.
2. Enable Bluetooth on your Android device.
3. Open nRF Connect → Scanner tab.
4. Filter for devices starting with `TVSRTR310`.
5. Tap the device to connect.

### What to Check

- Verify all services are present: 0x1800, 0x1801, 0x180A, TVS proprietary
- Verify characteristics 0x5352 (WRITE) and 0x5354 (NOTIFY) are present in the TVS service
- Enable notifications on 0x5354 and observe raw bytes
- Record the bytes in `captures/` and log the session in `docs/sessions/`

---

## RTR Hardware Session Testing

### Before Connecting

- Confirm the bike is stationary and not in motion
- No rider on the bike during active test sessions
- Have the app's Logcat filter (`adb logcat -s RTR`) ready before connecting
- Do NOT tap any Write buttons or send any commands unless testing a documented command

### Connection Checklist

- [ ] App shows `PermissionScreen` → tap Grant → shows `ScanScreen`
- [ ] Tap Scan → device `TVSRTR310...` appears in list within 15s
- [ ] Tap device → app shows "Connecting..." → "Discovering services..." → "Ready"
- [ ] Logcat shows services: 1800, 1801, 180A, TVSM (short form of proprietary UUID)
- [ ] Logcat shows characteristics: 5352 (WRITE), 5354 (NOTIFY)
- [ ] Logcat shows "Notifications enabled on 5354"
- [ ] `PacketLogScreen` appears and packets start appearing

### Packet Capture Checklist

- [ ] At least 60 seconds of packets received and visible
- [ ] Note packet rate (approx packets per second)
- [ ] Note any observable patterns (fixed-length vs variable, recurring byte sequences)
- [ ] Tap Export → save the `.txt` file → copy to `captures/` in the repo
- [ ] Log the session in `docs/sessions/Session-NNN.md`
- [ ] Update `docs/BLE-Protocol.md` with any new observations
- [ ] Update `docs/KNOWN_FACTS.md` with confirmed facts

---

## Regression Checklist

Run this checklist after any significant code change:

| Check | Expected result |
|-------|----------------|
| App builds | `./gradlew assembleDebug` succeeds with zero errors |
| Unit tests pass | `./gradlew testDebugUnitTest` zero failures |
| Permissions granted path | `ScanScreen` shown after granting permissions |
| Permissions denied path | `PermissionScreen` shown; both buttons functional |
| BLE scan | Devices found within 15s when in range |
| BLE connect | Connection reaches `Ready` state |
| Packet log | Packets appear in `PacketLogScreen` |
| Export | Share sheet opens; `.txt` file contains valid content |
| Disconnect | Returns to `ScanScreen` without crash |
| Clear log | Log empties; count shows 0 packets |
| No writes to 0x5352 | BLE Write Audit must still show 0 characteristic writes |
