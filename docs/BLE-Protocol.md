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
  - UUID is experimentally confirmed (nRF Connect + APK class `ApacheBLEConnectionConfig`).

### Characteristics
- `00005352-0000-1000-8000-00805f9b34fb` — **WRITE** (write type 2) — Phone → Bike
- `00005354-0000-1000-8000-00805f9b34fb` — **NOTIFY** — Bike → Phone

---

## Packet Structure (All Messages)

Every packet is **exactly 20 bytes** (confirmed from RTR 310 capture 2026-08-08
and cross-confirmed by APK model layouts — all use positions 1–20).

### 20-byte inbound (Bike → Phone, NOTIFY):
```
Byte  0    : Frame type   — 0x5A (data) or 0x5B (control/null)
Byte  1    : Message ID   — identifies the data type
Bytes 2–17 : Payload      — 16 bytes; 0xEA = empty/null field
Byte  18   : Checksum     — (C − sum(B0..B17)) mod 256, C is per-message-type
Byte  19   : Terminator   — always 0xFF
```

### 20-byte outbound (Phone → Bike, WRITE):
```
Byte  0    : Start byte   — 0x5A or 0x5B
Byte  1    : Data ID
Bytes 2–17 : Payload      — 16 bytes; 0x00 = empty field
Byte  18   : Checksum     — 255 − (sum(B0..B17) mod 256)  [HYPOTHESIS — not verified]
Byte  19   : End byte     — always 0xFF
```

### APK field position convention
APK models use **1-based positions** where position 1 = byte index 0 (the start byte).
`ParsingMeta.start` becomes `start - 1` for the Java byte array.
Multi-byte values are concatenated as hex strings before integer parsing.

### Null/Empty field value
- **Inbound:** `0xEA` = empty/unused field
- **Outbound:** `0x00` = empty/unused field

---

## Authentication Handshake (Required for Live Telemetry)

**Status:** Flow confirmed via Jupiter RE (same protocol). RTR 310 key unconfirmed.

### Flow
```
1. Phone connects and enables NOTIFY on 0x5354
2. Bike sends challenge: 0x9A 0xF2 + 16 random bytes
3. Phone encrypts challenge with AES-128-CTR, no padding
   Key (Jupiter): 7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4
   Key (RTR 310): UNCONFIRMED — fetched from tvs.kogo.ai/challenges_v2 at runtime
                  APK has Apache-specific crypto: encryptDataApache / getKeyByteArrayApache
4. Phone sends: 0x9A 0xF1 + encrypted bytes → WRITE char 0x5352
5. Bike acknowledges and begins streaming telemetry
```

Without this handshake, only shutdown burst packets are received (no live telemetry).

### Keep-Alive Ping — Data ID 0x4A
Must be sent continuously while connected.

| Byte | Field | Notes |
|------|-------|-------|
| 0 | Start | `0x5B` |
| 1 | Data ID | `0x4A` |
| 2 | Signal/Battery | Upper nibble: signal bars (0–5). Lower nibble: battery bars (0–5) |
| 4 | Temperature | °C + 40 |
| 6 | Hour | 12-hour |
| 7 | Minute | |
| 8 | Second | |
| 9 | AM/PM | 0x00 = AM, 0x01 = PM |
| 11 | Network | 0x04 = LTE/4G |
| 12 | Day | |
| 13 | Month | 1–12 |
| 14 | Year | Year mod 100 |
| 17 | Find Me | 0x01 = flash + beep |
| 18 | Checksum | `255 - (sum(bytes[0..17]) % 256)` |
| 19 | End | `0xFF` |

---

## Inbound Message Types (Bike → Phone, NOTIFY)

APK class: `com.tvs.bike.core.protocol.apache.ApacheIncomingFrameIdentifier`

| ID | APK Frame Type | APK Model | Confidence |
|----|---------------|-----------|-----------|
| `0x10` | `SPEEDOMETER_DATA_5A_10_FRAME` | `ApacheSpeedOMeter1` | **APK + capture confirmed** |
| `0x11` | `SPEEDOMETER_DATA_5A_11_FRAME` | `ApacheSpeedOMeter2` | **APK + capture confirmed** |
| `0x12` | `SPEEDOMETER_DATA_5A_12_FRAME` | `ApacheSpeedOMeter3` | **APK + capture confirmed** |
| `0x16` | `SPEEDOMETER_DATA_5A_16_FRAME` | `ApacheSpeedOMeter4` | APK confirmed, not in capture |
| `0x18` | `SPEEDOMETER_DATA_5A_18_FRAME` | `ApacheSpeedOMeter5` | APK confirmed, not in capture |
| `0x29` | `WIFI_PASSWORD_5A_29_FRAME` | `ApacheBasicData` | APK confirmed, not in capture |
| `0x5F` | *(not in APK map)* | RTR 310 specific? | Capture only — unresolved |
| `0x7D` | *(not in APK map)* | **Device identity (VIN)** | Capture + XOR decode |
| `0x42` | *(not in APK map)* | Keep-alive heartbeat | Capture only — hypothesis |

