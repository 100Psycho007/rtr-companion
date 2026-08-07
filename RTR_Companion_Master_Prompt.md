# RTR Companion -- Project Master Prompt

> Project: RTR Companion Repository: 100Psycho007/rtr-companion

## Vision

RTR Companion is an open-source Android application and Bluetooth SDK
for the TVS Apache RTR 310 SmartXonnect TFT.

This project is **not** a clone of TVS Connect. Build a reusable BLE SDK
first, then a polished companion application.

## Confirmed Discoveries

-   Device: `TVSRTR310FKB0925`
-   Standard BLE Services:
    -   `0x1800` Generic Access
    -   `0x1801` Generic Attribute
    -   `0x180A` Device Information
-   Proprietary Service:
    -   `5456534d-5647-5341-5342-454e544f5251`
-   Characteristics:
    -   `5352` WRITE (Phone → Bike)
    -   `5354` NOTIFY (Bike → Phone)

Current evidence suggests a packet-based protocol using these two
characteristics.

## Engineering Principles

-   SDK first
-   UI second
-   Kotlin
-   Jetpack Compose
-   Material 3
-   Clean Architecture
-   SOLID
-   StateFlow
-   Coroutines
-   Android API 29+

Never send unknown BLE commands.

## Repository Structure

``` text
rtr-companion/
├── app/
├── ble-core/
├── protocol/
├── docs/
├── captures/
├── tools/
├── scripts/
└── .github/
```

## Roadmap

### Sprint 0

-   Environment setup
-   BLE discovery

### Sprint 1

-   BLE permissions
-   Scanner
-   Connect
-   Service discovery

Expected output:

``` text
Searching...
RTR Found
Connected
1800
1801
180A
TVSM
5352
5354
```

### Sprint 2

-   Notifications
-   Packet logger

### Sprint 3

-   Packet parser
-   SDK stabilization

### Sprint 4

-   Compose UI
-   Navigation
-   Notifications
-   Music

### Sprint 5

-   Diagnostics
-   Ride history
-   Public beta

## Workflow

Every feature: 1. Design 2. Document 3. Code 4. Test 5. Commit

## AI Instructions

Act as a senior Android engineer. Prefer clean architecture over
shortcuts. Do not invent packet formats. Initially only scan, connect,
discover services, enable notifications and log packets.
