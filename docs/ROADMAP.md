# Roadmap

> RTR Companion development roadmap. Updated as sprints complete.
> See `PROJECT_STATE.md` for current sprint details.

---

## Current Sprint: Sprint 3 — Export & Analysis Foundation 🔄 In Progress

| Task | Status |
|------|--------|
| Packet export (share as `.txt`) | ✅ Done |
| `PacketAnalyzer` skeleton | ✅ Done |
| All documentation caught up | ✅ Done |
| Real hardware capture session | ⏳ Pending |
| Update `docs/BLE-Protocol.md` from captures | ⏳ Pending |

---

## Sprint 0 — Environment & Discovery ✅ Done

**Goal:** Working project structure and initial BLE discovery.

- [x] Gradle Kotlin DSL multi-module project (`app`, `ble-core`, `protocol`)
- [x] GitHub Actions CI (build + test)
- [x] Timber logging
- [x] Material 3 theme with RTR brand colours
- [x] nRF Connect BLE investigation — service and characteristic discovery

---

## Sprint 1 — Connect & Listen ✅ Done

**Goal:** App can scan, connect, and enable notifications. Read-only.

- [x] BLE runtime permissions (API 31+ and ≤ 30)
- [x] `RtrScanner` — scan with 15s timeout, deduplicated results, `StateFlow`
- [x] `RtrGattManager` — GATT connect, service discovery, notification enable
- [x] `ScanScreen` — scan controls + device list
- [x] `PermissionScreen` — permission denial recovery
- [x] Connection lifecycle state machine (`ConnectionState`)

---

## Sprint 2 — Capture ✅ Done

**Goal:** All notification packets logged and visible in the UI.

- [x] `RawPacket` — raw byte container with hex display
- [x] `PacketLogger` — 500-entry ring buffer
- [x] `MainViewModel` — GATT → PacketLogger pipeline
- [x] `PacketLogScreen` — live auto-scrolling hex log
- [x] App state router (`RtrCompanionApp`)

---

## Sprint 3 — Export & Analysis Foundation 🔄 In Progress

**Goal:** Users can export captures. Analysis infrastructure laid out.

- [x] Packet export — share captured session as `.txt` file
- [x] `PacketAnalyzer` skeleton — interface defined, no decoding yet
- [x] Documentation catch-up — PROJECT_STATE, KNOWN_FACTS, ROADMAP, sessions, ADRs
- [x] Architecture review — BLE write audit, naming consistency, UUID docs
- [ ] Real hardware capture session — connect to bike, collect packets
- [ ] Save capture to `captures/` directory
- [ ] Protocol documentation — update `docs/BLE-Protocol.md` from capture data
- [ ] `docs/sessions/Session-004.md` — log the hardware session

---

## Sprint 4 — UI Polish & Navigation 🔜 Planned

**Goal:** Polished app with proper navigation and more screens.

- [ ] Introduce `NavController` / Navigation Compose
- [ ] Connection screen separated from scan screen
- [ ] Settings screen (scan timeout, log buffer size)
- [ ] About screen (version, open-source links)
- [ ] Handle Bluetooth disabled state gracefully
- [ ] Handle location permission requirement (Android ≤ 11)
- [ ] Landscape layout support

---

## Sprint 5 — Protocol Parsing 🔜 Planned

**Goal:** Begin interpreting known packet types.

> **Prerequisite:** Sufficient capture data to reverse-engineer at least one message type.

- [ ] Define `ParsedPacket` sealed class subtypes for known message types
- [ ] Implement parser for first confirmed message type
- [ ] Add typed packet display to `PacketLogScreen`
- [ ] Document all decoded fields in `docs/BLE-Protocol.md`
- [ ] Update `KNOWN_FACTS.md` — promote confirmed hypotheses

---

## Sprint 6 — Feature Implementation 🔜 Planned

**Goal:** Implement first real app features based on decoded protocol.

> **Prerequisite:** Sprint 5 complete with ≥1 confirmed decoded message type.

- [ ] Display live speed / RPM (if decodable)
- [ ] Display ride mode
- [ ] Display trip data
- [ ] Notification for low fuel / TPMS alerts (if supported)

---

## Sprint 7 — SDK Stabilisation & Open Beta 🔜 Planned

**Goal:** Stable SDK, comprehensive documentation, public release.

- [ ] SDK public API finalised and documented with full KDoc
- [ ] Unit tests for protocol parsing
- [ ] Integration test for BLE scan (mocked)
- [ ] Release to Google Play open beta
- [ ] SDK published as Maven artifact

---

## Long-Term Goals (Post-Beta)

- WRITE command support (requires full protocol documentation + safety review per `docs/SECURITY.md`)
- Support for additional TVS SmartXonnect bikes (Apache 200/300 series)
- Ride history recording and replay
- Geofencing / stolen vehicle alerts
- Desktop companion (macOS/Windows) via Bluetooth
- Protocol capture tooling for contributors

---

## Version History

| Version | Sprint | Date | Notes |
|---------|--------|------|-------|
| 0.1.0 | Sprint 0–3 | 2026-08-08 | Initial release candidate — internal use only |

---

## What Will Never Be Done

- Sending undocumented write commands to the bike
- Publishing fake or fabricated packet documentation
- Reverse-engineering the TVS cloud API (out of scope)
- Moving hypotheses to Confirmed in `KNOWN_FACTS.md` without experimental evidence