---

### Message 0x10 — ApacheSpeedOMeter1

| Pos | Idx | Field | Notes |
|-----|-----|-------|-------|
| 1 | 0 | startByte | `0x5A` |
| 2 | 1 | dataId | `0x10` |
| 3 | 2 | speed | km/h |
| 4–6 | 3–5 | odometer | UInt24 big-endian; `/ 10.0` = km |
| 7 | 6 | fuelLevel | raw bar value |
| 8 | 7 | averageSpeed | km/h |
| 9 | 8 | mileage | km/L |
| 10 | 9 | topSpeed | km/h |
| 11 | 10 | throttle | throttle position |
| 12 | 11 | locationTag / switchStatus | same byte |
| 13 | 12 | zeroTo60Time | raw; `/ 10.0` = seconds |
| 14 | 13 | averageMileageDirect | km/L |
| 15–16 | 14–15 | tripFMeter | UInt16; `/ 10.0` = km |
| 17–18 | 16–17 | engineRpm | UInt16 |
| 19 | 18 | checkSum | `(0x31 − sum(B0..B17)) mod 256` |
| 20 | 19 | endByte | `0xFF` |

---

### Message 0x11 — ApacheSpeedOMeter2

| Pos | Idx | Field | Notes |
|-----|-----|-------|-------|
| 1 | 0 | startByte | `0x5A` |
| 2 | 1 | dataId | `0x11` |
| 3 | 2 | vehicleDirection2 | |
| 4 | 3 | vehicleState1 | |
| 5 | 4 | serviceReminder | |
| 6 | 5 | gearPosition | |
| 7 | 6 | batteryVoltage | 1 decimal place |
| 8 | 7 | softwareVersion | |
| 9 | 8 | milBlinkCode / reserve1 | |
| 10 | 9 | vehicleModel / reserve2 | |
| 11 | 10 | vehicleDiagnostics | |
| 12 | 11 | reserve3 | |
| 13 | 12 | turnIndicatorStatus | |
| 14 | 13 | tellTaleStatus / engineTemperature | |
| 15 | 14 | screenMatrix / reserve15 | |
| 16 | 15 | vehicleState3 / reserve16 | |
| 17 | 16 | absMilBlinkCode / reserve17 | |
| 18 | 17 | backlightIllumination / vehicleMode | |
| 19 | 18 | checkSum | `(0xC3 − sum(B0..B17)) mod 256` |
| 20 | 19 | endByte | `0xFF` |

---

### Message 0x12 — ApacheSpeedOMeter3

| Pos | Idx | Field | Notes |
|-----|-----|-------|-------|
| 1 | 0 | startByte | `0x5A` |
| 2 | 1 | dataId | `0x12` |
| 3 | 2 | leanAngle | degrees |
| 4–5 | 3–4 | cruisingRange | UInt16; km |
| 6 | 5 | wheelAngelOffset | |
| 7 | 6 | acceleration | |
| 8 | 7 | torque | |
| 9–10 | 8–9 | tripDistance | UInt16; `/ 10.0` = km |
| 11 | 10 | tripTimeHour | hours |
| 12 | 11 | tripTimeMin | minutes |
| 13 | 12 | tripMileage | km/L |
| 14–15 | 13–14 | tripFuel | UInt16; `/ 10.0` = litres |
| 16 | 15 | overspeedThreshold | km/h |
| 17 | 16 | overSpeedSetting | |
| 18 | 17 | reserve3 | |
| 19 | 18 | checkSum | `(C − sum(B0..B17)) mod 256` |
| 20 | 19 | endByte | `0xFF` |

---

### Message 0x16 — ApacheSpeedOMeter4 (Lap Timing)

Not yet observed in RTR 310 captures.

| Pos | Idx | Field |
|-----|-----|-------|
| 1 | 0 | startByte |
| 2 | 1 | dataId |
| 3 | 2 | lapTimeMin |
| 4 | 3 | lapTimeSec |
| 5 | 4 | lapTimeMSec |
| 6 | 5 | lapNumber |
| 7 | 6 | bestLapMin |
| 8 | 7 | bestLapSeconds |
| 9 | 8 | bestLapMilliSeconds |
| 10 | 9 | bestLapNumber |
| 11 | 10 | lapTrigger |
| 12–18 | 11–17 | reserve1–reserve7 |
| 19 | 18 | checkSum |
| 20 | 19 | endByte |

---

### Message 0x18 — ApacheSpeedOMeter5 (Engine Diagnostics)

