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
- **Bike does NOT send telemetry without authentication handshake** — only shutdown sequences broadcast unconditionally

### Packet Structure (from RTR 310 shutdown capture, 2026-08-08)
- **Fixed length:** every inbound packet is exactly **19 bytes**
- **Frame type byte:** `0x5A` = data frame, `0x5B` = control/null frame
- **Message ID:** byte 1 identifies the data type
- **Payload:** bytes 2–16 (15 bytes); `0xEA` = empty/null field
- **Checksum:** byte 17 = additive sum of bytes 2–16, mod 256
- **Terminator:** byte 18 = always `0xFF`
- **Null field value:** `0xEA` for inbound packets

### Checksum Algorithm (verified)
- Additive sum of payload bytes mod 256
- Verified: `0x11` packet variants differ by `0x20` in one data byte; checksum differs by exactly `0x20`
- Equivalent form (from Jupiter RE): `255 - (sum(bytes[0..17]) % 256)`

### Message Types Observed (RTR 310 shutdown capture)
- `0x5A 0x10` — present, static across capture
- `0x5A 0x11` — present, near-static (1 byte changes between first and subsequent packets)
- `0x5A 0x12` — present, static
- `0x5A 0x5F` — present, **dynamic** — multiple bytes change between occurrences
- `0x5A 0x7D` — present, fully packed (zero `0xEA` bytes), static
- `0x5B 0x42` — present, almost all `0xEA`, static

### Protocol Shared with TVS Jupiter (cross-reference confirmed)
- Service UUID, write/notify characteristics: **identical**
- Packet framing (`0x5A`/`0x5B` start, `0xFF` end): **identical**
- Message ID `0x10` (odometer/fuel): **present on both**
- Message ID `0x11` (service reminder): **present on both**
- Authentication handshake required: **confirmed on Jupiter, strongly suspected on RTR 310**

---

## Hypothesis

Facts in this section are plausible based on available evidence but not yet experimentally confirmed on the RTR 310.

### Authentication Handshake
- **Authentication is required before live telemetry starts** — confirmed on Jupiter, strongly suspected on RTR 310 based on observed behaviour (no packets during connected+running state, burst at shutdown)
- **Handshake sequence (Jupiter, likely same on RTR 310):**
  1. Bike sends `0x9A 0xF2` + 16 random challenge bytes via NOTIFY
  2. Phone encrypts challenge with AES-128-CTR using shared key
  3. Phone sends `0x9A 0xF1` + encrypted response to WRITE characteristic
- **Jupiter AES key:** `7A A3 20 4D 16 1D B5 33 F4 EB 20 4F BC D7 3D D4` — may or may not be the same on RTR 310

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
