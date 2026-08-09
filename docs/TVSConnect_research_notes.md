# TVS Connect / RTR 310 BLE Reverse-Engineering Findings

> **Status:** Research snapshot compiled from the official TVS SmartXonnect APK analysis and RTR 310 BLE captures.
>
> **Scope:** Apache/RTR-related BLE protocol, incoming frame parsing, dashboard data flow, outgoing write path, and current capture observations.
>
> **Important:** Confirmed APK facts, observations from real-bike captures, and hypotheses are kept separate. Do not treat unconfirmed capture packet types as official Apache frames.

---

## 1. Project Goal

The goal of this research is to understand how the official TVS SmartXonnect application communicates with the Apache/RTR cluster over Bluetooth Low Energy, so that an independent RTR Companion application can reproduce useful communication required for a custom dashboard.

The intended architecture is:

```text
RTR 310 Cluster
      ↕ BLE
RTR Companion
      ├── incoming frame parser
      ├── live vehicle state
      ├── ride data
      └── outgoing commands/navigation
```

---

## 2. Apache BLE Connection Configuration

The official APK contains an Apache-specific BLE configuration:

`com.tvs.bike.core.bikes.ApacheBLEConnectionConfig`

The Apache bike variant explicitly uses this configuration.

### Confirmed values

| Property | Value |
|---|---|
| Type name | `Apache` |
| Service UUID | `5456534D-5647-5341-5342-454E544F5251` |
| Read UUID | `00005354-0000-1000-8000-00805F9B34FB` |
| Write UUID | `00005352-0000-1000-8000-00805F9B34FB` |
| Characteristic write type | `2` |
| Auto-connect | `false` |

The read/write characteristics therefore appear to be:

```text
Service:
5456534D-5647-5341-5342-454E544F5251

Read:
00005354-0000-1000-8000-00805F9B34FB

Write:
00005352-0000-1000-8000-00805F9B34FB
```

---

## 3. Apache Incoming Frame Identification

The official APK contains:

`com.tvs.bike.core.protocol.apache.ApacheIncomingFrameIdentifier`

Its `getFrameConfig()` maps the Apache incoming frame IDs as follows:

| Data ID | Official TVS frame type | Model |
|---|---|---|
| `0x10` | `SPEEDOMETER_DATA_5A_10_FRAME` | `ApacheSpeedOMeter1` |
| `0x11` | `SPEEDOMETER_DATA_5A_11_FRAME` | `ApacheSpeedOMeter2` |
| `0x12` | `SPEEDOMETER_DATA_5A_12_FRAME` | `ApacheSpeedOMeter3` |
| `0x16` | `SPEEDOMETER_DATA_5A_16_FRAME` | `ApacheSpeedOMeter4` |
| `0x18` | `SPEEDOMETER_DATA_5A_18_FRAME` | `ApacheSpeedOMeter5` |
| `0x29` | `WIFI_PASSWORD_5A_29_FRAME` | `ApacheBasicData` |

This is one of the strongest pieces of evidence that these frame IDs are part of the official Apache protocol implementation.

---

## 4. BtData

Class:

`com.tvs.bike.core.protocol.BtData`

`BtData` is the raw frame/base model rather than the actual field decoder.

It:

- stores the raw `byte[]`
- exposes it using `getData()`
- defines abstract `getStartByte()`
- defines abstract `getDataId()`
- defines abstract `setStartByte(int)`
- defines abstract `setDataId(int)`
- validates the first byte
- accepts `0x5A` and signed `0x9B` as valid start values in the shown validation logic

This class does not determine individual field offsets or scaling.

---

## 5. ParsingMeta

Annotation:

`com.tvs.bike.core.annotations.ParsingMeta`

Definition:

```java
FrameParameterType frameType();

int length() default 1;

int max() default Integer.MAX_VALUE;

int min() default 0;

int start();
```

### Meaning

| Parameter | Meaning |
|---|---|
| `frameType` | Field's `FrameParameterType` |
| `start` | Start position; used as 1-based by `DataParser` |
| `length` | Number of bytes |
| `min` | Declared minimum |
| `max` | Declared maximum |

Important observation:

**The generic `DataParser` code we inspected does not use `min()` or `max()` during extraction.**

---

## 6. FrameParameterType

Class:

`com.tvs.bike.core.protocol.FrameParameterType`

This enum contains the field vocabulary used by TVS's frame models.

