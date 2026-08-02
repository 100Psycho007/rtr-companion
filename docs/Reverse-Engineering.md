# Reverse Engineering Log

## Session 1

- Connected to the RTR 310 using nRF Connect on a OnePlus 12.
- Confirmed BLE advertising and successful connection.
- Identified a proprietary TVS BLE service.
- Found two important characteristics:
  - `5352` – WRITE
  - `5354` – NOTIFY
- Generated a bug report on the phone for Bluetooth investigation.

## Notes

- The current evidence suggests that the RTR 310 uses a simple BLE design:
  - phone to bike: write packets
  - bike to phone: notification packets
- The exact packet format is not yet documented.
- The HCI log was not yet extracted from the bug report at this stage.

## What to do next

- Build a small Android test app outside the IDE workflow.
- Connect to the bike.
- Discover services.
- Enable notifications on `5354`.
- Log every notification received.
- Keep the app passive at first and avoid sending unknown writes.
