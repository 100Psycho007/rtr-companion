# Session 005 — Protocol Integrity Correction

## Goal

Correct all unverified assumptions that had been promoted to "Confirmed" status.
Re-analyse the capture byte-for-byte. Disable unverified writes. Restore full
research integrity — the raw capture is the only source of truth.

---

## Problems Found (Pre-Correction)

1. `docs/BLE-Protocol.md` still stated **19 bytes** for inbound packets — incorrect.
   Session 004 corrected KNOWN_FACTS.md but BLE-Protocol.md was not updated.
2. KNOWN_FACTS.md Confirmed section contained Jupiter-derived checksum formula
   (`255 − sum mod 256`) presented without qualification that it fails on RTR 310.
3. `HandshakeManager` and `PingPacketBuilder` were wired into `RtrGattManager` with
   **no mode gate** — the Jupiter AES key would have been sent to the RTR 310
   automatically on the next session if a challenge packet arrived.
4. `BLE_WRITE_AUDIT.md` marked the handshake and ping writes as "SAFE — documented"
   without flagging that the key and format were UNVERIFIED on RTR 310.
5. No separate Jupiter cross-reference document — Jupiter observations were mixed
   into Confirmed and Hypothesis sections without clear labelling.
6. No `PROTOCOL_STATUS.md` to track per-packet-type confidence levels.
7. `docs/KNOWN_FACTS.md` stated "Bike does NOT send telemetry without auth handshake"
   as Confirmed — the correct observed fact is "bike did not send live telemetry during
   the connected session"; the auth requirement is an explanation, not a confirmed cause.
8. `local.properties` was already in `.gitignore` (correct). `bug report hci/` was
   not ignored — added to `.gitignore`.
9. The checksum formula had not been independently cross-checked against all 6 message
   types observed in the capture.

---

## Work Completed

### Byte-by-Byte Re-Analysis (`docs/protocol/capture-20260808-150945.md`)

Re-analysed all 21 packets from `captures/rtr-capture-20260808-150945.txt`.

Key findings (correcting or extending Session 004):

- **Frame size confirmed 20 bytes** — all 21 packets.
- **Checksum formula refined:** `(C − sum(B0..B17)) mod 256` where C is per-message-type.
  Session 004 identified this for 0x11 only. This session derived C for all 6 types:

  | Msg ID | C value | Evidence |
  |--------|---------|---------|
  | 0x10   | 0x31    | Consistent with 3 identical packets |
  | 0x11   | 0xC3    | Cross-verified against 2 variants ✓ |
  | 0x12   | 0x0B    | Single value (3 identical packets) |
  | 0x5F   | UNKNOWN | Formula still fails — UNRESOLVED |
  | 0x7D   | 0x99    | Single value (3 identical packets) |
  | 0x42   | 0x34    | Single value (3 identical packets) |

- **Jupiter formula `255 − sum mod 256` (C=0xFF) does NOT match any RTR 310 message type.**
- **0x5F checksum remains UNRESOLVED.** C1→C2 checksum delta (−0x86) does not match
  the expected −Δsum. Brute-force and CRC-8 tests from Session 004 still hold.
  Needs live-ride capture.
- **0x10 candidate odometer:** bytes 4–6 = F6 24 DB = 0xF624DB / 10 = 1,606,235 km —
  impossible value. Jupiter's byte numbering may be 1-indexed vs 0-indexed, or the
  formula/offset is wrong. **Do not decode 0x10 payload until confirmed with known odometer.**

### New Documents Created

- `docs/protocol/capture-20260808-150945.md` — full byte-by-byte analysis
- `docs/protocol/PROTOCOL_STATUS.md` — per-packet-type confidence tracker
- `docs/research/JUPITER_CROSS_REFERENCE.md` — clear separation of Jupiter vs RTR 310

### Documents Updated

- `docs/KNOWN_FACTS.md`
  - Checksum section rewritten: C is per-message-type, not 0xFF; constants tabulated
  - "Bike does NOT send telemetry without auth" corrected to observed fact
  - Auth handshake hypothesis section updated to note it was never observed on RTR 310
