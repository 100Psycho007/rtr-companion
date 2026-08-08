# TVS Connect / RTR Apache BLE Research Notes

This document captures what we have confirmed from the official TVS Connect APK and from our RTR 310 BLE captures. Keep confirmed facts separate from hypotheses.

## Confirmed from the official TVS Connect APK

### Apache BLE connection configuration
The Apache variant is explicitly wired to its own BLE config class:

- `com.tvs.bike.core.BikeVariants.Apache.getConnectionConfiguration()` returns `new ApacheBLEConnectionConfig()`.
- `ApacheBLEConnectionConfig` reports:
  - `getTypeName()` -> `Apache`
  - `getServiceUUID()` -> `5456534D-5647-5341-5342-454E544F5251`
  - `getReadUUID()` -> `00005354-0000-1000-8000-00805F9B34FB`
  - `getWriteUUID()` -> `00005352-0000-1000-8000-00805F9B34FB`
  - `getCharacteristicsWriteType()` -> `2`
  - `shouldAutoConnect()` -> `false`

### Apache frame identification
`com.tvs.bike.core.protocol.apache.ApacheIncomingFrameIdentifier` maps the Apache incoming frames to official frame types:

- `0x10` -> `DataFrameType.SPEEDOMETER_DATA_5A_10_FRAME`
- `0x11` -> `DataFrameType.SPEEDOMETER_DATA_5A_11_FRAME`
- `0x12` -> `DataFrameType.SPEEDOMETER_DATA_5A_12_FRAME`
- `0x16` -> `DataFrameType.SPEEDOMETER_DATA_5A_16_FRAME`
- `0x18` -> `DataFrameType.SPEEDOMETER_DATA_5A_18_FRAME`
- `0x29` -> `DataFrameType.WIFI_PASSWORD_5A_29_FRAME`

### Apache `ParsingMeta` and parser behavior
`com.tvs.bike.core.annotations.ParsingMeta` defines the metadata used by frame models:

- `frameType()`
- `start()`
- `length()` default `1`
- `min()` default `0`
- `max()` default `Integer.MAX_VALUE`

`com.tvs.bike.core.protocol.DataParser` is the generic parser that processes these annotations:

- It scans declared fields and keeps only those annotated with `@ParsingMeta`.
- It reads `start` and `length` from the annotation.
- `start` is 1-based in the annotation; the parser converts it to `start - 1` for array indexing.
- It extracts the selected bytes, formats each byte as two uppercase hex characters, concatenates them, and converts the result with `Integer.parseInt(hexString, 16)`.
- The parser supports fields whose type name is `int`, `long`, `float`, `double`, or `java.lang.String`.
- For `String`, it decodes the raw byte slice directly to a string.
- `min` and `max` exist in the annotation, but this parser does not use them.

`com.tvs.bike.core.protocol.BtData` is the raw-frame base class:

- It stores the original `byte[]` unchanged.
- It exposes the data via `getData()`.
- It validates the start byte and logs an invalid start-byte warning when needed.
- It does not itself perform field parsing.

### Apache `FrameParameterType`
`com.tvs.bike.core.protocol.FrameParameterType` contains the field labels used across the TVS protocol, including:

- `START_BYTE`
- `DATA_ID`
- `SPEED`
- `ODOMETER`
- `FUEL_LEVEL`
- `AVERAGE_SPEED`
- `TOP_SPEED`
- `THROTTLE_POSITION`
- `LOCATION_TAG`
- `SWITCH_STATUS`
- `TIME_60KMPH`
- `AVERAGE_MILEAGE`
- `TRIP_F_METER`
- `ENGINE_RPM`
- `CHECKSUM`
- `END_BYTE`
- `VEHICLE_STATE`
- `SERVICE_REMINDER`
- `BATTERY_VOLTAGE`
- `SOFTWARE_VERSION`
- `TURN_INDICATOR_STATUS`
- `ENGINE_TEMPRATURE`
- `INTAKE_AIR_TEMPRATURE`
- `FUEL_INJECTION_TIME`
- `GEAR_POSITION`
- `LEAN_ANGLE`
- `CRUISING_RANGE`
- `ACCELERATION`
- `TORQUE`
- `WHEEL_ANGEL_OFFSET`
- `TRIP_DISTANCE`
- `TRIP_TIME_HR`
- `TRIP_TIME_MIN`
- `TRIP_MILEAGE`
- `TRIP_FUEL`
- `LAP_TIME`
- `LAP_NUMBER`
- `BEST_LAP`
- `LAP_TRIGGER`
- `ENGINE_LOAD`
- `ACCUMULATED_FUEL_INJECTION_TIME`
- `MANIFOLD_AIR_PRESSURE`
- `BAROMETRIC_PRESSURE`
- `ENGINE_RUNNING_TIME`
- `DISTANCE_TRAVELED`
- `FUEL_INJECTION_VOLUME`
- `RUN_TIME_SINCE_ENGINE_START`
- `TELL_TALE_STATUS`
- `SCREEN_MATRIX`
- `ABS_MIL_BLINK_CODE`
- `BYTE_1` through `BYTE_18`