Important Apache/RTR-related values include:

```text
START_BYTE
DATA_ID
SPEED
ODOMETER
FUEL_LEVEL
FUEL_SENSOR
AVERAGE_SPEED
ECONOMY_STATUS
TOP_SPEED
THROTTLE_POSITION
BACKLIGHT
LOCATION_TAG
SWITCH_STATUS
TIME_60KMPH
AVERAGE_MILEAGE
TRIP_F_METER
RESERVED
ENGINE_RPM
CHECKSUM
END_BYTE
VEHICLE_STATE
VEHICLE_STATE_1
VEHICLE_STATE_2
VEHICLE_STATE_3
SERVICE_REMINDER
BATTERY_VOLTAGE
SOFTWARE_VERSION
VEHICLE_DIAGNOSTICS
VEHICLE_DIAGNOSTICS_2
VEHICLE_DIAGNOSTICS_1
TURN_INDICATOR_STATUS
ENGINE_TEMPRATURE
INTAKE_AIR_TEMPRATURE
FUEL_INJECTION_TIME
GEAR_POSITION
LEAN_ANGLE
CRUISING_RANGE
ACCELERATION
TORQUE
WHEEL_ANGEL_OFFSET
TRIP_DISTANCE
TRIP_TIME_HR
TRIP_TIME_MIN
TRIP_MILEAGE
TRIP_FUEL
LAP_TIME
LAP_NUMBER
BEST_LAP
LAP_TRIGGER
MILLEAGE
CURRENT_STATUS
FRAME_NO
DND_STATE
CALL_MUTE_STATE
VEHICLE_LOCK_STATE
TRUNK_STATE
INCOMING_CALL_STATE
MUSIC_COMMAND
DIALER_BYTE_3
DIALER_BYTE_4
DIALER_BYTE_5
DIALER_BYTE_6
DIALER_BYTE_7
DIALER_BYTE_8
DIALER_BYTE_9
DIALER_BYTE_10
DIALER_BYTE_11
DIALER_BYTE_12
BATTERY_PERCENTAGE
D2E
SELECTED_TRIP
TRIP_A_DISTANCE
TRIP_A_MILEAGE
TRIP_A_SPEED
TRIP_A_AVG_SPEED
TRIP_B_DISTANCE
TRIP_B_MILEAGE
TRIP_B_SPEED
TRIP_B_AVG_SPEED
OVERSPEED_THRESHOLD
MIL_BLINK_CODE
VEHICLE_MODE
ISG_BLINK_CODE
CONNECTOR_STATUS
ISS_DURATION
OTA_STATUS
AVERAGE_FUEL_ECONOMY
INSTANTANEOUS_FUEL_ECONOMY
DISTANCE_TO_EMPTY
ISS_COUNT
ENGINE_LOAD
ACCUMULATED_FUEL_INJECTION_TIME
MANIFOLD_AIR_PRESSURE
BAROMETRIC_PRESSURE
ENGINE_RUNNING_TIME
DISTANCE_TRAVELED
FUEL_INJECTION_VOLUME
CALIBRATION_COMMAND
CALIBRATION_ID
DATA_LENGTH
CALIBRATION_DATA_1
CALIBRATION_DATA_2
CALIBRATION_DATA_3
CALIBRATION_DATA_4
TRIP_STATUS
VEHICLE_MODEL
CAPTURE_SCREENSHOT
OVERSPEED_SETTING
AVERAGR_MILEAGE
RUN_TIME_SINCE_ENGINE_START
TELL_TALE_STATUS
SCREEN_MATRIX
ABS_MIL_BLINK_CODE
BYTE_1
BYTE_2
BYTE_3
BYTE_4
BYTE_5
BYTE_6
BYTE_7
BYTE_8
BYTE_9
BYTE_10
BYTE_11
BYTE_12
BYTE_13
BYTE_14
BYTE_15
BYTE_16
BYTE_17
BYTE_18
```

---

## 7. DataParser — Actual Generic Decoder

Class: `com.tvs.bike.core.protocol.DataParser`

This was a key discovery because it reveals how TVS interprets `ParsingMeta`.

### Parsing flow

```text
BtData raw byte[]
        ↓
DataParser
        ↓
find fields carrying @ParsingMeta
        ↓
read start + length
        ↓
start - 1
        ↓
extract byte range
        ↓
convert bytes to hexadecimal string
        ↓
parse hex as base-16 integer
        ↓
assign value to model field
```

