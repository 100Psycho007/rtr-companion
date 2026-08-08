# BLE Protocol — TVS SmartXonnect

## Device

- Device name prefix observed: `TVSRTR310` (e.g. `TVSRTR310FKB0925`)

---

## BLE Identifiers

### Standard Services
- `0x1800` – Generic Access
- `0x1801` – Generic Attribute
- `0x180A` – Device Information

### Proprietary TVS Service
- `5456534D-5647-5341-5342-454E544F5251`
  - UUID is experimentally confirmed. Any textual interpretation is speculative and intentionally omitted until verified.

### Characteristics
- `00005352-0000-1000-8000-00805f9b34fb` — **WRITE** — Phone → Bike
- `00005354-0000-1000-8000-00805f9b34fb` — **NOTIFY** — Bike → Phone

> **Protocol Note:** The RTR 310 uses the same TVS SmartXonnect BLE protocol as the
> TVS Jupiter, confirmed by matching service/characteristic UUIDs and identical packet
> structure observed in cross-referencing with the JupiterRideCompanion project
> (github.com/overclock98/JupiterRideCompanion).

---

## Packet Structure (All Messages)

Every packet is **exactly 20 bytes** (confirmed from RTR 310 capture 2026-08-08 —
all 21 packets are 20 bytes). Previous documentation that stated 19 bytes was incorrect
and has been corrected.

### 20-byte inbound (Bike → Phone, NOTIFY):
```
Byte  0    : Frame type   — 0x5A (data) or 0x5B (control/null)
Byte  1    : Message ID   — identifies the data type
Bytes 2–17 : Payload      — 16 bytes; 0xEA = empty/null field
Byte  18   : Checksum     — formula: (C − sum(B0..B17)) mod 256,
             where C is a per-message-type constant (NOT Jupiter's formula 255 − sum)
             Confirmed for 0x10 (C=0x31), 0x11 (C=0xC3).
             UNRESOLVED for 0x5F.
Byte  19   : Terminator   — always 0xFF
```

### 20-byte outbound (Phone → Bike, WRITE):
```
Byte  0    : Start byte   — 0x5A or 0x5B
Byte  1    : Data ID
Bytes 2–17 : Payload      — 16 bytes; 0x00 = empty field (outbound uses 0x00 not 0xEA)
Byte  18   : Checksum     — formula from Jupiter RE: 255 − (sum(B0..B17) mod 256)
             STATUS: HYPOTHESIS — not verified on RTR 310. Outbound writes are DISABLED
             in PASSIVE mode.
Byte  19   : End byte     — always 0xFF
```

### Null/Empty field value
- **Inbound (Bike → Phone):** `0xEA` = empty/unused field
- **Outbound (Phone → Bike):** `0x00` = empty/unused field

---

## Authentication Handshake (Required for Live Telemetry)

**Source:** JupiterRideCompanion RE report (same protocol, same service UUID).
**Status:** Hypothesis — key may differ on RTR 310. Must be verified with btsnoop log.

### Flow
```
1. Phone connects and enables NOTIFY on 0x5354
2. Bike sends challenge: 0x9A 0xF2 + 16 random bytes
3. Phone encrypts the 16 bytes using AES-128-CTR, no padding
   Key (Jupiter): 7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4
4. Phone sends response: 0x9A 0xF1 + encrypted bytes (to WRITE char 0x5352)
5. Bike acknowledges and begins streaming telemetry
```

**Without this handshake:** bike accepts the BLE connection and CCCD notification
enable, but sends NO telemetry during normal operation. Only shutdown/power-off
sequences are broadcast unconditionally.

### Keep-Alive (Ping) Packet — Data ID 0x4A
Must be sent continuously while connected to keep the connection alive and update
the cluster display with phone status.

| Byte | Field | Description |
|------|-------|-------------|
| 0 | Start | `0x5B` |
| 1 | Data ID | `0x4A` |
| 2 | Signal/Battery | Upper nibble: signal bars (0–5). Lower nibble: battery bars (0–5) |
| 4 | Temperature | Ambient °C + 40 (e.g. 25°C = 0x41) |
| 6 | Hour | 12-hour format |
| 7 | Minute | |
| 8 | Second | |
| 9 | AM/PM | 0x00 = AM, 0x01 = PM |
| 11 | Network | 0x04 = LTE/4G |
| 12 | Day | |
| 13 | Month | 1–12 |
| 14 | Year | Year mod 100 |
| 17 | Find Me | 0x01 = flash lights + beep, 0x00 = off |
| 18 | Checksum | `255 - (sum(bytes[0..17]) % 256)` |
| 19 | End | `0xFF` |