### Apache `5A 10` field map
`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter1` shows the parsing layout for the `5A 10` frame:

- `startByte` at position `1`
- `dataId` at position `2`
- `speed` at position `3`
- `odometer` at positions `4..6` with length `3`
- `fuelLevel` at position `7`
- `averageSpeed` at position `8`
- `mileage` at position `9`
- `topSpeed` at position `10`
- `throttle` at position `11`
- `locationTag` at position `12`
- `switchStatus` also mapped at position `12`
- `zeroTo60Time` at position `13`
- `averageMileageDirect` at position `14`
- `tripFMeter` at positions `15..16` with length `2`
- `engineRpm` at positions `17..18` with length `2`
- `checkSum` at position `19`
- `endByte` at position `20`

The getter for odometer divides the stored value by `10`.

### Apache `5A 11` field map
`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter2` shows the parsing layout for the `5A 11` frame:

- `startByte` at position `1`
- `dataId` at position `2`
- `vehicleDirection2` at position `3`
- `vehicleState1` at position `4`
- `serviceReminder` at position `5`
- `gearPosition` at position `6`
- `batteryVoltage` at position `7`
- `softwareVersion` at position `8`
- `milBlinkCode` / `reserve1` at position `9`
- `vehicleModel` / `reserve2` at position `10`
- `vehicleDiagnostics` at position `11`
- `reserve3` at position `12`
- `turnIndicatorStatus` at position `13`
- `tellTaleStatus` / `engineTemperature` / `reserve14` at position `14`
- `screenMatrix` / `reserve15` at position `15`
- `vehicleState3` / `reserve16` at position `16`
- `absMilBlinkCode` / `reserve17` at position `17`
- `backlightIllumination` / `vehicleMode` at position `18`
- `checkSum` at position `19`
- `endByte` at position `20`

The battery voltage getter formats the parsed value to one decimal place.

### Apache `5A 12` field map
`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter3` shows the parsing layout for the `5A 12` frame:

- `startByte` at position `1`
- `dataId` at position `2`
- `leanAngle` at position `3`
- `cruisingRange` at positions `4..5` with length `2`
- `wheelAngelOffset` at position `6`
- `acceleration` at position `7`
- `torque` at position `8`
- `tripDistance` at positions `9..10` with length `2`
- `tripTimeHour` at position `11`
- `tripTimeMin` at position `12`
- `tripMileage` at position `13`
- `tripFuel` at positions `14..15` with length `2`
- `overspeedThreshold` at position `16`
- `overSpeedSetting` at position `17`
- `reserve3` at position `18`
- `checkSum` at position `19`
- `endByte` at position `20`

The getters for trip distance and trip fuel divide the stored value by `10`.

### Apache `5A 16` field map
`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter4` shows the parsing layout for the `5A 16` frame:

- `startByte` at position `1`
- `dataId` at position `2`
- `lapTimeMin` at position `3`
- `lapTimeSec` at position `4`
- `lapTimeMSec` at position `5`
- `lapNumber` at position `6`
- `bestLapMin` at position `7`
- `bestLapSeconds` at position `8`
- `bestLapMilliSeconds` at position `9`
- `bestLapNumber` at position `10`
- `lapTrigger` at position `11`
- `reserve1` through `reserve5` at position `16`
- `reserve6` at position `17`
- `reserve7` at position `18`
- `checkSum` at position `19`
- `endByte` at position `20`

The class formats current lap and best lap as `MM:SS:MS` strings.

### Apache `5A 18` field map
`com.tvs.bike.core.protocol.apache.ApacheSpeedOMeter5` shows the parsing layout for the `5A 18` frame:

