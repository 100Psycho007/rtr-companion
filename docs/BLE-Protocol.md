# BLE Protocol Notes

## Device

- Device name observed in nRF Connect: `TVSRTR310FKB0925`

## Services observed

### Standard services
- `0x1800` – Generic Access
- `0x1801` – Generic Attribute
- `0x180A` – Device Information

### Proprietary TVS service
- `5456534d-5647-5341-5342-454e544f5251`

## Characteristics observed

- `5352` – WRITE – Characteristic 3
- `5354` – NOTIFY – Characteristic 4

## Notes

The current evidence suggests that the RTR 310 uses a simple BLE design:

- phone to bike: write packets
- bike to phone: notification packets

The exact packet format is not yet documented.