Not yet observed in RTR 310 captures.

| Pos | Idx | Field | Notes |
|-----|-----|-------|-------|
| 1 | 0 | startByte | |
| 2 | 1 | dataId | |
| 3 | 2 | engineLoad | |
| 4–5 | 3–4 | accumulatedFuelInjectionTime | UInt16 |
| 6 | 5 | manifoldAirPressure | |
| 7 | 6 | barometricPressure | |
| 8 | 7 | intakeAirTemperature | |
| 9 | 8 | engineTemperature | |
| 10–11 | 9–10 | fuelInjectionTime | UInt16 |
| 12 | 11 | batteryVoltage | `× 0.1`, 2 decimal places |
| 13–14 | 12–13 | runTimeSinceEngineStart | UInt16 |
| 15–16 | 14–15 | distanceTraveled | UInt16 |
| 17–18 | 16–17 | fuelInjectionVolume | UInt16 |
| 19 | 18 | checkSum | |
| 20 | 19 | endByte | |

---

### Message 0x7D — Device Identity (VIN / Chassis ID)

Not in APK frame map. Observed in every capture. Static, fully packed (no `0xEA`).

**Decode:** each payload byte XOR `0xEA` = ASCII character.

Real captured packet:
```
5A 7D A7 AE DC D9 D2 A9 B9 DB B2 BE DB AB D8 DD DC D8 DA FF
```

XOR decode → **`MD638CS1XT1A2762`**
- `MD6` = TVS Motor Company manufacturer prefix (confirms TVS origin)
- Full string is the bike's VIN or chassis identifier

| Idx | Raw | XOR 0xEA | ASCII |
|-----|-----|----------|-------|
| 2 | A7 | 4D | M |
| 3 | AE | 44 | D |
| 4 | DC | 36 | 6 |
| 5 | D9 | 33 | 3 |
| 6 | D2 | 38 | 8 |
| 7 | A9 | 43 | C |
| 8 | B9 | 53 | S |
| 9 | DB | 31 | 1 |
| 10 | B2 | 58 | X |
| 11 | BE | 54 | T |
| 12 | DB | 31 | 1 |
| 13 | AB | 41 | A |
| 14 | D8 | 32 | 2 |
| 15 | DD | 37 | 7 |
| 16 | DC | 36 | 6 |
| 17 | D8 | 32 | 2 |

---

### Message 0x5F — Unknown (RTR 310 specific, dynamic)

Not in APK `ApacheIncomingFrameIdentifier`. May be RTR 310 specific.

```
5A 5F EA EA 96 98 0D 1E 7A C4 F5 A9 EB AB EA EA EA EA F1 FF
5A 5F EA EA 96 98 0D 1F 92 33 F5 A3 CB DD EA EA EA 00 6B FF
```

- Byte 7 increments (0x1E → 0x1F) — **frame counter confirmed**
- Bytes 8–13 change — likely live sensor payload
- Byte 17 changes (`0xEA` → `0x00`) — unknown flag
- Checksum **UNRESOLVED** — needs live-ride capture to decode

---

### Message 0x42 (5B prefix) — Keep-Alive Heartbeat

```
5B 42 EA EA EA EA EA EA EA EA EA EA DD EA EA EA EA EA C6 FF
```
- Almost all `0xEA` null bytes
- Byte 12 = `0xDD` — possibly a counter or status flag
- Hypothesis: bike-side keep-alive

---

## Ride Mode vs Parked Mode

- **Parked (ignition OFF):** Shutdown burst only. Responds to ping `0x4A`.
- **Ride (ignition ON):** Full telemetry: `0x10`, `0x11`, `0x12`, `0x16`, `0x18`, possibly `0x5F`.

---

## Outstanding Questions

1. AES-128-CTR key for RTR 310 (intercept via mitmproxy + objection, or Frida hook on `getKeyByteArrayApache()`)
2. Does RTR 310 send `0x9A 0xF2` challenge immediately after CCCD enable?
3. What does `0x5F` carry? Needs ignition-ON capture.
4. What is `0x42` byte 12 (`0xDD`) — counter or status?
5. Packet rate during active ride?
6. Navigation HUD commands (`0x4E`/`0x4F`/`0x50`) supported?
7. Decode `ApacheMobileToCluster` for full outgoing command protocol.

---

## Sources

| Source | Type | Confidence |
|--------|------|-----------|
| RTR 310 hardware capture (2026-08-08) | Direct observation | High |
| TVS Connect APK (JADX decompile) | Static analysis | High |
| TVS_RTR310_BLE_Reverse_Engineering_Findings.md | Compiled research | High |
| JupiterRideCompanion RE report | Cross-reference | High (shared), Medium (RTR-specific) |
| nRF Connect session on OnePlus 12 | Direct observation | High |