### Annotated fields

`DataParser.collectAnnotatedFields()` scans declared fields and retains fields with:

```java
field.getAnnotation(ParsingMeta.class) != null
```

### Start indexing

The parser explicitly performs:

```text
actualIndex = start - 1
```

Therefore:

**`ParsingMeta.start` is 1-based.**

### Multi-byte extraction

The parser uses `copyOfRange()` to obtain the requested byte range.

`extractValue()` then formats every byte as a two-digit hexadecimal value, concatenates them, and parses the resulting string using base 16.

Conceptually:

```text
01 2C
 ↓
"012C"
 ↓
0x012C
 ↓
300
```

### String fields

For `java.lang.String`, the extracted byte array is decoded directly as a string.

### Supported field types

The decompiled parser has paths for:

- `int`
- `long`
- `float`
- `double`
- `String`

The numeric path assigns the extracted integer value.

### Important caution

Do not infer additional byte-order/scaling behavior from `DataParser` alone.

Scaling that we know about is implemented in individual model getters.

---

## 8. Apache 5A 10 — ApacheSpeedOMeter1

Class:

`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter1`

Frame:

```text
5A 10
```

### Field map

| Position | Field | Length |
|---:|---|---:|
| 1 | `startByte` | 1 |
| 2 | `dataId` | 1 |
| 3 | `speed` | 1 |
| 4–6 | `odometer` | 3 |
| 7 | `fuelLevel` | 1 |
| 8 | `averageSpeed` | 1 |
| 9 | `mileage` | 1 |
| 10 | `topSpeed` | 1 |
| 11 | `throttle` | 1 |
| 12 | `locationTag` | 1 |
| 12 | `switchStatus` | 1 |
| 13 | `zeroTo60Time` | 1 |
| 14 | `averageMileageDirect` | 1 |
| 15–16 | `tripFMeter` | 2 |
| 17–18 | `engineRpm` | 2 |
| 19 | `checkSum` | 1 |
| 20 | `endByte` | 1 |

Known scaling:
- odometer getter divides by `10`
- receiver handles zero-to-60 using a `/10` conversion

---

## 9. Apache 5A 11 — ApacheSpeedOMeter2

Class:

`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter2`

Frame:

```text
5A 11
```

### Field map

| Position | Field |
|---:|---|
| 1 | startByte |
| 2 | dataId |
| 3 | vehicleDirection2 |
| 4 | vehicleState1 |
| 5 | serviceReminder |
| 6 | gearPosition |
| 7 | batteryVoltage |
| 8 | softwareVersion |
| 9 | milBlinkCode / reserve1 |
| 10 | vehicleModel / reserve2 |
| 11 | vehicleDiagnostics |
| 12 | reserve3 |
| 13 | turnIndicatorStatus |
| 14 | tellTaleStatus / engineTemperature / reserve14 |
| 15 | screenMatrix / reserve15 |
| 16 | vehicleState3 / reserve16 |
| 17 | absMilBlinkCode / reserve17 |
| 18 | backlightIllumination / vehicleMode |
| 19 | checkSum |
| 20 | endByte |

Battery-voltage getter formats the parsed value to one decimal place.

---

## 10. Apache 5A 12 — ApacheSpeedOMeter3

Class:

`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter3`

Frame:

```text
5A 12
```

### Field map

| Position | Field | Length |
|---:|---|---:|
| 1 | startByte | 1 |
| 2 | dataId | 1 |
| 3 | leanAngle | 1 |
| 4–5 | cruisingRange | 2 |
| 6 | wheelAngelOffset | 1 |
| 7 | acceleration | 1 |
| 8 | torque | 1 |
| 9–10 | tripDistance | 2 |
| 11 | tripTimeHour | 1 |
| 12 | tripTimeMin | 1 |
| 13 | tripMileage | 1 |
| 14–15 | tripFuel | 2 |
| 16 | overspeedThreshold | 1 |
| 17 | overSpeedSetting | 1 |
| 18 | reserve3 | 1 |
| 19 | checkSum | 1 |
| 20 | endByte | 1 |

Known scaling:
- tripDistance getter divides by `10`
- tripFuel getter divides by `10`

---

## 11. Apache 5A 16 — ApacheSpeedOMeter4

Class:

