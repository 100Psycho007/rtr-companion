# Protocol Status

> Tracks every known packet type and field with its current confidence level.
> Source of truth: `captures/rtr-capture-20260808-150945.txt` + `docs/protocol/capture-20260808-150945.md`
>
> Confidence levels:
> - **CONFIRMED** — experimentally verified on RTR 310 hardware
> - **HYPOTHESIS** — plausible from cross-reference or partial evidence, not independently confirmed
> - **UNRESOLVED** — investigated but no conclusion reached; needs more data
> - **UNVERIFIED** — from Jupiter RE or other external source; not tested on RTR 310

---

## Frame Structure

| Field | Position | Value/Range | Confidence |
|-------|----------|-------------|-----------|
| Frame type | Byte 0 | 0x5A (data) or 0x5B (control) | **CONFIRMED** — capture 2026-08-08 |
| Message ID | Byte 1 | Varies per type | **CONFIRMED** |
| Payload | Bytes 2–17 | 16 bytes; 0xEA = null (inbound) | **CONFIRMED** |
| Checksum | Byte 18 | `(C − sum(B0..B17)) mod 256`, C varies per msg | **CONFIRMED** for 0x10,0x11,0x12,0x7D,0x42; UNRESOLVED for 0x5F |
| Terminator | Byte 19 | Always 0xFF | **CONFIRMED** — all 21 captured packets |
| Frame length | — | 20 bytes | **CONFIRMED** — all 21 captured packets |

---

## Checksum Constants (per message ID)

| Message ID | Constant C | Status |
|-----------|-----------|--------|
| 0x10 | 0x31 | **CONFIRMED** — 3 identical packets verify same checksum |
| 0x11 | 0xC3 | **CONFIRMED** — 2 variants cross-verified |
| 0x12 | 0x0B | Single value only — **HYPOTHESIS** (3 identical packets; no diff to verify formula) |
| 0x5F | UNKNOWN | **UNRESOLVED** — formula does not fit; needs live-ride capture |
| 0x7D | 0x99 | Single value only — **HYPOTHESIS** |
| 0x42 | 0x34 | Single value only — **HYPOTHESIS** |

---

## Inbound Message Types (Bike → Phone, NOTIFY characteristic 0x5354)

### 0x10 — Unknown (candidate: Odometer/Fuel)

| Field | Byte(s) | Value in capture | Confidence |
|-------|---------|-----------------|-----------|
| Frame type | 0 | 0x5A | CONFIRMED |
| Message ID | 1 | 0x10 | CONFIRMED |
| Null | 2–3 | EA EA | CONFIRMED null |
| Candidate odometer | 4–6 | F6 24 DB | HYPOTHESIS — consistent with Jupiter B3–B5 if 0-indexed; value = 0xF624DB / 10 = 1,606,235.1 km (implausible → indexing or scaling is wrong) |
| Candidate fuel | 7 | CA | HYPOTHESIS — lower nibble 0xA = 10 (out of 0–5 range → field mapping may differ) |
| Null/data | 8–17 | mix of EA and data bytes | See raw capture |
| Checksum | 18 | 0x01 | CONFIRMED (C=0x31) |
| Terminator | 19 | 0xFF | CONFIRMED |

**Notes:** The odometer bytes from Jupiter RE (bytes 3–5, 1-indexed = bytes 2–4, 0-indexed)
do not match. Jupiter uses 1-indexed byte positions in its documentation. If RTR 310
uses 0-based indexing, the odometer would be B4–B6. The value F6 24 DB / 10 = 1,606,235
km is impossible for a single bike — the scaling or offset is wrong, or these bytes
carry different data. **Do not decode 0x10 payload until confirmed with known odometer reading.**

---

### 0x11 — Unknown (candidate: Service Reminder)

