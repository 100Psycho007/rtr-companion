# Architecture

## Vision

RTR Companion is a SDK-first, UI-second Android project. The BLE communication layer
(`ble-core`) and protocol layer (`protocol`) are designed to be independently usable
and eventually publishable as a standalone library.

---

## Module Dependency Graph

```
         ┌─────────┐
         │   app   │
         └────┬────┘
              │ depends on
       ┌──────┴────────┐
       │               │
  ┌────▼─────┐   ┌─────▼──────┐
  │ ble-core │◄──│  protocol  │
  └──────────┘   └────────────┘
```

- `app` depends on both `ble-core` and `protocol`
- `protocol` depends on `ble-core` (for shared models: `RtrDevice`, `ConnectionState`, etc.)
- `ble-core` has no internal project dependencies

---

## Module Responsibilities

### `app/`

**Purpose:** Android application layer — UI, navigation, permissions, and user-facing features.

**Must not:** contain BLE API calls or raw protocol handling.

**Key components:**

| Component | Responsibility |
|-----------|---------------|
| `MainActivity` | Single activity; handles runtime BLE permissions; launches share sheet for packet export |
| `RtrApplication` | App initialisation (Timber logging) |
| `MainViewModel` | Bridges `ble-core` + `protocol` to Compose UI; owns `PacketLogger` instance |
| `RtrCompanionApp` | State-based screen router — no NavController (see ADR-001) |
| `PermissionScreen` | BLE permission explanation + grant/settings buttons |
| `ScanScreen` | BLE device scan controls and device list |
| `PacketLogScreen` | Live hex log with auto-scroll, Export, and Clear |
| `PacketExporter` | Writes log to cache dir; produces `ACTION_SEND` Intent via `FileProvider` |

### `ble-core/`

**Purpose:** All Android Bluetooth API interactions. No UI, no protocol decoding.

**Must not:** know about `PacketLogger`, `PacketAnalyzer`, or Compose.

**Key components:**

| Component | Responsibility |
|-----------|---------------|
| `BleConstants` | Single source of truth for all confirmed BLE UUIDs and timing constants |
| `RtrScanner` | BLE LE scan with name-prefix filtering; `StateFlow<ScanState>` output |
| `RtrGattManager` | Full GATT lifecycle: connect → discover services → enable notifications → receive packets |
| `ConnectionState` | Sealed class lifecycle: Disconnected / Connecting / Connected / DiscoveringServices / Ready / Error |
| `ScanState` | Sealed class: Idle / Scanning / Stopped / Failed |
| `RtrDevice` | Simple value object: address, name, RSSI |

### `protocol/`

**Purpose:** Protocol-level data structures and analysis pipeline. Technology-agnostic.

**Must not:** contain Android BLE API calls or Compose imports.

**Key components:**

| Component | Responsibility |
|-----------|---------------|
| `RawPacket` | Raw byte container: `bytes`, `timestamp`, computed `hex` string |
| `PacketLogger` | 500-entry ring buffer; `StateFlow<List<RawPacket>>` for UI |
| `PacketAnalyzer` | Sprint 5 stub: `analyze()` → always null until format is confirmed |
| `ParsedPacket` | Sealed class placeholder — empty until Sprint 5 |

---

## Data Flow

### BLE Notification Path

```
RTR 310 peripheral
      │
      │  BLE GATT notification on CHAR_NOTIFY (0x5354)
      ▼
RtrGattManager.BluetoothGattCallback.onCharacteristicChanged()
      │  handlePacket(ByteArray)
      │  scope.launch { _packetFlow.emit(bytes) }
      ▼
MainViewModel  (collects packetFlow in init block)
      │  packetLogger.record(RawPacket(bytes))
      ▼
PacketLogger._log (MutableStateFlow<List<RawPacket>>)
      │  StateFlow exposed via MainViewModel.packetLog
      ▼
PacketLogScreen  (collectAsStateWithLifecycle)
      │
      ▼
User taps Export → MainActivity.exportPacketLog()
      │  PacketExporter.createShareIntent()
      ▼
Android share sheet
```

### BLE Connection Path

```
User taps device card in ScanScreen
      │  MainViewModel.connect(RtrDevice)
      ▼
MainViewModel resolves BluetoothDevice from address
      │  gattManager.connect(btDevice)
      ▼
RtrGattManager.connect()
      │  device.connectGatt(autoConnect=false, TRANSPORT_LE)
      ▼
onConnectionStateChange(STATE_CONNECTED)
      │  delay 600ms (SERVICE_DISCOVERY_DELAY_MS)
      │  gatt.discoverServices()
      ▼
onServicesDiscovered()
      │  log service UUIDs
      │  enableNotifications(gatt)  ← CCCD descriptor write only
      ▼
onDescriptorWrite() success
      │  _connectionState = Ready(device, serviceIds)
      ▼
RtrCompanionApp routes to PacketLogScreen
```

### Permission Flow

```
MainActivity.onCreate()
      │  requestBlePermissions()
      ▼
ContextCompat.checkSelfPermission() for each required permission
      │  all granted → viewModel.onPermissionsGranted()
      │  missing     → permissionLauncher.launch(missing)
      ▼
onPermissionsGranted()  →  permissionState = GRANTED  →  ScanScreen
onPermissionsDenied()   →  permissionState = DENIED   →  PermissionScreen
```

---

## Compose Screen Routing

```
RtrCompanionApp (root composable)
      │
      ├── permissionState == DENIED  ────────────►  PermissionScreen
      │
      ├── connectionState is Ready   ────────────►  PacketLogScreen
      │
      └── else                       ────────────►  ScanScreen
```

No `NavController` is used in Sprint 1–3. See ADR-001 for reasoning.

---

## Threading Model

- BLE callbacks arrive on arbitrary system threads.
- All state mutations happen via `scope.launch {}` inside `viewModelScope` (Dispatchers.Main).
- `PacketLogger` mutations are on the coroutine collector thread (Main by default).
- `RtrScanner` timeout uses `delay()` in the provided `CoroutineScope`.
- No raw `Thread` or `Handler` usage in the codebase.

---

## Future Interfaces (Sprint 5+)

The following interfaces are reserved for future implementation. Do NOT add logic until
packet formats are confirmed from real capture data.

| Interface | Module | Sprint |
|-----------|--------|--------|
| `PacketParser` | `protocol` | 5 |
| `PacketRepository` | `protocol` | 5 |
| `ParsedPacket` subtypes | `protocol` | 5 |
| `PacketStorage` (SQLite/Room) | `app` or new module | 6 |
| WRITE command API | `ble-core` | Post-beta |
