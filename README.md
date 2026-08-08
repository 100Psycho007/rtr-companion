# RTR Companion

Open-source Android companion app and Bluetooth SDK for the **TVS Apache RTR 310 SmartXonnect TFT**.

RTR Companion is a reverse-engineering research project. The goal is to understand the BLE protocol used by the TVS Connect system, document it completely, and build a clean, reusable SDK and companion app on top of that understanding.

This is not a clone of TVS Connect. The project is SDK-first — the BLE communication layer is isolated in `ble-core` and `protocol` so it can eventually be reused or published independently of the Android app.

---

## Features

### Currently Implemented

- **BLE scan** — scans for RTR 310 devices by name prefix (`TVSRTR310`)
- **BLE connect** — establishes GATT connection with automatic service discovery
- **Passive notification capture** — enables notifications on the NOTIFY characteristic and logs every raw byte received
- **Live hex log** — real-time scrolling packet log on screen
- **Packet export** — share a captured session as a plain-text `.txt` file
- **Permission handling** — graceful runtime permission flow for Android 12+ and legacy

### Reverse Engineering Progress

- Proprietary TVS BLE service UUID identified (experimentally confirmed)
- WRITE characteristic (0x5352) and NOTIFY characteristic (0x5354) confirmed
- Passive notification capture working — raw bytes received from bike
- Packet format not yet decoded — Sprint 5 will begin analysis once sufficient captures exist

### BLE Capabilities

| Capability | Status |
|-----------|--------|
| BLE scan | ✅ Working |
| GATT connect | ✅ Working |
| Service discovery | ✅ Working |
| Enable NOTIFY notifications | ✅ Working |
| Receive raw packets | ✅ Working |
| Decode packet format | 🔜 Sprint 5 |
| Send WRITE commands | 🚫 Disabled until protocol is documented |

---

## Architecture

RTR Companion is a multi-module Gradle project with clear responsibility separation.

### Modules

#### `app/`

The Android application. Contains all UI, navigation, and the ViewModel bridge.

- `MainActivity` — single-activity host; handles runtime permissions and export share intent
- `RtrApplication` — application class; initialises Timber logging
- `MainViewModel` — bridges `ble-core` and `protocol` to the Compose UI layer
- `ui/RtrCompanionApp` — state-based screen router (no NavController needed yet per ADR-001)
- `ui/screen/PermissionScreen` — guides user through BLE permission grant
- `ui/screen/ScanScreen` — BLE scan controls and device list
- `ui/screen/PacketLogScreen` — live auto-scrolling hex log with Export and Clear
- `export/PacketExporter` — writes session to cache and produces a share `Intent`

#### `ble-core/`

The BLE SDK layer. Contains all Android Bluetooth API interactions. Pure BLE — no UI, no protocol decoding.

- `BleConstants` — all confirmed BLE UUIDs and timing constants
- `RtrScanner` — scans for RTR 310 devices; emits `StateFlow<ScanState>`
- `RtrGattManager` — manages the GATT connection lifecycle; emits `StateFlow<ConnectionState>` and `SharedFlow<ByteArray>` for raw packets
- `model/` — `RtrDevice`, `ScanState`, `ConnectionState` data models

#### `protocol/`

Protocol and analysis layer. Depends on `ble-core` for shared models.

- `RawPacket` — raw byte container with hex display; the currency of the packet pipeline
- `PacketLogger` — 500-entry ring buffer; `StateFlow<List<RawPacket>>` for UI
- `PacketAnalyzer` — analysis stub (Sprint 5 placeholder); `ParsedPacket` sealed class

#### `captures/`

Raw packet capture files from hardware sessions. Each file is a timestamped export from the app. Used as input for reverse engineering.

#### `docs/`

All research documentation:
- `BLE-Protocol.md` — observed BLE structure and protocol notes
- `KNOWN_FACTS.md` — confirmed / hypothesis / rejected knowledge base
- `ROADMAP.md` — full sprint roadmap
- `Architecture.md` — module dependency and data flow diagrams
- `adr/` — Architecture Decision Records
- `security/` — BLE safety audit
- `sessions/` — session-by-session work logs

#### `scripts/`

Utility scripts for development and research tasks.

### Data Flow

```
RTR 310 BLE Peripheral
        │
        │  NOTIFY characteristic (0x5354)
        ▼
  RtrGattManager          ← ble-core
        │  SharedFlow<ByteArray>
        ▼
  MainViewModel           ← app
        │  records RawPacket
        ▼
  PacketLogger            ← protocol
        │  StateFlow<List<RawPacket>>
        ▼
  PacketLogScreen         ← app/ui
        │
        ▼
  [user exports to .txt]
```