`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter4`

Frame:

```text
5A 16
```

### Field map

| Position | Field |
|---:|---|
| 1 | startByte |
| 2 | dataId |
| 3 | lapTimeMin |
| 4 | lapTimeSec |
| 5 | lapTimeMSec |
| 6 | lapNumber |
| 7 | bestLapMin |
| 8 | bestLapSeconds |
| 9 | bestLapMilliSeconds |
| 10 | bestLapNumber |
| 11 | lapTrigger |
| 16 | reserve1–reserve5 |
| 17 | reserve6 |
| 18 | reserve7 |
| 19 | checkSum |
| 20 | endByte |

The class formats current lap and best lap as `MM:SS:MS` strings.

---

## 12. Apache 5A 18 — ApacheSpeedOMeter5

Class:

`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter5`

Frame:

```text
5A 18
```

### Field map

| Position | Field | Length |
|---:|---|---:|
| 1 | startByte | 1 |
| 2 | dataId | 1 |
| 3 | engineLoad | 1 |
| 4–5 | accumulatedFuelInjectionTime | 2 |
| 6 | manifoldAirPressure | 1 |
| 7 | barometricPressure | 1 |
| 8 | intakeAirTemperature | 1 |
| 9 | engineTemperature | 1 |
| 10–11 | fuelInjectionTime | 2 |
| 12 | batteryVoltage | 1 |
| 13–14 | runTimeSinceEngineStart | 2 |
| 15–16 | distanceTraveled | 2 |
| 17–18 | fuelInjectionVolume | 2 |
| 19 | checkSum | 1 |
| 20 | endByte | 1 |

Known scaling:
- batteryVoltage getter multiplies the parsed value by `0.1` and formats it to two decimal places

---

## 13. ApacheClusterDataReceiver

Class: `com.tvs.ble.feature.datareceiver.ApacheClusterDataReceiver`

This is the official Apache incoming-data receiver.

It maintains the Apache parsed frame models and raw frame buffers.

Important methods found include:

```text
parseData(...)
handleSpeedOMeter(...)
setSpeedometerDataByte5A10(...)
updateData(...)
sendDataToDashboard(...)
```

### 5A 10 handling

`setSpeedometerDataByte5A10()` uses:

- speed
- mileage
- fuel level
- location tag
- zero-to-60 time
- engine RPM
- checksum
- average mileage direct
- switch status
- throttle
- top speed
- odometer

It also computes or updates:

- average speed
- ride start odometer
- current ride top speed
- acceleration / deceleration
- voice-assist flag
- vehicle moved state

### Other frames

`5A 12`:
- cruising range
- trip distance
- trip time
- trip mileage
- trip fuel
- lean angle
- wheelie-angle offset
- acceleration
- torque
- overspeed threshold
- overspeed setting

`5A 16`:
- lap trigger
- lap time
- best lap
- lap number

`5A 18`:
- engine load
- accumulated fuel-injection time
- manifold air pressure
- barometric pressure
- intake air temperature
- engine temperature
- fuel-injection time
- battery voltage
- runtime since engine start
- distance traveled
- fuel-injection volume

---

## 14. Apache BLE Service and cyclic write path

Class: `com.tvs.ble.feature.bikewiseblehelpers.ApacheBleService`

The Apache BLE service owns the Apache mobile-to-cluster sender and the cyclic write loop.

Confirmed service methods:

- `sendInitialDataPacket()`
- `sendRiderName()`
- `writeCyclicDataPacket()`
- `connectBle()`

### Cyclic write chain

The base service starts a timer every 2 seconds, and Apache overrides the cyclic write hook:

```text
BaseBleConnectHelperService.startSendingCyclicData()
        ↓
TimerTask every 2000 ms
        ↓
writeCyclicDataPacket()
        ↓
ApacheBleService override
        ↓
ApacheMobileToCluster.sendMobileData()
```

This is confirmed by the base service and Apache override.

### Confirmed timing

- `startSendingCyclicData()` schedules the timer immediately with `0 ms` initial delay.
- The repeating interval is `2000 ms`.

---

## 15. ApacheMobileToCluster

Class: `com.tvs.ble.feature.datasender.ApacheMobileToCluster`

This class is the main Apache outgoing-data builder.

Confirmed entry points:

