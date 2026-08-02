# Reverse Engineering Log

## Session 1

- Connected to the RTR 310 using nRF Connect on a OnePlus 12.
- Confirmed BLE advertising and successful connection.
- Identified a proprietary TVS BLE service.
- Found two important characteristics:
  - `5352` – WRITE
  - `5354` – NOTIFY
- Generated a bug report on the phone for Bluetooth investigation.

## Session 2

- Started documenting the project in GitHub alongside the investigation.
- Updated the architecture and BLE protocol notes in the repository.
- Confirmed the project direction: build a separate RTR 310 app and reuse ideas only when useful.

## Notes

- The current evidence suggests that the RTR 310 uses a simple BLE design:
  - phone to bike: write packets
  - bike to phone: notification packets
- The exact packet format is not yet documented.
- The HCI log was not yet extracted from the bug report at this stage.
- We should continue documenting every discovery in GitHub so nothing is lost.

## What to do next

- Build a small Android test app outside the IDE workflow.
- Connect to the bike.
- Discover services.
- Enable notifications on `5354`.
- Log every notification received.
- Keep the app passive at first and avoid sending unknown writes.
