# Jupiter Cross-Reference

> **Purpose:** Clearly separate what is known about the TVS Jupiter from what is
> confirmed on the RTR 310. Jupiter information is a research starting point, NOT
> proof of RTR 310 behaviour.
>
> Source: github.com/overclock98/JupiterRideCompanion

---

## CONFIRMED — Shared Between Jupiter and RTR 310

These items have been directly confirmed on both devices.

| Item | Evidence |
|------|---------|
| TVS proprietary service UUID `5456534d-5647-5341-5342-454e544f5251` | nRF Connect on RTR 310 + Jupiter RE |
| WRITE characteristic `5352` (Phone → Bike) | nRF Connect on RTR 310 + Jupiter RE |
| NOTIFY characteristic `5354` (Bike → Phone) | nRF Connect on RTR 310 + Jupiter RE |
| 20-byte frame structure for outbound (Phone → Bike) packets | Jupiter RE (20-byte) + RTR 310 capture confirmed 20-byte inbound |
| Frame type byte 0x5A (data) and 0x5B (control) | RTR 310 capture 2026-08-08 confirmed both |
| Terminator byte 0xFF at position 19 | RTR 310 capture 2026-08-08 — all 21 packets |
| Message ID 0x10 present on both | RTR 310 capture confirmed presence; fields unverified |
| Message ID 0x11 present on both | RTR 310 capture confirmed presence; fields unverified |
| Bike does NOT send live telemetry without authentication | Observed on RTR 310: only shutdown burst received |

---

## JUPITER CONFIRMED — Not Yet Verified on RTR 310

These observations are documented in the Jupiter RE report and are plausible for the
RTR 310 given the shared protocol layer, but have **not** been independently confirmed
by direct RTR 310 hardware testing.

### Authentication Handshake

**Jupiter observation:**
1. After CCCD notification enable, bike sends challenge:
   `0x9A 0xF2 [16 random bytes] [checksum] 0xFF`
2. Phone encrypts the 16-byte challenge using AES-128-CTR, zero IV, no padding
3. Phone sends response to WRITE characteristic:
   `0x9A 0xF1 [16 encrypted bytes] [checksum] 0xFF`
4. Bike acknowledges and begins streaming live telemetry

**RTR 310 status:** UNVERIFIED.
- No `0x9A 0xF2` packet was observed in the 2026-08-08 shutdown capture.
- The capture was taken with ignition OFF — the bike may only send the challenge
  with ignition ON or during an active session.
- Verification method: enable HCI snoop log on Android, connect TVS Connect to
  RTR 310 with ignition ON, extract btsnoop log from bug report, inspect in Wireshark.

### Jupiter AES-128-CTR Key

```
7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4
```

**Jupiter status:** Used in JupiterRideCompanion, reportedly working on Jupiter.
**RTR 310 status:** UNVERIFIED.
- The RTR 310 may use the same key (same protocol family) or a different one.
- This key is present in `HandshakeManager.kt` as `JUPITER_AES_KEY` with explicit
  comments marking it unverified.
- **This key MUST NOT be sent to the RTR 310 until verified via btsnoop log.**
  HandshakeManager is currently disabled behind the PASSIVE protocol mode flag.

### Keep-Alive Ping Packet (0x5B 0x4A)

**Jupiter observation:** Phone must send a 20-byte ping every ~1 second.

| Byte | Field | Value |
|------|-------|-------|
| 0    | Frame type | 0x5B |
| 1    | Msg ID | 0x4A |
| 2    | Signal+battery | upper nibble: signal bars (0–5), lower nibble: battery bars (0–5) |
| 3    | Padding | 0x00 |
| 4    | Temperature | °C + 40 |
| 5    | Padding | 0x00 |
| 6    | Hour | 12-hour (1–12) |
| 7    | Minute | 0–59 |
| 8    | Second | 0–59 |
| 9    | AM/PM | 0x00=AM, 0x01=PM |
| 10   | Padding | 0x00 |
| 11   | Network type | 0x04=LTE/4G |
| 12   | Day | 1–31 |
| 13   | Month | 1–12 |
| 14   | Year | year mod 100 |
| 15   | Padding | 0x00 |
| 16   | Padding | 0x00 |
| 17   | Find Me | 0x01=flash+beep, 0x00=off |
| 18   | Checksum | formula unverified on RTR 310 |
| 19   | Terminator | 0xFF |