- `sendMobileData()`
- `sendRiderName(fullName)`
- `sendNavigationInstructions(...)`
- `sendManuverString(...)`
- `sendCustomTextLineTwo(...)`
- `sendDestReachedCustomText(...)`
- `sendBirthDate(...)`
- `calibrationWriteRequest(...)`
- `updateVehicleSettings(...)`

### Important send paths

- `sendMobileData()` calls `BlePacketsProvider.sendApacheMobileData(...)`.
- `sendRiderName(fullName)` creates a rider-name packet.
- `sendNavigationInstructions(...)` / `sendManuverString(...)` use checksum/encryption helpers before the BLE write.
- `updateVehicleSettings(...)` uses the Apache vehicle-control packet path.
- `sendBirthDate(...)` and `calibrationWriteRequest(...)` directly construct 20-byte packets with Apache-specific prefixes.

### Final BLE handoff

The service ultimately writes bytes through the BLE engine/write layer.

---

## 16. BlePacketsProvider — Apache outgoing packet construction

Class: `com.tvs.bluetooth.core.utils.BlePacketsProvider`

This is where the Apache outgoing packet is actually assembled.

### Confirmed Apache packet builder

`sendApacheMobileData(Context, byte, IceSettingsModel, BikeType)` builds the periodic Apache mobile packet.

Confirmed packet prefix/suffix:

```text
[0x5B, 0x4A, ...payload..., 0xFF]
```

This is the packet used by the Apache cyclic write path.

### Observed payload contents

The payload includes the following data sources:

- battery / ICE battery level conversion
- network strength
- overspeed limit (defaults to `120` if not enabled or not provided)
- missed-call count
- current call state
- voice-assist status
- date and time fields
- AM/PM flag
- bike-type-specific layout differences
- zero / reserved bytes

### Confirmed layout behavior

The method branches depending on `BikeType`, building slightly different payload lengths/field positions before padding to a 20-byte packet.

### Other confirmed builder methods in the same class

- `sendApacheVehicleControl(...)`
- `byteArrayForFistConnection(...)`
- `byteArrayForPictogram(...)`
- `byteArrayForUnLock(...)`
- `returnMobilePacket(...)`
- `returnValidMobilePacket(...)`
- `returnMobilePacketWithACK(...)`
- `returnContactInfoPacket(...)`
- `returnMusicPacket(...)`
- `returnPlayPauseStateApache(...)`
- `returnVIN(...)`

---

## 17. Apache vehicle-control packet

`BlePacketsProvider.sendApacheVehicleControl(...)` builds the Apache control packet.

Confirmed behavior:

- starts with `0x5A, 0xF1`
- ends with `0xFF`
- includes illumination state selection
- includes speedo value
- includes `tslOnOff`
- includes `clearOrSowingCallInfo`
- includes date bytes

This is the packet used for vehicle control settings.

---

## 18. Apache rider-name / text / navigation packets

`ApacheMobileToCluster` and `BlePacketsProvider` also support packets used for text and navigation-style messages.

Known confirmed packet builders include:

- rider name
- custom text line two
- destination reached text
- navigation instructions/manoeuvre string
- birth date
- pictogram packets

Some of these use checksum helpers or encryption helpers before being written to the cluster.

---

## 19. BLE write path and timer behavior

The base Apache service chain is:

```text
initializeDeviceServices()
        ↓
sendInitialDataPacket()
        ↓
startSendingCyclicData()
        ↓
Timer every 2000 ms
        ↓
writeCyclicDataPacket()
        ↓
ApacheMobileToCluster.sendMobileData()
        ↓
BlePacketsProvider.sendApacheMobileData()
        ↓
BLE write
```

This means the cyclic Apache mobile packet is not random: it is sent on a repeating 2-second cadence after service initialization.

---

## 20. Crypto-related Apache symbols

The APK contains Apache-specific encryption/decryption symbols such as:

- `encryptDataApache`
- `decryptDataForApache`
- `getKeyByteArrayApache`
- `getKeyByteArrayForApache`

This suggests there is an Apache-specific crypto layer in the official app.

The exact algorithm, key/IV details, and which frames use it are not yet fully confirmed.

---

## 21. Confirmed from our RTR BLE captures

Our captures showed Apache-like frames that matched the official frame identifiers for several frame types.

At minimum, the captured `5A 10`, `5A 11`, and `5A 12` frames line up with the official TVS mapping above.

---

## 22. Important caution

