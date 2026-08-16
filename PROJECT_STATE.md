# PROJECT_STATE.md

> This file is the project's memory. Any engineer or AI must be able to read this file
> and understand the complete current state of the project immediately.

---

## Current Sprint

**Sprint 3 — Export & Analysis Foundation** (completing)
**Protocol Integrity Correction applied: 2026-08-08**
**Logcat Analysis Session: 2026-08-15**

---

## Current Goal

Complete the Sprint 3 remaining tasks: hardware capture session, update protocol docs
from real packet data, then begin Sprint 4 (Navigation Compose, UI polish).

---

## Sprint Status

| Sprint | Description | Status |
|--------|-------------|--------|
| 0 | Environment setup, BLE discovery | ✅ Done |
| 1 | Permissions, scanner, GATT connect, service discovery, notification enable | ✅ Done |
| 2 | Notification logger, PacketLogger, PacketLogScreen, raw packet capture | ✅ Done |
| 3 | Packet export UI, PacketAnalyzer skeleton, docs & architecture cleanup | 🔄 In Progress |
| 4 | Compose navigation, UI polish, settings screen | 🔜 Planned |
| 5 | Protocol parsing — first decoded packet type | 🔜 Planned |
| 6 | Feature implementation (speed/RPM/ride mode display) | 🔜 Planned |
| 7 | SDK stabilisation, public beta | 🔜 Planned |

---

## Completed

### Sprint 0
- [x] Gradle Kotlin DSL project structure created
- [x] Three modules: `app`, `ble-core`, `protocol`
- [x] CI/CD via GitHub Actions (build + unit tests)
- [x] Timber logging configured
- [x] Brand theme (RtrOrange, dark/light, Material 3 dynamic color)

### Sprint 1
- [x] BLE permissions (API 31+ and legacy ≤ 30) declared in `ble-core` manifest
- [x] Runtime permission request in `MainActivity`
- [x] `RtrScanner` — name-prefix filtered BLE scan, 15s timeout, `StateFlow<ScanState>`
- [x] `RtrGattManager` — GATT connect, service discovery, CCCD notification enable, raw packet flow
- [x] `ScanScreen` — scan controls, device list, connection status banner
- [x] `PermissionScreen` — permission explanation with two recovery paths

### Sprint 2
- [x] `RawPacket` — raw byte container with hex property, timestamp
- [x] `PacketLogger` — 500-entry ring buffer, `StateFlow<List<RawPacket>>`, export
- [x] `MainViewModel` — connects GattManager → PacketLogger pipeline
- [x] `PacketLogScreen` — live auto-scrolling hex log, disconnect, clear
- [x] `RtrCompanionApp` router — permission → scan → packet log navigation

### Sprint 3 (in progress)
- [x] `PacketExporter` — writes capture to cache dir, returns Android share Intent
- [x] `FileProvider` — declared in manifest, `res/xml/file_provider_paths.xml` created
- [x] Export button wired into `PacketLogScreen` (enabled only when packets exist)
- [x] `RtrCompanionApp` updated with `onExportLog` callback
- [x] `MainActivity` — `exportPacketLog()` launches share chooser
- [x] `PacketAnalyzer` skeleton — `analyze()` stub + `ParsedPacket` sealed class
- [x] `lifecycle-runtime-compose` + `lifecycle-viewmodel-compose` deps added
- [x] **Architecture review complete:**
  - [x] BLE write audit — `docs/security/BLE_WRITE_AUDIT.md` (zero characteristic writes confirmed)
  - [x] `smartx-sdk` naming removed — all docs now use `ble-core` consistently
  - [x] UUID ASCII speculation removed from `BleConstants.kt` and `docs/KNOWN_FACTS.md`
  - [x] `README.md` completely rewritten with all required sections
  - [x] `docs/Architecture.md` rewritten with diagrams and data flow
  - [x] `docs/SECURITY.md` created
  - [x] `docs/TESTING.md` created
  - [x] `docs/ROADMAP.md` updated with version history
  - [x] `docs/KNOWN_FACTS.md` updated (UUID comment corrected)
  - [x] `docs/research/`, `docs/testing/`, `tools/` directories created
  - [x] `.github/ISSUE_TEMPLATE/bug_report.md` created
  - [x] `.github/ISSUE_TEMPLATE/feature_request.md` created
  - [x] `.github/ISSUE_TEMPLATE/protocol_discovery.md` created
  - [x] `.github/PULL_REQUEST_TEMPLATE.md` created
  - [x] `.github/CODEOWNERS` created
  - [x] `CONTRIBUTING.md` created
  - [x] `PROJECT_STATE.md` — this file, updated

