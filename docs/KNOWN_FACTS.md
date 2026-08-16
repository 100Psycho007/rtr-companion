# KNOWN FACTS

> This document tracks everything we know about the TVS Apache RTR 310 BLE protocol.
> Facts are categorised strictly. **Never move an entry between categories without experimental evidence.**

---

## Confirmed

Facts in this section have been directly and experimentally verified.

### Device Identification
- **Device name prefix:** `TVSRTR310`
  - Observed device: `TVSRTR310FKB0925`
  - The suffix after `TVSRTR310` appears to be a model/unit identifier
  - Source: nRF Connect on OnePlus 12

### BLE Services
- **0x1800** — Generic Access service present
- **0x1801** — Generic Attribute service present
- **0x180A** — Device Information service present
- **5456534d-5647-5341-5342-454e544f5251** — Proprietary TVS service present
  - UUID is experimentally confirmed. Any textual interpretation is speculative and intentionally omitted until verified.
  - **Cross-reference confirmed:** TVS Jupiter uses the identical service UUID and characteristics (source: github.com/overclock98/JupiterRideCompanion). The RTR 310 and Jupiter share the same TVS SmartXonnect BLE protocol layer.

### BLE Characteristics (within TVS proprietary service)
- **`5352`** — WRITE property — direction: Phone → Bike
- **`5354`** — NOTIFY property — direction: Bike → Phone
  - Notifications successfully enabled via CCCD descriptor write

### Connection Behaviour
- Device accepts `connectGatt()` with `autoConnect=false`, `TRANSPORT_LE`
- Service discovery completes successfully after connection
- CCCD descriptor write to `5354` successfully enables notifications
- 600ms delay before `discoverServices()` is stable (per Android BLE recommendations)
- **Bike does NOT send live telemetry during normal connected+running state** — observed directly: app connected, CCCD write succeeded, but no live packets received during the active session. Only a shutdown burst was captured when the bike powered off. This behaviour is *consistent with* an authentication requirement but is not the same as confirming authentication is the cause.

### Confirmed Connection Sequence (from bluetooth_20260802_180111_21384.log)

The complete sequence from Oplus BT stack logs, **confirmed on RTR 310 with TVS Connect app**:

1. **ACL connected** — `handle=1`, `conn_interval=24` (~30ms, standard BLE)
2. **Bike sends SMP Security Request immediately** — `smp_data_received: cmd=SMP_OPCODE_SEC_REQ[0x0b]`, `auth_req=0xd`
3. **Phone sends `LE_START_ENCRYPTION(0x2019)` with LTK** — `use_stk:false` (existing bond, no re-pairing)
4. **Bike sends another Security Request** — `sec_req_act=BTM_BLE_SEC_REQ_ACT_DISCARD` (already encrypting, discarded)
5. **Encryption complete** — `bta_dm_encrypt_cback: Encrypted:T` ~280ms after connection, `key_size=16`, `sec_flags=0x3688`
6. **Service discovery from cache** — `oplus_bta_gattc_cache_load: load cache: /data/misc/bluetooth/gatt_cache_58d15afc222d` (no re-discovery)
7. **Services found** — handles: 0x1800@1, 0x1801@12, 0x180A@13, TVS proprietary@32
8. **Register for notifications** — `btif_gattc_reg_for_notification: char_handle=0x0022, desc_cccd_handle=0x0023` 
   - CHAR_NOTIFY handle = **0x0022** (decimal 34)
   - CCCD descriptor handle = **0x0023** (decimal 35)
9. **Writes begin** — `BTA_GATTC_API_WRITE_EVT` at ~2000ms intervals

**Critical finding: `auth_req=0xd` on the SMP Security Request**
- `0x0d` = `0b00001101` = Bonding(bits 0-1 = 01) + MITM(bit 2 = 1) + SC(bit 3 = 1)
- This means: **LE Secure Connections + MITM protection required**
- MITM + SC = **Numeric Comparison** or **Passkey Entry** pairing method (NOT Just Works)
- This confirms the passkey dialog seen in rtr_auth_logcat.txt

**Firmware version confirmed:**
- `btif_update_remote_version_property: [xx:xx:xx:xx:22:2d]:0xa,0xd,0x225`
- LMP version 0x0A = Bluetooth 5.1, manufacturer 0x000D = CSR/Qualcomm, subversion 0x0225
- **Confirmed: RTR 310 requires BLE SMP bonding on first connection**
  Source: `rtr_auth_logcat.txt` (2026-08-15) — `android.bluetooth.device.action.PAIRING_REQUEST`
  broadcast observed, triggering `com.android.settings.bluetooth.BluetoothPairingDialog`.