---

## Current Sprint

### Sprint 3 — Export & Analysis Foundation 🔄 In Progress

| Task | Status |
|------|--------|
| Packet export (share as `.txt`) | ✅ Done |
| `PacketAnalyzer` skeleton | ✅ Done |
| All documentation caught up | ✅ Done |
| Real hardware capture session | ⏳ Pending |
| Update `docs/BLE-Protocol.md` from captures | ⏳ Pending |

---

## Development Roadmap

| Sprint | Goal | Status |
|--------|------|--------|
| 0 | Environment setup, BLE discovery | ✅ Done |
| 1 | Scan, connect, service discovery, notifications | ✅ Done |
| 2 | Packet logger, live hex log UI | ✅ Done |
| 3 | Packet export, analysis skeleton | 🔄 In Progress |
| 4 | Navigation Compose, settings screen, UI polish | 🔜 Planned |
| 5 | Protocol parsing — first decoded packet type | 🔜 Planned |
| 6 | Feature display (speed, RPM, ride mode) | 🔜 Planned |
| 7 | SDK stabilisation, public beta | 🔜 Planned |

See `docs/ROADMAP.md` for full detail.

---

## Build Instructions

### Requirements

- **Android Studio** Hedgehog or later
- **JDK 17** (bundled with Android Studio)
- **minSdk** 29 (Android 10)
- **targetSdk / compileSdk** 35 (Android 15)
- Gradle 8.x (via wrapper — `gradlew`)

### Run

1. Clone the repository
2. Open in Android Studio
3. Connect an Android device running Android 10+ with BLE
4. Run the `app` configuration

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### CI Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

GitHub Actions runs both on every push to `main` and `develop`, and on every pull request to `main`.

---

## Contribution Guide

### Coding Standards

- Kotlin only. No Java.
- Jetpack Compose for all UI.
- Material 3.
- `StateFlow` / `SharedFlow` for all observable state.
- Coroutines for async work.
- KDoc on every public class and function.
- No hardcoded strings — use `BleConstants` for UUIDs, constants for timing.

### Branch Naming

| Type | Format |
|------|--------|
| Feature | `feature/short-description` |
| Bug fix | `fix/short-description` |
| Documentation | `docs/short-description` |
| Sprint work | `sprint/N-short-description` |

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add packet export button
fix: handle null CCCD descriptor gracefully
docs: update BLE-Protocol.md with capture observations
refactor: extract enableNotifications() helper
```

### Documentation Requirements

- Update `PROJECT_STATE.md` after every session.
- Log every session in `docs/sessions/Session-NNN.md`.
- New ADRs in `docs/adr/` for any significant architecture decision.
- Update `docs/KNOWN_FACTS.md` when new evidence is gathered.
- Never write undocumented protocol speculations as facts — see KNOWN_FACTS.md rules.

---

## Repository Structure

```
rtr-companion/
├── .github/
│   ├── workflows/ci.yml
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   ├── feature_request.md
│   │   └── protocol_discovery.md
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── CODEOWNERS
├── app/
│   └── src/main/java/dev/rtrcompanion/app/
│       ├── MainActivity.kt
│       ├── RtrApplication.kt
│       ├── export/PacketExporter.kt
│       ├── ui/RtrCompanionApp.kt
│       ├── ui/screen/{PermissionScreen,ScanScreen,PacketLogScreen}.kt
│       ├── ui/theme/{Color,Theme,Type}.kt
│       └── viewmodel/MainViewModel.kt
├── ble-core/
│   └── src/main/java/dev/rtrcompanion/blecore/
│       ├── BleConstants.kt
│       ├── connection/RtrGattManager.kt
│       ├── model/{ConnectionState,RtrDevice,ScanState}.kt
│       └── scanner/RtrScanner.kt
├── protocol/
│   └── src/main/java/dev/rtrcompanion/protocol/
│       ├── PacketAnalyzer.kt
│       ├── PacketLogger.kt
│       └── RawPacket.kt
├── captures/             ← raw .txt packet exports from hardware sessions
├── docs/
│   ├── Architecture.md
│   ├── BLE-Protocol.md
│   ├── Bike-Features.md
│   ├── KNOWN_FACTS.md
│   ├── Reverse-Engineering.md
│   ├── ROADMAP.md
│   ├── SECURITY.md
│   ├── TESTING.md
│   ├── adr/              ← Architecture Decision Records
│   ├── research/         ← raw research notes and HCI log analysis
│   ├── security/         ← BLE safety audit documents
│   └── sessions/         ← per-session work logs
├── scripts/              ← utility scripts
├── CONTRIBUTING.md
├── PROJECT_STATE.md
└── README.md
```
