# PROJECT_STATE.md

> This file is the project's memory. Any engineer or AI must be able to read this file
> and understand the complete current state of the project immediately.

---

## Current Sprint

**Sprint 3 — Export & Analysis Foundation**

---

## Sprint Status

| Sprint | Description | Status |
|--------|-------------|--------|
| 0 | Environment setup, BLE discovery | ✅ Done |
| 1 | Permissions, scanner, GATT connect, service discovery, notification enable | ✅ Done |
| 2 | Notification logger, PacketLogger, PacketLogScreen, raw packet capture | ✅ Done |
| 3 | Packet export UI, PacketAnalyzer skeleton, docs catch-up | 🔄 In Progress |
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
- [x] `RtrGattManager` — GATT connect, service discovery, notification enable, `StateFlow<ConnectionState>`
- [x] `ScanScreen` — scan controls, device list, connection status banner
- [x] `PermissionScreen` — permission explanation with two recovery paths

### Sprint 2
- [x] `RawPacket` — raw byte container with hex property, timestamp
- [x] `PacketLogger` — 500-entry ring buffer, `StateFlow<List<RawPacket>>`, export stub
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
- [x] `PROJECT_STATE.md` created
- [x] `docs/KNOWN_FACTS.md` created
- [x] `docs/ROADMAP.md` created
- [x] `docs/sessions/Session-001.md` through `Session-003.md` created
- [x] `docs/adr/ADR-001.md` through `ADR-003.md` created

---

## In Progress

- [ ] Real hardware session — connect to bike, collect packets, save to `captures/`
- [ ] Update `docs/BLE-Protocol.md` from real capture data
- [ ] Update `KNOWN_FACTS.md` when new evidence is gathered

---

## Blocked

Nothing currently blocked.

---

## Next Tasks

1. Hardware session: connect to bike, export a capture via the new Export button
2. Save the exported file to `captures/` in the repo
3. Analyse the capture — look for patterns (header bytes, packet length, type byte)
4. Update `docs/BLE-Protocol.md` with observations
5. Promote confirmed hypotheses in `KNOWN_FACTS.md`
6. Write `docs/sessions/Session-004.md` after hardware session
7. Sprint 4: introduce Navigation Compose for settings + about screens

---

## Known Facts

See full details in `docs/KNOWN_FACTS.md`.

**Confirmed:**
- Device advertises as `TVSRTR310` prefix (e.g. `TVSRTR310FKB0925`)
- Proprietary TVS service UUID: `5456534d-5647-5341-5342-454e544f5251`
- WRITE characteristic: `5352` (Phone → Bike)
- NOTIFY characteristic: `5354` (Bike → Phone)
- Standard services present: 0x1800, 0x1801, 0x180A
- BLE connection and notification enable confirmed working

**Hypothesis:**
- Protocol is packet-based (not confirmed by decoded data yet)
- Multiple features multiplexed through the single notify/write pair

---

## Open Questions

1. What is the packet structure? Header, length, type byte, checksum?
2. What commands does the phone send on `5352` at startup?
3. Are notifications sent continuously (polling) or event-driven?
4. Is there a connection handshake on `5352` before bike data starts?
5. How does the bike authenticate the phone app?

---

## Repository Health

| Check | Status |
|-------|--------|
| CI (build) | ✅ Should pass — all code is syntactically correct Kotlin |
| Unit tests | ⚠️ No test cases written yet |
| Lint | ⚠️ Not yet run via CI lint step |
| No hardcoded secrets | ✅ Clean |
| Documentation | ✅ Current |

---

## Module Summary

| Module | Key Files | Status |
|--------|-----------|--------|
| `ble-core` | `BleConstants`, `RtrScanner`, `RtrGattManager`, models | Stable — Sprint 1 complete |
| `protocol` | `RawPacket`, `PacketLogger`, `PacketAnalyzer` (stub) | Sprint 3 partial |
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

## Last Updated

2026-08-08 — Sprint 3 main deliverables complete. Pending hardware capture session.