---

## In Progress

- [x] Real hardware session — connect to bike, collect packets, save to `captures/`
      (shutdown-burst capture, ignition OFF, 21 packets)
- [ ] Update `docs/BLE-Protocol.md` from real capture data (KNOWN_FACTS.md done;
      BLE-Protocol.md still pending)
- [x] Update `KNOWN_FACTS.md` when new evidence is gathered — packet length, payload
      range, and checksum byte position corrected in Session 004

---

## Blocked

Nothing currently blocked.

---

## Next Tasks

### Immediate (unblock live-ride telemetry — highest value)

1. **HCI btsnoop capture with ignition ON** ← the single most important thing
   - Enable developer options → "Bluetooth HCI snoop log" on your phone
   - Open TVS Connect, connect to bike with **ignition ON**
   - Let it run for 30+ seconds while riding or just with ignition on
   - Pull the btsnoop log: `adb bugreport` then extract `FS/data/misc/bluetooth/logs/btsnoop_hci.log`
   - Open in Wireshark (filter: `btle`) and look for:
     - `0x9A 0xF2` — confirms auth challenge exists on RTR 310
     - `0x9A 0xF1` — confirms the AES key (compare encrypted bytes)
     - `0x5B 0x4A` — confirms ping format and content
     - New message IDs not seen in shutdown capture (speed, RPM, 0x18/0x19)
   - This one capture answers questions 1–5 in KNOWN_FACTS.md simultaneously

2. **Decode `0x5F` checksum** — needs many live-ride samples (frame counter B7
   increments so the checksum changes every packet). A 30-second ride should give
   enough variants to solve the formula by linear algebra.

3. **Decode `0x7D` and `0x12`** — compare against known device facts (VIN, firmware
   version visible in TVS Connect settings). Static packets so one comparison is enough.

### Code (Sprint 4 prep)

4. **RSSI in RtrDevice** — `RtrScanner` currently sets `rssi = 0`. Wire up actual RSSI
   from `ScanResult` so the device list shows signal strength.

5. **Auto-reconnect** — add `autoConnect = true` path in `RtrGattManager` so the app
   reconnects when the bike comes back in range without user action.

6. **Navigation Compose** — Sprint 4 planned item. Replace the manual state routing
   in `RtrCompanionApp` with `NavController` for settings + about screens.

7. **Session stats overlay** — once connected, show packet rate (pkts/sec),
   unique message types seen, and session duration in the `PacketLogScreen` header.
   Straightforward with the data already flowing through `PacketLogger`.

---

## Known Facts

See full details in `docs/KNOWN_FACTS.md`.

### Confirmed
- Device advertises as `TVSRTR310` prefix (e.g. `TVSRTR310FKB0925`)
- Proprietary TVS service UUID experimentally confirmed: `5456534d-5647-5341-5342-454e544f5251`
- WRITE characteristic: `5352` (Phone → Bike) — never written in Sprints 1–3
- NOTIFY characteristic: `5354` (Bike → Phone)
- Standard services present: 0x1800, 0x1801, 0x180A
- BLE connection and notification enable confirmed working

### Hypothesis
- Protocol is packet-based (not confirmed by decoded data yet)
- Multiple features multiplexed through the single notify/write pair

---

## Known Risks

| Risk | Severity | Mitigation |
|------|----------|-----------|
| No real packet captures yet | High | Hardware session planned |
| Unit test coverage is zero | Medium | Planned for Sprint 7 |
| No NavController yet | Low | Simple state routing sufficient for 3 screens |
| `minSdk=29` — BLE deprecated APIs | Low | Dual callback implementation covers all API levels (ADR-003) |

---

## Open Questions

1. What is the packet structure? Header, length, type byte, checksum?
2. What commands does the phone send on `5352` at startup?
3. Are notifications sent continuously (polling) or event-driven?
4. Is there a connection handshake on `5352` before bike data starts?
5. How does the bike authenticate the phone app?
6. What is the notification rate (packets per second)?

---

## Repository Health

| Check | Status |
|-------|--------|
| CI (build) | ✅ Should pass — all code is syntactically correct Kotlin |
| Unit tests | ⚠️ No test cases written yet — planned for Sprint 7 |
| Lint | ⚠️ Not yet run via CI lint step |
| No hardcoded secrets | ✅ Clean |
| Documentation | ✅ Current |
| BLE write audit | ✅ Zero characteristic writes — `docs/security/BLE_WRITE_AUDIT.md` |
| Naming consistency | ✅ `ble-core` used everywhere — `smartx-sdk` references removed |