| Field | Byte(s) | Variant B1 | Variant B2 | Confidence |
|-------|---------|-----------|-----------|-----------|
| Frame type | 0 | 0x5A | 0x5A | CONFIRMED |
| Message ID | 1 | 0x11 | 0x11 | CONFIRMED |
| Null | 2 | EA | EA | CONFIRMED null |
| Data | 3 | 2A | 2A | Static |
| Null | 4–5 | EA EA | EA EA | |
| Data | 6–7 | E6 EE | E6 EE | Static |
| Null | 8–9 | EA EA | EA EA | |
| Dynamic field | 10 | CA | EA | **CONFIRMED dynamic** — only byte that differs |
| Null | 11–12 | EA EA | EA EA | |
| Data | 13–14 | DB E1 | DB E1 | Static |
| Null | 15–16 | EA EA | EA EA | |
| Data | 17 | FA | FA | Static |
| Checksum | 18 | B2 | 92 | CONFIRMED (C=0xC3) |
| Terminator | 19 | FF | FF | CONFIRMED |

**Notes:** B10 changes once from 0xCA to 0xEA and remains at 0xEA for all subsequent
occurrences. Jupiter RE claims byte 4 (1-indexed) = service reminder flag. If 0-indexed
that is byte 3 = 0x2A (static) — not matching either. Field semantics UNVERIFIED.

---

### 0x12 — Unknown

| Field | Byte(s) | Value (all 3 identical) | Confidence |
|-------|---------|------------------------|-----------|
| Frame type | 0 | 0x5A | CONFIRMED |
| Message ID | 1 | 0x12 | CONFIRMED |
| Data | 2–17 | EA EA 63 EA E6 EA E9 01 E9 C1 F7 40 84 BA EB EA | Static |
| Checksum | 18 | 0xB6 | CONFIRMED (C=0x0B) |
| Terminator | 19 | 0xFF | CONFIRMED |

**Notes:** Not documented in Jupiter RE. May be RTR 310 specific. Static across all
3 occurrences in the capture — no diff possible. Purpose UNRESOLVED.

---

### 0x5F — Unknown (candidate: Live Telemetry)

| Field | Byte(s) | Variant C1 | Variant C2 | Confidence |
|-------|---------|-----------|-----------|-----------|
| Frame type | 0 | 0x5A | 0x5A | CONFIRMED |
| Message ID | 1 | 0x5F | 0x5F | CONFIRMED |
| Null | 2–3 | EA EA | EA EA | |
| Data | 4–5 | 96 98 | 96 98 | Static across variants |
| Data | 6 | 0D | 0D | Static |
| Frame counter | 7 | 1E | 1F | **CONFIRMED incrementing** (+1) — frame counter |
| Dynamic | 8–13 | 7A C4 F5 A9 EB AB | 92 33 F5 A3 CB DD | **Multiple bytes change** |
| Null | 14–16 | EA EA EA | EA EA EA | |
| Dynamic | 17 | EA | 00 | Changes |
| Checksum | 18 | F1 | 6B | Dynamic (formula UNRESOLVED) |
| Terminator | 19 | FF | FF | CONFIRMED |

**Notes:** Only 2 distinct values from a shutdown burst. Byte 7 increments — strongly
suggests frame counter. Bytes 8–13 change significantly between variants. Checksum
formula not solved — does not fit `(C − sum) mod 256`. Likely live telemetry (speed, RPM,
or similar sensor data). **Cannot decode without live-ride capture providing many variants.**

---

### 0x7D — Unknown (candidate: Static Config/Identity)

| Field | Byte(s) | Value (all 3 identical) | Confidence |
|-------|---------|------------------------|-----------|
| Frame type | 0 | 0x5A | CONFIRMED |
| Message ID | 1 | 0x7D | CONFIRMED |
| Data | 2–17 | A7 AE DC D9 D2 A9 B9 DB B2 BE DB AB D8 DD DC D8 | Static, fully packed |
| Checksum | 18 | 0xDA | CONFIRMED (C=0x99) |
| Terminator | 19 | 0xFF | CONFIRMED |

**Notes:** No 0xEA bytes in the payload — fully packed. Identical across all 3
occurrences. Not documented in Jupiter RE. Likely RTR 310 specific. Could be device
identity, firmware version, or static configuration. Static nature suggests identity/config.
Purpose UNRESOLVED.

---

### 0x42 (control frame) — Unknown (candidate: Heartbeat)

