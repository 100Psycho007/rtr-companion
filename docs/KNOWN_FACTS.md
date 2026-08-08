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

### BLE Characteristics (within TVS proprietary service)
- **`5352`** — WRITE property — direction: Phone → Bike
  - Short UUID (4-hex digits relative to base UUID)
- **`5354`** — NOTIFY property — direction: Bike → Phone
  - Notifications successfully enabled via CCCD descriptor write

### Connection Behaviour
- Device accepts `connectGatt()` with `autoConnect=false`, `TRANSPORT_LE`
- Service discovery completes successfully after connection
- CCCD descriptor write to `5354` successfully enables notifications
- 600ms delay before `discoverServices()` is stable (per Android BLE recommendations)

### Application Behaviour
- RTR Companion app can scan, connect, discover services, and enable notifications
- Raw packets are received via `CHAR_NOTIFY` notifications

---

## Hypothesis

Facts in this section are plausible based on available evidence but not yet experimentally confirmed.

### Protocol Structure
- **Packet-based protocol** — Evidence: two characteristics (one write, one notify) with binary data.
  This is typical of multiplexed BLE protocols but the actual framing is unknown.
- **Multiple features multiplexed** — The single NOTIFY characteristic likely carries data for all bike features (speed, RPM, ride mode, etc.) via packet type bytes.
- **Phone sends commands at startup** — Most BLE protocols require an initialisation exchange. The phone app likely writes to `5352` immediately after connection.
- **Periodic notifications** — The bike likely sends state packets on a polling interval rather than only on change events.

### Security
- **No pairing/bonding required for basic comms** — nRF Connect connected without bonding.
- **May have application-layer authentication** — The TVS app may send a handshake on `5352` that the bike requires before sending data.

---

## Rejected

Facts in this section have been tested and found to be false.

*(Nothing rejected yet — investigation is in early stages)*

---

## Questions to Resolve

1. What is the exact byte structure of a notification packet?
2. What does the phone send on `5352` immediately after connecting?
3. Is there a length field, type byte, or checksum in the packets?
4. What is the notification rate (packets per second)?
5. Are there separate packet types for different features or is everything in one stream?
6. What happens if `5352` receives no writes — does the bike still send notifications?
7. What is the full Device Information service content (manufacturer name, firmware version)?

---

## Sources

| Source | Type | Date |
|--------|------|------|
| nRF Connect on OnePlus 12 | Direct hardware observation | Pre-2026-08 |
| RTR 310 owner's manual | Documentation reference | Pre-2026-08 |
| Android HCI log | Not yet extracted | Pending |
