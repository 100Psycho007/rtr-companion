# Protocol Analysis — btsnoop HCI Capture 2026-08-16

## Session Summary

| Field | Value |
|-------|-------|
| Date | 2026-08-16 16:46 PM |
| Device | LXX505 (Lava phone — bugreport filename) |
| App | `com.tvsm.connect` — focused on `AddVehicleByVinActivity` |
| Capture method | Android HCI snoop log (btsnoop v1, datalink 0x3EA Linux Monitor) |
| File | `bt-logs/btsnoop_hci.log` (95,639 bytes) |
| Total RX notifications | 733 |
| Total TX writes | 94 |
| Ignition state | **PARKED** (same message types as shutdown capture — no 0x18/0x19) |

**Key difference from previous capture:** This session has **351× 0x10** and **351× 0x12** versus 3 of each before. Much richer statistics for checksum analysis and field tracking.

**New discovery:** The phone was on the `AddVehicleByVinActivity` screen — registering a new vehicle by VIN. This is why 0x52 (FIRST_CAPTURED_WRITE) appears — it is the **initial device registration/identification packet**, not an authentication packet.

---

## WRITE Characteristic Handle — CORRECTED

**Previous value in docs: `0x0022`** — WRONG.  
**Correct value: `0x0026`**

The HCI capture shows all 94 phone→bike writes going to handle `0x0026`.  
Handle `0x0022` is CHAR_NOTIFY (bike→phone). Handle `0x0026` is CHAR_WRITE (phone→bike).

This does not affect the UUID values (`5352` write, `5354` notify) — only the GATT handle numbers matter inside the BLE stack and Android manages them automatically.

---

## Packet Encoding — CONFIRMED: XOR 0xEA

**All packets (both inbound and outbound) have their payload XOR-encoded with `0xEA`.**

- `0xEA` in the raw byte = `0x00` decoded (empty/null field)
- The null marker `0xEA` = `0x00` after decode

This is consistent with the inbound `0xEA` = null field value previously documented.

---

## Ping Packet Decode (MSG_PING = 0x4A)

All ping bytes are XOR-encoded with `0xEA`. Decode first by XORing each byte with `0xEA`.

### Sample decoded pings (time: 2026-08-16, 16:46 PM)

| Raw hex | s | Decoded time | Signal | Bat | Net | Find Me |
|---------|---|-------------|--------|-----|-----|---------|
| `5B 4A DF 92 EA EA EE C4 CD EB EA EF FA E2 F0 EA EA EA EA FF` | 39 | 4:46:39 PM 16/8/2026 | 3 | 5 | 0x05 | 0 |
| `5B 4A DF 92 EA EA EE C4 C3 EB EA EF FA E2 F0 EA EA EA EA FF` | 41 | 4:46:41 PM 16/8/2026 | 3 | 5 | 0x05 | 0 |
| `5B 4A DF 92 EA EA EE C4 C1 EB EA EF FA E2 F0 EA EA EA EA FF` | 43 | 4:46:43 PM 16/8/2026 | 3 | 5 | 0x05 | 0 |

**Ping interval confirmed: ~2 seconds** — matches `PING_INTERVAL_MS = 2000` in `BleConstants.kt`.

### Decoded ping fields (after XOR 0xEA):

| Byte | Field | Decoded value |
|------|-------|---------------|
| 0 | `0xB1` XOR `0xEA` = `0x5B` | FRAME_CONTROL |
| 1 | `0xA0` XOR `0xEA` = `0x4A` | MSG_PING |
| 2 | `0x35` = upper nibble=3 (signal), lower nibble=5 (battery) | sig=3, bat=5 |
| 3 | `0x78` = padding | |
| 4 | `0x00` → 0°C + 40 = `0x28` encoded | temp field (0x00 = not populated) |
| 5 | `0x00` | padding |
| 6 | `0x04` | hour = 4 (12-hour) |
| 7 | `0x2E` = 46 | minute = 46 |
| 8 | `0x27/29/2B...` = 39/41/43... | second (incrementing every ~2s) |
| 9 | `0x01` | PM |
| 10 | `0x00` | padding |
| 11 | `0x05` | network = 5G or network code 5 |
| 12 | `0x10` = 16 | day = 16 |
| 13 | `0x08` = 8 | month = August |
| 14 | `0x1A` = 26 | year mod 100 = 2026 |
| 15-17 | `0x00` | padding / find me = 0 |
| 18 | `0x00` | checksum (after decode = 0x00) |
| 19 | `0x15` XOR `0xEA` = `0xFF` | PACKET_END |

