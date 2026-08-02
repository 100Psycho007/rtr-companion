# Reverse Engineering Log

## Session 1

- Connected to the RTR 310 using nRF Connect on a OnePlus 12.
- Confirmed BLE advertising and successful connection.
- Identified a proprietary TVS BLE service.
- Found two important characteristics:
  - `5352` – WRITE
  - `5354` – NOTIFY

## What to do next

- Capture Bluetooth HCI snoop logs while using the official TVS Connect app.
- Look for writes to `5352` and notifications from `5354`.
- Map packet contents to features such as navigation, notifications, and music.
