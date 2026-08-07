# Roadmap

> RTR Companion development roadmap. Updated as sprints complete.
> See `PROJECT_STATE.md` for current sprint details.

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

- [ ] Packet export — share captured session as `.txt` file
- [ ] `PacketAnalyzer` skeleton — interface defined, no decoding yet
- [ ] Real hardware capture session — connect to bike, collect packets
- [ ] `captures/` — add at least one real session file
- [ ] Protocol documentation — update `docs/BLE-Protocol.md` from capture data
- [ ] `docs/sessions/Session-003.md` — log the hardware session

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

- [ ] Define `ParsedPacket` sealed class for known message types
- [ ] Implement parser for first confirmed message type
- [ ] Add typed packet display to `PacketLogScreen`
- [ ] Document all decoded fields in `docs/BLE-Protocol.md`
- [ ] Update `KNOWN_FACTS.md` — move hypotheses to confirmed

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

- [ ] SDK public API finalized and documented
- [ ] Javadoc / KDoc for all public APIs
- [ ] Unit tests for protocol parsing
- [ ] Integration test for BLE scan (mocked)
- [ ] Release to Google Play open beta
- [ ] SDK published as Maven artifact

---

## Long-Term (Post-Beta)

- WRITE command support (requires full protocol documentation + safety review)
- Support for additional TVS SmartXonnect bikes (Apache 200/300 series)
- Ride history recording and replay
- Geofencing / stolen vehicle alerts
- Desktop companion (macOS/Windows) via Bluetooth
- Protocol capture tooling for contributors

---

## What Will Never Be Done

- Sending undocumented write commands to the bike
- Publishing fake or fabricated packet documentation
- Reverse-engineering the TVS cloud API (out of scope)