- `startByte` at position `1`
- `dataId` at position `2`
- `engineLoad` at position `3`
- `accumulatedFuelInjectionTime` at positions `4..5` with length `2`
- `manifoldAirPressure` at position `6`
- `barometricPressure` at position `7`
- `intakeAirTemperature` at position `8`
- `engineTemperature` at position `9`
- `fuelInjectionTime` at positions `10..11` with length `2`
- `batteryVoltage` at position `12`
- `runTimeSinceEngineStart` at positions `13..14` with length `2`
- `distanceTraveled` at positions `15..16` with length `2`
- `fuelInjectionVolume` at positions `17..18` with length `2`
- `checkSum` at position `19`
- `endByte` at position `20`

The battery voltage getter multiplies the parsed value by `0.1` and formats it to two decimal places.

### Apache receiver pipeline
We also found:

- `com.tvs.ble.feature.datareceiver.ApacheClusterDataReceiver.parseData(byte[], Function2<byte[], DataFrameType, Unit>)`
- `com.tvs.bike.core.protocol.apache.ApacheIncomingFrameIdentifier`
- `com.tvs.ble.feature.datareceiver.BaseClusterDataReceiver.parseData(byte[], Function1<com.tvs.bike.core.protocol.BtData, Unit>)`

This confirms that the official app has a dedicated Apache incoming-frame identification and parsing path.

### Apache receiver handling of `5A 10`
`ApacheClusterDataReceiver.setSpeedometerDataByte5A10()` copies the parsed `ApacheSpeedOMeter1` values into the app state.

The method uses the following values from `ApacheSpeedOMeter1`:

- `speed`
- `mileage`
- `fuelLevel`
- `locationTag`
- `zeroTo60Time`
- `engineRpm`
- `checkSum`
- `averageMileageDirect`
- `switchStatus`
- `throttle`
- `topSpeed`
- `odometer`

It also computes or updates:

- average speed
- ride start odometer
- current ride top speed
- acceleration / deceleration
- voice-assist flag
- vehicle moved state

### Apache receiver handling of the other official frames
The receiver also updates app state from the other official Apache frames:

- `5A 12` -> cruising range, trip distance, trip time, trip mileage, trip fuel, lean angle, wheelie-angle offset, acceleration, torque, overspeed threshold, overspeed setting
- `5A 16` -> lap trigger, lap time, best lap, lap number
- `5A 18` -> engine load, accumulated fuel injection time, manifold air pressure, barometric pressure, intake air temperature, engine temperature, fuel injection time, battery voltage, run time since engine start, distance traveled, fuel injection volume

### Apache-specific protocol classes present in the APK
The APK contains Apache-specific classes and helpers including:

- `ApacheBleService`
- `ApacheClusterDataReceiver`
- `ApacheIncomingFrameIdentifier`
- `ApacheSpeedOMeter1`
- `ApacheSpeedOMeter2`
- `ApacheSpeedOMeter3`
- `ApacheSpeedOMeter4`
- `ApacheSpeedOMeter5`
- `ApacheClusterNavigationData`
- `ApacheMobileToCluster`
- `ApacheBaseClusterProvider`
- `ApacheUtils`

### Apache crypto-related symbols present in the APK
The APK contains Apache-specific encryption/decryption symbols such as:

- `encryptDataApache`
- `decryptDataForApache`
- `getKeyByteArrayApache`
- `getKeyByteArrayForApache`

This suggests there is an Apache-specific crypto layer in the official app.

## Confirmed from our RTR BLE captures

Our captures showed Apache-like frames that matched the official frame identifiers for several frame types.

At minimum, the captured `5A 10`, `5A 11`, and `5A 12` frames line up with the official TVS mapping above.

## Important caution

The following were observed during reverse-engineering, but are not yet confirmed as RTR 310-specific by the official code:

- `5A 5F`
- `5A 7D`
- `5B 42`

Do not label these as Apache speedometer frames until we find an official mapping or parser logic for them.

## Next target files/classes

1. `ApacheClusterDataReceiver`
2. `ApacheBleService`
3. `ApacheMobileToCluster`
4. `ApacheSpeedOMeter1` byte layout and getter scaling
5. `DataParser` and `FrameParameterType` for how annotated fields are converted from raw bytes

## Working rule for future notes

When documenting protocol findings:

- Put verified facts first
- Put hypotheses in a separate section
- Keep the official APK findings distinct from local capture findings
- Do not merge code from other TVS platforms into Apache/RTR notes unless the class name or parser path is explicitly Apache/RTR-related
