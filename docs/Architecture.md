# Architecture

## Goal

Build an open-source Android companion app and Bluetooth SDK for the TVS Apache RTR 310.

## Planned structure

- `app/` – Android application UI and features
- `ble-core/` or `smartx-sdk/` – Bluetooth communication layer
- `protocol/` – packet definitions and message notes
- `docs/` – reverse-engineering notes and feature documentation
- `captures/` – Bluetooth logs and reference captures
- `tools/` – scripts and helpers

## Current understanding

From nRF Connect, the RTR 310 exposes a proprietary TVS BLE service with two key characteristics:

- `5352` – WRITE
- `5354` – NOTIFY

This suggests the protocol is packet-based and likely multiplexes multiple features through those two channels.

## Next step

Capture and document traffic from the official TVS Connect app, then map packet formats to features.