---

## Inbound Message Types (Bike → Phone, NOTIFY)

### Observed from RTR 310 shutdown capture

| ID | Prefix | Behaviour | Source |
|----|--------|-----------|--------|
| `0x10` | 5A | Static in capture | Odometer, fuel (confirmed Jupiter) |
| `0x11` | 5A | Near-static, 1 byte changes | Service reminder (confirmed Jupiter) |
| `0x12` | 5A | Static | Unknown — not in Jupiter docs |
| `0x5F` | 5A | Dynamic — multiple bytes change | Live telemetry (hypothesis) |
| `0x7D` | 5A | Static, fully packed (no 0xEA) | Unknown — not in Jupiter docs |
| `0x42` | 5B | Almost all 0xEA | Keep-alive / heartbeat (hypothesis) |

### Message 0x10 — Odometer & Fuel (from Jupiter RE, same ID)
| Byte | Field | Description |
|------|-------|-------------|
| 3–5 | Odometer | UInt24, big-endian. `value / 10.0` = km |
| 6 | Fuel | Lower nibble: bars (0–5). Upper nibble: reserve flag |
| 13 | Call Cmd | Button press during call: 1=Answer, 2=Reject |

### Message 0x11 — Service Reminder (from Jupiter RE, same ID)
| Byte | Field | Description |
|------|-------|-------------|
| 4 | Service | Service reminder indicator |

### Message 0x19 — Economy (from Jupiter RE — not yet observed on RTR 310)
| Byte | Field | Description |
|------|-------|-------------|
| 8 | Economy | Average fuel economy in km/L |
| 11–12 | DTE | Distance to empty (short) |

---

## Packet Analysis — RTR 310 Shutdown Capture (2026-08-08)

**Source file:** `captures/rtr-capture-20260808-150945.txt`
**Capture conditions:** Bike powering off, app connected passively.

### Observations
- All 21 packets are exactly 20 bytes (corrected from earlier 19-byte claim)
- All terminate with `0xFF`
- `0xEA` is the null/empty field value for inbound packets
- Checksum confirmed at byte 18 using formula `(C − sum(B0..B17)) mod 256`
  where C is per-message-type (NOT the Jupiter formula)
- Two packet variants for `0x5F` with byte 7 incrementing by 1 (0x1E → 0x1F) — confirmed frame counter
- `0x7D` packets are fully packed (zero `0xEA` bytes) and identical across all 3 occurrences — likely static config or device identity
- `0x5B 0x42` appears to be a control-channel heartbeat (almost all `0xEA`)
- Checksum for `0x5F` is **UNRESOLVED** — does not fit simple formula

### Timing
- Packet burst spans ~830ms (timestamps 975961 → 976791)
- Approximately 25 packets/second during shutdown sequence

---

## Ride Mode vs Parked Mode (from Jupiter RE)

- **Parked (ignition OFF):** Bike transmits only basic connectivity data. Responds to ping `0x4A`.
- **Ride (ignition ON):** Bike broadcasts telemetry packets `0x10`, `0x11`, `0x18`, `0x19`.
  Detecting any of these indicates the engine is running.

---

## Outstanding Questions

1. Is the AES-128 key the same on RTR 310 as Jupiter?
   → **Verify with btsnoop log:** enable HCI snoop, connect TVS Connect, extract log
2. Does the RTR 310 send a `0x9A 0xF2` challenge immediately after CCCD enable?
3. What do message IDs `0x12` and `0x7D` carry? (Not in Jupiter docs, observed on RTR 310)
4. What live data does `0x5F` carry? (2 variants observed — bytes 8–13 change)
5. What is the notification rate when ignition is ON?
6. Does the RTR 310 support navigation HUD commands (`0x4E`, `0x4F`, `0x50`)?

---

## Sources

| Source | Type | Confidence |
|--------|------|-----------|
| RTR 310 hardware capture (2026-08-08) | Direct observation | High |
| JupiterRideCompanion RE report (github.com/overclock98/JupiterRideCompanion) | Cross-reference, same protocol | High for shared fields, Medium for RTR-specific |
| nRF Connect session on OnePlus 12 | Direct observation | High |
