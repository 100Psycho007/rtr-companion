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

### Apache receiver pipeline
We also found:

- `com.tvs.ble.feature.datareceiver.ApacheClusterDataReceiver.parseData(byte[], Function2<byte[], DataFrameType, Unit>)`
- `com.tvs.bike.core.protocol.apache.ApacheIncomingFrameIdentifier`

This confirms that the official app has a dedicated Apache incoming-frame identification and parsing path.

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

1. `ApacheSpeedOMeter4`
2. `ApacheSpeedOMeter5`
3. `ApacheClusterDataReceiver`
4. `ApacheBleService`
5. `ApacheMobileToCluster`

## Working rule for future notes

When documenting protocol findings:

- Put verified facts first
- Put hypotheses in a separate section
- Keep the official APK findings distinct from local capture findings
- Do not merge code from other TVS platforms into Apache/RTR notes unless the class name or parser path is explicitly Apache/RTR-related