**RTR 310 status:** UNVERIFIED. Ping format and necessity not confirmed on RTR 310.
Ping is disabled in PASSIVE protocol mode (current default).

### Message 0x10 — Odometer & Fuel (field layout)

**Jupiter observation:**
| Byte | Field | Format |
|------|-------|--------|
| 3–5  | Odometer | UInt24 big-endian, divide by 10.0 for km |
| 6    | Fuel | lower nibble = bars (0–5), upper nibble = reserve flag |
| 13   | Call command | 1=Answer, 2=Reject |

**RTR 310 status:** Message 0x10 is present in the RTR 310 shutdown capture.
The bytes at positions 3–5 contain non-null data (F6 24 DB in all occurrences)
which could be odometer bytes. **Field semantics are UNVERIFIED** — the numeric
value has not been cross-checked against the physical odometer reading.

### Message 0x11 — Service Reminder

**Jupiter observation:** Byte 4 = service reminder indicator.

**RTR 310 status:** Message 0x11 is present. Byte 4 = 0xEA (null) in the capture.
Field semantics are UNVERIFIED on RTR 310.

### Message 0x19 — Economy

**Jupiter observation:** Bytes 8 = fuel economy (km/L), bytes 11–12 = DTE.

**RTR 310 status:** Message 0x19 was **not observed** in the RTR 310 shutdown capture.
May appear during an active ride (ignition ON).

### Message 0x18 — Speed/RPM (hypothesis)

**Jupiter observation:** Live speed and RPM data.

**RTR 310 status:** Message 0x18 was **not observed** in the RTR 310 shutdown capture.
Expected only with ignition ON.

---

## SHARED HYPOTHESES (plausible for both, unverified on either individually)

- `0xEA` is the null/empty field sentinel for inbound (Bike → Phone) packets.
  Observed in RTR 310 capture — confirmed for RTR 310.
- Checksum formula uses bytes B0..B17 with a per-message-type constant C.
  Jupiter documents `255 − (sum mod 256)` — this does NOT match RTR 310 values.
  The C constant differs between Jupiter and RTR 310 (or between message IDs on RTR 310).

---

## UNVERIFIED ASSUMPTIONS (must not be treated as facts)

| Assumption | Origin | Why Unverified |
|-----------|--------|----------------|
| Jupiter AES key works on RTR 310 | Jupiter RE | Never tested on RTR 310 |
| RTR 310 sends auth challenge 0x9A/0xF2 | Jupiter RE extrapolation | Not observed in capture |
| Ping 0x5B/0x4A is required on RTR 310 | Jupiter RE extrapolation | Not observed response |
| Checksum formula `255 − sum mod 256` | Jupiter RE | Does not reproduce RTR 310 byte 18 values |
| 0x18/0x19 present on RTR 310 | Jupiter RE extrapolation | Not observed in any RTR 310 capture |
| Navigation HUD commands (0x4E/0x4F/0x50) supported | Jupiter RE extrapolation | Not tested |

---

## How to Promote Items to RTR 310 Confirmed

1. Capture btsnoop HCI log from TVS Connect connecting to RTR 310 with ignition ON
2. Extract and decode in Wireshark
3. Cross-reference observed packet bytes with this document
4. Update `docs/KNOWN_FACTS.md` with evidence citation
5. Update `docs/protocol/PROTOCOL_STATUS.md` confidence levels

---

*Last updated: 2026-08-08 — Protocol integrity correction pass*