- **Pairing type: Passkey Display or Passkey Entry** — the system dialog was shown (rules out
  Just Works which is silent). Google Fast Pair logs `passkey=%s, variant=%s` with redacted values,
  confirming a passkey is involved. The bike has no keyboard so the bike displays the passkey and
  the user types it on the phone.
- **After first bond: all subsequent connections are silent** — the device appeared in
  `getBondedDevices: length=9` throughout the entire log. Every reconnect by nRF Connect
  went directly to `onClientConnectionState connected=true` without triggering the dialog.
- **Android handles bonding automatically** — `connectGatt()` triggers the OS pairing dialog;
  the app does not call `createBond()`. The GATT stack pauses during pairing and resumes
  automatically on `BOND_BONDED`.
- **Two-layer auth model confirmed:**
  1. OS layer (one-time): Standard BLE SMP bonding → system passkey dialog
  2. App layer (every session, HYPOTHESIS): `0x9A 0xF2` challenge-response — still unobserved on RTR 310

### Packet Structure (from RTR 310 shutdown capture, 2026-08-08 — corrected in Session 004)
- **Fixed length:** every inbound packet is exactly **20 bytes** (indices 0–19)
  - Corrected from "19 bytes" — a byte-count script on the raw capture confirms all 21
    packets are 20 bytes long.
- **Frame type byte:** `0x5A` = data frame, `0x5B` = control/null frame
- **Message ID:** byte 1 identifies the data type
- **Payload:** bytes 2–17 (16 bytes); `0xEA` = empty/null field
  - Corrected from "bytes 2–16" — byte 17 was assumed reserved/fixed but changes
    (`0xEA`→`0x00`) between the two `0x5F` variants, so it's payload, not reserved.
- **Checksum (candidate):** byte 18
  - Corrected from "byte 17" — script comparison of the two `0x11` variants shows
    the byte that moves in lockstep with the changed data byte is index 18, not 17.
- **Terminator:** byte 19 = always `0xFF`
- **Null field value:** `0xEA` for inbound packets

### Checksum Algorithm (session 004 + integrity correction pass 2026-08-08)

**Formula confirmed for messages 0x10, 0x11:**
`checksum = (C − sum(B0..B17)) mod 256` where C is a per-message-type constant.

Verification (from `docs/protocol/capture-20260808-150945.md`):
- 0x11 pair: B10 changes by −0x20, B18 changes by −0x20 → confirms `(C − sum)` where C=0xC3
- 0x10: single value only (3 identical packets) → C=0x31 derived, consistent

**Per-message-type constants C (derived from capture):**

| Msg ID | C value | Evidence strength |
|--------|---------|------------------|
| 0x10 | 0xE5 | Single packet value; consistent |
| 0x11 | 0xD5 | **Cross-verified against 2 variants** |
| 0x12 | 0xF1 | Single packet value; consistent |
| 0x5F | UNKNOWN | **UNRESOLVED** — formula fails |
| 0x7D | 0x29 | Single packet value; consistent |
| 0x42 | 0xF6 | Single packet value; consistent |

**IMPORTANT:** C is NOT a universal constant (0xFF as Jupiter uses). Each message ID
appears to have its own C. Jupiter's formula `255 − (sum mod 256)` does **not**
reproduce RTR 310 byte 18 values for any of the observed message types.

**0x5F checksum — UNRESOLVED:** Does not fit `(C − sum) mod 256` with any consistent C.
Brute-forced all (start, end) sum ranges × constants 0–255. Tested XOR and 5 CRC-8
polynomials. Zero formulas matched. Likely has a hidden input (frame counter B7 = 0x1E/0x1F
may feed the checksum separately). Needs live-ride capture to resolve.

### Message Types Observed (RTR 310 shutdown capture)
- `0x5A 0x10` — present, static across capture
- `0x5A 0x11` — present, near-static (1 byte changes between first and subsequent packets)
- `0x5A 0x12` — present, static
- `0x5A 0x5F` — present, **dynamic** — multiple bytes change between occurrences
- `0x5A 0x7D` — present, fully packed (zero `0xEA` bytes), static
- `0x5B 0x42` — present, almost all `0xEA`, static

### Official App Timing (from rtr_auth_logcat.txt, 2026-08-15)
- **Ping interval: ~200ms** — TVS Connect (`com.tvsm.connect`, `ApacheMobileToCluster.sendMobileData()`)
  fires every ~190–200ms, not 1 second as documented in Jupiter RE.
  Observed timestamps (18:03:35.589, .782, .992, 36.578, .786, .990) confirm ~5Hz rate.