- `docs/BLE-Protocol.md`
  - 19-byte claim corrected to 20 bytes
  - Checksum formula corrected (no longer claims Jupiter formula)
  - PASSIVE mode note added to outbound section
- `docs/security/BLE_WRITE_AUDIT.md`
  - Default mode set to PASSIVE
  - CHAR_WRITE writes marked DISABLED (EXPERIMENTAL only)
  - Added UI warning requirement

### Code Changes

- `ble-core/ProtocolMode.kt` — new enum: `PASSIVE` (default) / `EXPERIMENTAL`
- `ble-core/connection/RtrGattManager.kt`
  - Added `protocolMode: ProtocolMode = ProtocolMode.PASSIVE` constructor parameter
  - PASSIVE: no writes to CHAR_WRITE; logs warning on connect
  - EXPERIMENTAL: handshake + ping enabled (existing behaviour, now gated)
  - `writeToCharWrite()` has hard no-op guard in PASSIVE mode
- `ble-core/auth/HandshakeManager.kt` — updated KDoc: clearly UNVERIFIED on RTR 310
- `ble-core/ping/PingPacketBuilder.kt` — updated KDoc: HYPOTHESIS status, UNVERIFIED
- `ble-core/BleConstants.kt` — updated auth constant comments to note unverified status
- `.gitignore` — added `bug report hci/` to prevent accidental commit of system dump

### Not Changed (Intentional)

- `HandshakeManager` and `PingPacketBuilder` remain in codebase — not deleted
- CCCD notification enable remains active in all modes
- Architecture (app / ble-core / protocol modules) unchanged
- All existing capture data and docs preserved

---

## PASSIVE Mode Behaviour (Current Default)

After this session the app does the following on connect:
1. Scan for TVSRTR310* devices
2. Connect via GATT
3. Discover services
4. Enable CCCD notifications on CHAR_NOTIFY (0x5354)
5. Log: `⚠️ Experimental protocol writes disabled (PASSIVE mode). ...`
6. Capture all incoming packets — emit to packetFlow → PacketLogger → UI

**Nothing is written to CHAR_WRITE (0x5352).**

---

## Remaining Research Questions

1. Checksum formula for 0x5F — requires live-ride capture
2. AES key correct on RTR 310? — requires btsnoop HCI log from TVS Connect
3. Does RTR 310 send 0x9A/0xF2 auth challenge? — not observed yet
4. What do 0x7D and 0x12 carry?
5. What live data does 0x5F carry?
6. Is 0x10 B4–B6 actually the odometer? (value doesn't match expected range)
7. Are 0x18/0x19 present on RTR 310?
8. Is ping 0x4A required?

---

## Files Changed

| File | Change |
|------|--------|
| `docs/protocol/capture-20260808-150945.md` | CREATED — full byte analysis |
| `docs/protocol/PROTOCOL_STATUS.md` | CREATED — confidence tracker |
| `docs/research/JUPITER_CROSS_REFERENCE.md` | CREATED — Jupiter vs RTR 310 separation |
| `docs/sessions/Session-005.md` | CREATED — this file |
| `docs/KNOWN_FACTS.md` | UPDATED — checksum, auth claim corrections |
| `docs/BLE-Protocol.md` | UPDATED — 19→20 bytes, checksum formula corrected |
| `docs/security/BLE_WRITE_AUDIT.md` | UPDATED — PASSIVE default, writes disabled |
| `ble-core/ProtocolMode.kt` | CREATED |
| `ble-core/connection/RtrGattManager.kt` | UPDATED — ProtocolMode gate |
| `ble-core/auth/HandshakeManager.kt` | UPDATED — KDoc corrected |
| `ble-core/ping/PingPacketBuilder.kt` | UPDATED — KDoc corrected |
| `ble-core/BleConstants.kt` | UPDATED — auth constant comments |
| `.gitignore` | UPDATED — bug report hci/ excluded |
| `PROJECT_STATE.md` | UPDATED |