The following were observed during reverse-engineering, but are not yet confirmed as RTR 310-specific by the official code:

- `5A 5F`
- `5A 7D`
- `5B 42`

Do not label these as Apache speedometer frames until we find an official mapping or parser logic for them.

---

## 23. What we can build now

The incoming/read-only side is sufficiently understood to start a prototype.

A first implementation can contain:

```text
BLE scanner
   ↓
Apache service discovery
   ↓
connect using Apache service UUID
   ↓
subscribe/read using Apache read UUID
   ↓
receive raw bytes
   ↓
frame identification
   ↓
5A10 / 5A11 / 5A12 / 5A16 / 5A18 parsing
   ↓
typed RTR vehicle state
   ↓
live dashboard
```

---

## 24. What remains before full feature parity

### High priority

1. Fully decode `ApacheMobileToCluster` payload variants
2. Trace outgoing calls into the BLE write layer
3. Determine outgoing packet structure for all command types
4. Determine crypto use and exact implementation
5. Validate write-side behavior safely
6. Reconcile any capture interpretations with the official 20-position model layout
7. Identify the meaning of `5A 5F`, `5A 7D`, and `5B 42`

---

## 25. Recommended build phases

### Phase 1 — Read-only prototype
- BLE scanning
- Apache service discovery
- notification subscription
- frame parser
- 5A10/11/12/16/18
- live vehicle state
- dashboard

### Phase 2 — Real-bike validation
Compare against the official app/cluster:
- speed
- RPM
- fuel
- odometer
- throttle
- gear
- battery voltage
- trip data
- engine/diagnostic data

### Phase 3 — Outgoing communication
After `ApacheMobileToCluster` is fully understood:
- outgoing frame construction
- required crypto
- navigation
- phone/music controls
- other non-destructive cluster features

### Phase 4 — Full companion
- ride logging
- trip statistics
- navigation
- cluster controls
- local storage
- polished dashboard
- reconnect/error handling

---

## 26. Current status

| Area | Status |
|---|---|
| Apache BLE service UUID | Confirmed |
| Apache read UUID | Confirmed |
| Apache write UUID | Confirmed |
| Incoming IDs 10/11/12/16/18/29 | Confirmed from APK |
| ParsingMeta | Confirmed |
| DataParser | Confirmed |
| 5A10 layout | Confirmed from APK |
| 5A11 layout | Confirmed from APK |
| 5A12 layout | Confirmed from APK |
| 5A16 layout | Confirmed from APK |
| 5A18 layout | Confirmed from APK |
| Receiver/dashboard mapping | Confirmed from APK |
| ApacheMobileToCluster | Confirmed and traced |
| ApacheBleService cyclic write | Confirmed |
| Cyclic interval | Confirmed at 2000 ms |
| Apache mobile cyclic packet | Confirmed as `5B 4A ... FF` |
| Apache vehicle control packet | Confirmed as `5A F1 ... FF` |
| Apache crypto symbols | Confirmed to exist |
| Exact crypto algorithm | Not yet confirmed |
| Outgoing command protocol | Not yet fully decoded |
| 5A5F meaning | Unconfirmed |
| 5A7D meaning | Unconfirmed |
| 5B42 meaning | Unconfirmed |
| Read-only prototype | Ready to start |
| Full two-way companion | Not yet |

---

## 27. Immediate next action

Search the official APK for:

```text
writeCharacteristicToCentral
```

Then open the implementation that actually performs the BLE GATT write.

That is the remaining bridge between the decoded protocol packets and a standalone RTR 310 companion implementation.

---

## 28. Research rules

- Keep official APK findings separate from live capture observations.
- Keep hypotheses explicitly labeled.
- Do not infer an Apache frame from another TVS platform merely because it looks similar.
- Do not re-investigate ApacheSpeedOMeter1–5 unless new evidence contradicts the documented mapping.
- Validate packet length against raw BLE notification boundaries.
- Prefer direct code evidence over assumptions from packet appearance.
- Record every confirmed field, scaling rule, and command in this document.

---

## Summary

The official TVS Apache incoming BLE protocol is mapped far enough to begin a read-only RTR Companion implementation.

We have also now confirmed the Apache cyclic write loop, the cyclic timer interval, and the Apache mobile/control packet builders.

The biggest remaining unknown for a full two-way implementation is the exact write-layer implementation and any remaining command-specific payloads.