**Checksum finding:** After XOR-decoding all bytes, the checksum byte (18) is `0x00` in all pings observed. This suggests the ping checksum may be computed differently or the outbound checksum is simply `0x00` for a specific reason. Needs further analysis.

---

## FIRST_CAPTURED_WRITE — Decoded (5B 52)

Raw: `5B 52 AE 82 8B 84 9F 99 82 CA A1 CA B9 EA EA EA EA EA EA FF`

XOR B2..B17 with `0xEA`:

| Decoded hex | ASCII |
|-------------|-------|
| `44 68 61 6E 75 73 68 20 4B 20 53 00 00 00 00 00` | `[user display name]` |

**This is the phone user's registered display name in the TVS Connect app.**

This packet is the **user identification / first-connection registration packet**, not an authentication packet and not a session key.

- Byte 0: `0x5B` = CTRL frame
- Byte 1: `0x52` = MSG_USER_ID (outbound only — phone→bike only)
- Bytes 2–12: Username XOR-encoded with `0xEA` — `"Dhanush K S"`
- Bytes 13–17: `0xEA` = null padding
- Byte 18: `0xEA` = `0x00` after decode (checksum field = 0 or no checksum)
- Byte 19: `0xFF` = PACKET_END

**C constant:** `C = (0xEA + sum_mod256) % 256 = 0x10`

**Updated `FIRST_CAPTURED_WRITE` label:** `MSG_USER_ID (0x52)` — sends the registered user's display name to the bike cluster.

---

## Message 5B 43 — Decoded

Raw: `5B 43 BE 8F 80 8B 99 EA EA EA EA EA EA EA EA EA EA EA EA FF`

XOR B2..B6 with `0xEA`: `54 65 6A 61 73` = **`Tejas`**

This is the **vehicle name** (as configured in TVS Connect app). The bike's registered display name is `"Tejas"` — likely what shows on the cluster.

- Byte 1: `0x43` = MSG_VEHICLE_NAME (outbound only)
- Bytes 2–6: Vehicle name XOR-encoded = `"Tejas"`
- C constant: `0x87`

---

## Message 5B 9C — Decoded

Raw: `5B 9C E3 EA EA EA EA EA EA EA EA EA EA EA EA EA EA EA 15 FF`

- XOR B2: `E3 XOR EA = 09` = numeric value 9 (possibly user count or session index)
- C constant: `0xF6`
- Purpose: unknown — possibly session/slot selection

---

## Message 5A 4E — Navigation (CONFIRMED IN CAPTURE)

Raw: `5A 4E EA F1 EA E9 EA EE A6 E9 E8 15 EA EA EE EA EA EA 0B FF`  
XOR decode: `00 1B 00 03 00 04 4C 03 02 FF 00 00 04 00 00 00`

Empty variant: `5A 4E EA EA EA EA EA EA EA EA EA EA EA EA EA EA EA EA BD FF` (all zeros after decode)

**Navigation data is confirmed present in this session.** Field mapping (from APK `BleNavigationSendData`):

| XOR-decoded byte | Position | Probable field |
|-----------------|----------|----------------|
| `0x1B` = 27 | B3 | distance to next turn (meters × some factor) |
| `0x03` | B5 | turn instruction type |
| `0x4C` = 76 | B7 | road type or speed limit |
| `0x03` | B8 | lanes or road segment |
| `0x02` | B9 | navigation state (active) |
| `0xFF` = 255 | B10 | possibly total distance high byte |
| `0x04` | B14 | direction indicator |

Navigation was active during this session — the 0x4E burst confirms it.

---

## Updated Checksum Constants (from 733-packet capture)

Formula: `checksum = (C − sum(B0..B17)) mod 256`  
Derivation: `C = (checksum_byte + sum_mod_256) mod 256`

