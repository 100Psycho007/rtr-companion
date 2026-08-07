# Session 001 — Initial BLE Discovery

## Goal

Connect to the TVS Apache RTR 310 using nRF Connect and identify BLE services and characteristics.

## Work Completed

- Connected to the RTR 310 using **nRF Connect** on a OnePlus 12
- Confirmed that the device advertises successfully and accepts BLE connections
- Identified the following BLE structure:
  - Standard services: 0x1800 (Generic Access), 0x1801 (Generic Attribute), 0x180A (Device Information)
  - Proprietary TVS service: `5456534d-5647-5341-5342-454e544f5251`
  - Two key characteristics inside the proprietary service:
    - `5352` — WRITE (Phone → Bike)
    - `5354` — NOTIFY (Bike → Phone)
- Generated a Bluetooth bug report on the phone for future HCI log extraction

## Files Changed

None — investigation session only, no code written.

## Architecture Decisions

None formal. Observation that the device appears to use a simple two-characteristic protocol (write + notify) which suggests a packet-multiplexed design.

## Problems Encountered

- HCI log not yet extracted from the bug report file

## Testing Performed

- Manual hardware test with nRF Connect
- Confirmed connection and service discovery

## Next Session Goals

- Build a small Android app to connect passively to the bike
- Enable notifications on `5354`
- Log every notification received
- Avoid all WRITE operations until protocol is known