| Field | Byte(s) | Value (all 3 identical) | Confidence |
|-------|---------|------------------------|-----------|
| Frame type | 0 | 0x5B (control) | CONFIRMED |
| Message ID | 1 | 0x42 | CONFIRMED |
| Mostly null | 2–11 | EA×10 | |
| Data | 12 | DD | Static |
| Null | 13–17 | EA×5 | |
| Checksum | 18 | 0xC6 | CONFIRMED (C=0x34) |
| Terminator | 19 | 0xFF | CONFIRMED |

**Notes:** Control frame (0x5B prefix). Almost entirely 0xEA. One non-null byte at B12=0xDD.
Identical across all 3 occurrences. Likely a heartbeat or keepalive from the bike.
Relationship to the phone's ping (0x5B 0x4A) is unknown.

---

## Outbound Message Types (Phone → Bike, WRITE characteristic 0x5352)

### 0x4A — Keep-Alive Ping (HYPOTHESIS — from Jupiter RE)

| Field | Byte | Description | Confidence |
|-------|------|-------------|-----------|
| Frame type | 0 | 0x5B | HYPOTHESIS |
| Message ID | 1 | 0x4A | HYPOTHESIS |
| Signal+battery | 2 | upper nibble: signal bars, lower: battery | HYPOTHESIS |
| Padding | 3 | 0x00 | HYPOTHESIS |
| Temperature | 4 | °C + 40 | HYPOTHESIS |
| Padding | 5 | 0x00 | HYPOTHESIS |
| Hour | 6 | 12-hour (1–12) | HYPOTHESIS |
| Minute | 7 | 0–59 | HYPOTHESIS |
| Second | 8 | 0–59 | HYPOTHESIS |
| AM/PM | 9 | 0x00=AM, 0x01=PM | HYPOTHESIS |
| Padding | 10 | 0x00 | HYPOTHESIS |
| Network | 11 | 0x04=LTE/4G | HYPOTHESIS |
| Day | 12 | 1–31 | HYPOTHESIS |
| Month | 13 | 1–12 | HYPOTHESIS |
| Year | 14 | year mod 100 | HYPOTHESIS |
| Padding | 15–16 | 0x00 0x00 | HYPOTHESIS |
| Find Me | 17 | 0x01=flash+beep, 0x00=off | HYPOTHESIS |
| Checksum | 18 | formula from Jupiter, NOT verified on RTR 310 | HYPOTHESIS |
| Terminator | 19 | 0xFF | HYPOTHESIS |

**Status: DISABLED** — PingPacketBuilder is isolated behind PASSIVE protocol mode flag.
Must not be sent until verified on RTR 310 hardware.

---

## Authentication Handshake (HYPOTHESIS — from Jupiter RE)

| Packet | Direction | Structure | Confidence |
|--------|-----------|-----------|-----------|
| Challenge | Bike → Phone | 0x9A 0xF2 [16 random bytes] [checksum] 0xFF | HYPOTHESIS |
| Response | Phone → Bike | 0x9A 0xF1 [AES-128-CTR encrypted] [checksum] 0xFF | HYPOTHESIS |

**AES key:** `7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4`
**Key status: UNVERIFIED on RTR 310.** Key is in HandshakeManager.kt but the
handshake is disabled in PASSIVE protocol mode. Must not be sent until verified.

---

## Questions to Resolve

| # | Question | How to Resolve |
|---|----------|----------------|
| 1 | Checksum formula for 0x5F | Live-ride capture with many 0x5F variants |
| 2 | AES key correct on RTR 310? | btsnoop HCI log from TVS Connect session |
| 3 | Does RTR 310 send 0x9A/0xF2 auth challenge? | Live session with ignition ON |
| 4 | What data does 0x7D carry? | Compare against known device info (VIN, firmware version) |
| 5 | What data does 0x12 carry? | Compare with ignition ON capture |
| 6 | What data does 0x5F carry? | Live-ride capture + accelerometer/GPS cross-reference |
| 7 | Are 0x18/0x19 present on RTR 310? | Ignition ON capture |
| 8 | Is ping 0x4A required on RTR 310? | Test with/without ping during live session |
| 9 | Are navigation HUD commands supported? | Inspect TVS Connect network traffic |

---

*Last updated: 2026-08-08 — Protocol integrity correction pass*
*Based on: captures/rtr-capture-20260808-150945.txt (21 packets, ignition OFF)*