- **Class name `ApacheMobileToCluster`** confirms the official app has RTR 310-specific (Apache)
  data sender code, distinct from Jupiter. Protocol may diverge more than expected.
- Service UUID, write/notify characteristics: **identical**
- Packet framing (`0x5A`/`0x5B` start, `0xFF` end): **identical**
- Message ID `0x10` (odometer/fuel): **present on both**
- Message ID `0x11` (service reminder): **present on both**
- Authentication handshake required: **confirmed on Jupiter, strongly suspected on RTR 310**

---

## Hypothesis

Facts in this section are plausible based on available evidence but not yet experimentally confirmed on the RTR 310.

### Authentication Handshake
- **Authentication is required before live telemetry starts** — confirmed on Jupiter; STRONGLY SUSPECTED on RTR 310 based on observed behaviour (no live packets during connected+running state), but not confirmed independently
- **Handshake sequence (Jupiter documented, likely same on RTR 310):**
  1. Bike sends `0x9A 0xF2` + 16 random challenge bytes via NOTIFY
  2. Phone encrypts challenge with AES-128-CTR using shared key
  3. Phone sends `0x9A 0xF1` + encrypted response to WRITE characteristic
- **Jupiter AES key:** `7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4` — UNVERIFIED on RTR 310
- **NO auth packet (`0x9A 0xF2`) was observed** in the 2026-08-08 capture (ignition OFF shutdown burst)

### Keep-Alive Ping
- Phone must send `0x5B 0x4A` ping packet continuously to maintain connection and update cluster display
- Ping carries: phone signal bars, battery bars, time, date, temperature, network type
- `Find Me` flag in byte 17 of ping triggers indicator flash + beep

### Message 0x10 Fields (from Jupiter RE, same message ID)
- Bytes 3–5: Odometer (UInt24, divide by 10 for km)
- Byte 6: Fuel level (lower nibble = bars 0–5, upper nibble = reserve flag)
- Byte 13: Call command from cluster button

### Message 0x5F — Live Telemetry
- Two variants observed in RTR 310 capture with bytes 8–13 changing
- Byte 7 increments by 1 between variants (0x1E → 0x1F) — likely frame counter
- Almost certainly carries live sensor data (speed, RPM, or similar)
- Not yet decoded — requires capture during active ride with ignition ON

### Message 0x7D
- Fully packed (no `0xEA` null bytes), identical across all 3 occurrences
- Likely device identity, firmware version, or static configuration data
- Not present in Jupiter protocol docs — may be RTR 310 specific

### Ride vs Parked State
- Ignition OFF: bike sends only shutdown/keepalive packets
- Ignition ON: bike sends active telemetry (`0x10`, `0x11`, `0x18`, `0x19` per Jupiter RE)
- Detecting any of `0x10`/`0x11`/`0x18`/`0x19` can distinguish ride from parked state

### Protocol Structure
- **Packet-based multiplexed protocol** — single NOTIFY characteristic carries all data types via message ID byte
- `0xEA` is the explicit null/empty value for inbound fields

---

## Rejected

Facts in this section have been tested and found to be false.

- **"Bike sends live telemetry immediately on connection without handshake"** — REJECTED. Bike connected successfully and CCCD write succeeded, but no live packets were received during active ride. Only shutdown burst received. Authentication handshake is required.

---

## Questions to Resolve

1. Is the AES-128-CTR key the same on RTR 310 as Jupiter?
   → Get btsnoop log: enable HCI snoop, connect TVS Connect to RTR 310, capture
2. Does the RTR 310 send `0x9A 0xF2` challenge immediately after CCCD enable?
3. What data does `0x5F` carry? (observed with changing bytes — likely live sensors)
4. What does `0x7D` carry? (fully packed, static, not in Jupiter docs)
5. What does `0x12` carry? (static, not in Jupiter docs)
6. What is the packet rate during active ride (ignition ON)?
7. Does RTR 310 support navigation HUD (`0x4E`/`0x4F`/`0x50`)?
8. What is the notification rate for the `0x5F` packet?

---

## Sources

| Source | Type | Date |
|--------|------|------|
| nRF Connect on OnePlus 12 | Direct hardware observation | Pre-2026-08 |
| RTR 310 packet capture (shutdown burst) | Direct hardware observation | 2026-08-08 |
| JupiterRideCompanion RE report (github.com/overclock98/JupiterRideCompanion) | Cross-reference — same protocol | 2026-08-08 |
| RTR 310 owner's manual | Documentation reference | Pre-2026-08 |
| Android HCI log (.cfa format, not yet decoded) | Pending — OnePlus proprietary format | 2026-08-02 |