| Msg ID | Old C (wrong) | **New C (correct)** | Sample count | Unique packets | Consistent? |
|--------|--------------|-------------------|-------------|----------------|------------|
| `0x10` | 0xE5 | **0x49** | 351 | 3 | ✅ Yes |
| `0x11` | 0xD5 | **0xDD** | 5 | 1 | ✅ Yes |
| `0x12` | 0xF1 | **0x59** | 351 | 1 | ✅ Yes |
| `0x42` | 0xF6 | **0x82** | 5 | 1 | ✅ Yes |
| `0x7D` | 0x29 | **0x29** | 5 | 1 | ✅ Same |
| `0x5F` | UNKNOWN | **UNRESOLVED** | 16 | 2 | ❌ Two C values |

**0x7D is the only constant that was already correct.**  
The August 2026-08-08 capture yielded wrong values for 0x10, 0x11, 0x12, 0x42 — likely due to single-sample noise. The 351-sample dataset here is authoritative.

### 0x5F Checksum — Analysis

The two alternating 0x5F variants:

| fcnt (B7) | chk (B18) | sum_mod256 | C |
|-----------|-----------|------------|---|
| `0x1E` | `0xC2` | 51 | **0xF5** |
| `0x1F` | `0x75` | 156 | **0x11** |

Delta: fcnt changes by +1 (0x1E→0x1F), sum_mod changes by +105, C changes by +28.  
There is no single constant C — the checksum depends on the frame counter at B7.  
**Status: UNRESOLVED.** The formula likely includes B7 as an input. Needs more variants with different B7 values (live-ride ignition-ON capture).

---

## Confirmed Message Type Summary (this capture)

| ID | Dir | Count | Label | New info |
|----|-----|-------|-------|----------|
| `0x10` | RX | 351 | Odometer/Fuel | Large sample — C=0x49 confirmed |
| `0x11` | RX | 5 | Service/State | C=0xDD confirmed |
| `0x12` | RX | 351 | SpeedOMeter3 | C=0x59 confirmed |
| `0x42` | RX | 5 | Heartbeat | C=0x82 confirmed |
| `0x5F` | RX | 16 | Live Telemetry | 2 variants, checksum UNRESOLVED |
| `0x7D` | RX | 5 | Device Identity (VIN) | C=0x29 confirmed |
| `0x4A` | TX | 92 | Ping / Mobile Data | XOR 0xEA encoding confirmed, ~2s interval |
| `0x52` | TX | 1 | User ID (name) | **NEW** — user's registered display name |
| `0x43` | TX | 6 | Vehicle Name | **NEW** — vehicle display name (5 chars) |
| `0x9C` | TX | 1 | Unknown (session?) | **NEW** |
| `0x4E` | TX | 16 | Navigation | **NEW** — confirmed active |

---

## No Authentication Challenge Observed

No `0x9A 0xF2` challenge packet was observed in either capture session.

**Updated hypothesis:** The RTR 310 may NOT require an application-level AES handshake in the same way as Jupiter. The connection sequence is:
1. OS-level BLE bonding (passkey, first time only)
2. `0x52` user ID write
3. `0x43` vehicle name write
4. `0x4A` ping loop begins
5. Bike streams telemetry immediately

This would mean **no AES key is needed** — the "authentication" is just the user ID + vehicle name registration, both XOR-encoded with `0xEA`.

**This is a significant finding.** If confirmed, the app can connect and receive live telemetry without any cryptographic handshake.

---

## Open Questions

1. **Is telemetry live?** This is still a parked/ignition-OFF session. Need ignition-ON capture to see 0x18/0x19 speed/RPM data.
2. **0x5F checksum** — needs more frame counter variants to solve.
3. **Is 0x52 mandatory?** Does the bike refuse pings without receiving 0x52 first?
4. **0x9C meaning** — single occurrence, purpose unknown.
5. **Navigation field mapping** — the 0x4E XOR-decoded values need mapping against APK `ApacheClusterNavigationData`.
6. **Network type 0x05** — what does value 5 mean? (0x04 was expected for LTE)

---

## Sources

| File | Type |
|------|------|
| `bt-logs/btsnoop_hci.log` | HCI snoop log (95KB, 733 RX + 94 TX ATT packets) |
| `bt-logs/bugreport-LXX505-UP1A.231005.007-2026-08-16-16-57-25.txt` | Android bugreport (94MB) |
| `captures/rtr-capture-20260816-btsnoop.txt` | Extracted 733 RX notifications (this session) |
