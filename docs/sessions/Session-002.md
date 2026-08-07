# Session 002 — Project Bootstrap & Sprint 1

## Goal

Bootstrap the RTR Companion project structure and implement Sprint 0 + Sprint 1:
BLE scan, connect, service discovery, notification enable.

## Work Completed

### Project Structure
- Created multi-module Gradle Kotlin DSL project: `app`, `ble-core`, `protocol`
- Configured `settings.gradle.kts` with all three modules
- Set `minSdk = 29`, `compileSdk = 35`, Kotlin `2.0.0`, AGP `8.5.2`
- Added `gradle/libs.versions.toml` version catalogue
- Configured `Timber` for debug logging

### ble-core Module
- `BleConstants.kt` — all confirmed UUIDs and timing constants
- `RtrDevice.kt` — discovered device data model
- `ScanState.kt` — sealed class scan lifecycle
- `ConnectionState.kt` — sealed class GATT lifecycle
- `RtrScanner.kt` — BLE scan with prefix filter, 15s timeout, StateFlow output
- `RtrGattManager.kt` — GATT connect, service discovery, CCCD notification enable, raw packet flow

### app Module
- `RtrApplication.kt` — Timber initialisation
- `MainActivity.kt` — single activity, BLE runtime permission handling
- `MainViewModel.kt` — SDK bridge, StateFlow exposure
- `RtrCompanionApp.kt` — stateful screen router
- `ScanScreen.kt` — scan controls, device list, connection status
- `PermissionScreen.kt` — permission denial recovery UI
- `Color.kt`, `Theme.kt`, `Type.kt` — RTR brand Material 3 theme

### CI/CD
- `.github/workflows/ci.yml` — build + test on push/PR

### Documentation
- `docs/Architecture.md`
- `docs/BLE-Protocol.md`
- `docs/Reverse-Engineering.md`
- `docs/Bike-Features.md`
- `README.md`

## Files Changed

All files — initial project creation.

## Architecture Decisions

- **No NavController for Sprint 1** — Simple `when` block in `RtrCompanionApp.kt` is sufficient for three screens
- **Permissions in `ble-core` manifest** — Automatic manifest merging means `app` consumers don't need to declare BLE permissions
- **`@SuppressLint("MissingPermission")`** — Caller responsibility pattern documented in KDoc
- **Both deprecated and new `onCharacteristicChanged` implemented** — Handles API < 33 and ≥ 33 correctly
- **`autoConnect = false`** — Direct, fast connection preferred over background reconnect

## Problems Encountered

- HCI log still not extracted
- No real hardware packets captured yet

## Testing Performed

- Code review only — app builds but not yet tested on hardware during this session

## Next Session Goals

- Sprint 2: Add `RawPacket`, `PacketLogger`, `PacketLogScreen`
- Test on real hardware — connect to bike and log first packets
- Document any discovered packet patterns