---

## Module Summary

| Module | Key Files | Status |
|--------|-----------|--------|
| `ble-core` | `BleConstants`, `ProtocolMode`, `RtrScanner`, `RtrGattManager`, `HandshakeManager` (disabled), `PingPacketBuilder` (disabled), models | Stable — PASSIVE mode default |
| `protocol` | `RawPacket`, `PacketLogger`, `PacketAnalyzer` (stub) | Sprint 3 complete |
| `app` | `MainActivity`, `MainViewModel`, 3 screens, `PacketExporter` | Sprint 3 complete |

---

## Architecture Snapshot

```
app/
  MainActivity            ← permissions + export share intent
  RtrApplication          ← Timber init
  export/
    PacketExporter        ← cache file write + FileProvider share intent
  viewmodel/
    MainViewModel         ← scan + connect + packet pipeline
  ui/
    RtrCompanionApp       ← state router (onExportLog callback)
    screen/
      PermissionScreen    ← permission denied recovery
      ScanScreen          ← BLE scan + connect
      PacketLogScreen     ← live hex log + Export + Clear buttons

ble-core/
  BleConstants            ← all confirmed BLE UUIDs + timing constants
  scanner/RtrScanner      ← BLE scan with StateFlow output
  connection/RtrGattManager ← GATT lifecycle + raw packet emission
  model/                  ← RtrDevice, ScanState, ConnectionState

protocol/
  RawPacket               ← raw byte container (parser placeholder)
  PacketLogger            ← 500-entry ring buffer StateFlow
  PacketAnalyzer          ← analysis stub (Sprint 5 will add real parsing)
  ParsedPacket            ← sealed class, empty until format is confirmed
```

---

## Recent Changes

- **2026-08-15 (Session 006)** — Logcat analysis + bonding support + improved UI:
  - `BleConstants.PING_INTERVAL_MS` corrected from 1000ms → **200ms** (confirmed from
    official TVS Connect app logcat: `ApacheMobileToCluster.sendMobileData` fires ~5Hz)
  - `ConnectionState.Bonding` added — new state for first-time OS-level BLE pairing
  - `RtrGattManager` now registers `BroadcastReceiver` for `ACTION_BOND_STATE_CHANGED`
    to surface bonding state to UI and handle pairing rejection/timeout gracefully
  - `ScanScreen` updated to display "Pairing with X — accept the dialog" during bonding
  - `PacketLogScreen` upgraded: inline decoded annotations per packet, type-count summary
    in header, checksum pass/fail indicator, colour-coded rows by known/unknown type
  - `KNOWN_FACTS.md` updated with pairing findings and official app timing evidence
  - All changes build clean (`assembleDebug` passes)
  - All writes to CHAR_WRITE disabled by default (ProtocolMode.PASSIVE)
  - `ProtocolMode` enum added to ble-core
  - HandshakeManager and PingPacketBuilder isolated behind EXPERIMENTAL flag
  - Checksum analysis completed: per-message-type constant C derived for 5 of 6 types
  - Jupiter formula `255 − sum mod 256` confirmed NOT matching RTR 310 values
  - 0x5F checksum remains UNRESOLVED
  - `docs/BLE-Protocol.md` corrected: 19-byte claim removed, now states 20 bytes
  - `docs/protocol/capture-20260808-150945.md` created — full byte-by-byte analysis
  - `docs/protocol/PROTOCOL_STATUS.md` created — per-packet confidence tracker
  - `docs/research/JUPITER_CROSS_REFERENCE.md` created — Jupiter vs RTR 310 separation
  - `bug report hci/` added to `.gitignore`

- **2026-08-08 (Session 004)** — First hardware capture analysed:
  - 21 packets from shutdown burst (ignition OFF)
  - 20-byte frame structure confirmed (corrected from 19)
  - Checksum at byte 18 confirmed (corrected from byte 17)
  - 6 message types identified: 0x10, 0x11, 0x12, 0x5F, 0x7D, 0x42(control)

- **2026-08-08 (earlier)** — Architecture review: BLE write audit confirmed clean; UUID ASCII
  speculation removed; README rewritten; Architecture/Security/Testing docs created;
  GitHub hygiene files added; naming consistency enforced (`ble-core` everywhere).

---

## Last Updated

2026-08-08 — Session 005: Protocol integrity correction. PASSIVE mode default. All writes to CHAR_WRITE disabled.
