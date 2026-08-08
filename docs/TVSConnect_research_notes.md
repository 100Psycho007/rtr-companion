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

1. `ApacheSpeedOMeter1`
2. `ApacheSpeedOMeter2`
3. `ApacheSpeedOMeter3`
4. `ApacheSpeedOMeter4`
5. `ApacheSpeedOMeter5`
6. `ApacheClusterDataReceiver`
7. `ApacheBleService`
8. `ApacheMobileToCluster`

## Working rule for future notes

When documenting protocol findings:

- Put verified facts first
- Put hypotheses in a separate section
- Keep the official APK findings distinct from local capture findings
- Do not merge code from other TVS platforms into Apache/RTR notes unless the class name or parser path is explicitly Apache/RTR-related
