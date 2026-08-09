# TVS Connect / RTR 310 — Confirmed BLE Write Path

## Status

Confirmed from the supplied/decompiled TVS SmartXonnect APK sources. This document records the outgoing Apache/RTR BLE transport path discovered during reverse engineering. It intentionally separates confirmed implementation details from items that still need investigation.

---

## 1. Apache BLE connection configuration

Class: `com.tvs.bike.core.bikes.ApacheBLEConnectionConfig`

Confirmed values:

| Property | Value |
|---|---|
| Type name | `Apache` |
| Service UUID | `5456534D-5647-5341-5342-454E544F5251` |
| Write characteristic UUID | `00005352-0000-1000-8000-00805F9B34FB` |
| Read characteristic UUID | `00005354-0000-1000-8000-00805F9B34FB` |
| Characteristics write type | `2` |
| Auto-connect | `false` |

These values are returned directly by the Apache connection configuration.

---

## 2. BleEngine UUID resolution

`BleEngine` lazily resolves the UUIDs from:

```text
bikeConfig
  -> bikeVariants
      -> connectionConfiguration
          -> getServiceUUID()
          -> getWriteUUID()
          -> getReadUUID()
```

The write UUID is therefore not hardcoded in the write method; it is obtained from the selected bike's connection configuration.

---

## 3. `writeCharacteristicToCentral(byte[], boolean)`

Class:

`com.tvs.ble.feature.handler.BleEngine`

Method:

```text
writeCharacteristicToCentral(byte[] data, boolean isChecksumNeeded)
```

### Confirmed processing order

```text
input byte[]
    |
    +-- if isChecksumNeeded -> BluetoothUtil.addChecksum()
    |
    v
Apache-series check
    |
    +-- Apache U449/U469 -> encryptU449Data()
    |
    +-- other Apache series -> encryptDataApache()
    |
    v
getCharacteristic(serviceUUID, writeCharacteristicUUID)
    |
    v
BluetoothPeripheral.writeCharacteristicApache(..., WITH_RESPONSE)
```

The method logs the data immediately after optional checksum addition and before the Apache encryption step.

For the general Apache-series branch, the code explicitly calls:

```java
bArrAddChecksum = BluetoothUtil.INSTANCE.encryptDataApache(bArrAddChecksum);
```

Then it obtains the GATT characteristic using the configured service and write UUID.

For Apache series it finally calls:

```java
bluetoothPeripheral.writeCharacteristicApache(
    characteristic,
    bArrAddChecksum,
    WriteType.WITH_RESPONSE
);
```

### Important distinction

The byte array reaching `BluetoothPeripheral.writeCharacteristicApache()` is **already transformed** by the checksum/encryption stage when those stages are enabled. `BluetoothPeripheral` itself does not construct the Apache protocol packet.

---

## 4. `BluetoothPeripheral.writeCharacteristicApache(...)`

Method:

```text
com.tvs.bluetooth.layer.peripheral.BluetoothPeripheral.writeCharacteristicApache(
    BluetoothGattCharacteristic characteristic,
    byte[] value,
    WriteType writeType
): boolean
```

Confirmed behavior:

1. Requires an active peripheral connection.
2. Rejects an empty byte array.
3. Checks that the characteristic supports the requested write type.
4. Queues the write operation.
5. The queued Apache write sets the Android characteristic write type.
6. The supplied byte array is passed directly to `BluetoothGattCharacteristic.setValue()`.
7. Android `BluetoothGatt.writeCharacteristic()` performs the actual GATT write.

Conceptually:

```text
Apache encrypted byte[]
        |
        v
BluetoothGattCharacteristic.setWriteType(...)
        |
        v
BluetoothGattCharacteristic.setValue(byte[])
        |
        v
BluetoothGatt.writeCharacteristic(characteristic)
```

There is no additional Apache packet encoding in this lower layer.

---

## 5. Final GATT endpoint

For the Apache configuration, the outgoing write therefore terminates at:

```text
Service UUID:
5456534D-5647-5341-5342-454E544F5251

Write characteristic:
00005352-0000-1000-8000-00805F9B34FB

Write mode used by BleEngine Apache path:
WITH_RESPONSE
```

The configured numeric `getCharacteristicsWriteType()` value is `2`; the Apache branch of `BleEngine.writeCharacteristicToCentral()` explicitly passes `WriteType.WITH_RESPONSE` to `writeCharacteristicApache()`.

---

## 6. Cyclic-data timing already confirmed

`BaseBleConnectHelperService.startSendingCyclicData()` schedules the vehicle-specific `writeCyclicDataPacket()` call with:

```text
initial delay: 0 ms
period: 2000 ms
```

Therefore the cyclic write path runs every **2 seconds** once started.

The base class method is a hook; the Apache-specific implementation is supplied by `ApacheBleService`.

Known Apache chain:

```text
ApacheBleService.writeCyclicDataPacket()
        -> ApacheMobileToCluster.sendMobileData()
        -> outgoing byte[]
        -> BleEngine.writeCharacteristicToCentral(...)
        -> Apache encryption
        -> Apache GATT write
```

The exact construction of every cyclic packet still belongs to the `ApacheMobileToCluster` / packet-provider side and should not be inferred from the transport layer alone.

---

## 7. Incoming Apache frame map already confirmed

`ApacheIncomingFrameIdentifier.getFrameConfig()` maps:

| Data ID | Frame type | Model |
|---:|---|---|
| `0x10` | `SPEEDOMETER_DATA_5A_10_FRAME` | `ApacheSpeedOMeter1` |
| `0x11` | `SPEEDOMETER_DATA_5A_11_FRAME` | `ApacheSpeedOMeter2` |
| `0x12` | `SPEEDOMETER_DATA_5A_12_FRAME` | `ApacheSpeedOMeter3` |
| `0x16` | `SPEEDOMETER_DATA_5A_16_FRAME` | `ApacheSpeedOMeter4` |
| `0x18` | `SPEEDOMETER_DATA_5A_18_FRAME` | `ApacheSpeedOMeter5` |
| `0x29` | `WIFI_PASSWORD_5A_29_FRAME` | `ApacheBasicData` |

These are official APK mappings and should be treated as confirmed protocol evidence.

---

## 8. Generic parsing architecture already confirmed

`BtData` stores the raw frame bytes and provides the common frame-model base.

`ParsingMeta` supplies:

```text
frameType
start
length
min
max
```

`DataParser` discovers fields carrying `@ParsingMeta`, uses `start - 1` as the zero-based extraction index, extracts the requested byte range, and converts numeric bytes into a base-16 integer value.

The generic parser itself does not apply `min`/`max` validation during the extraction code we inspected.

---

## 9. Current reverse-engineering boundary

### Confirmed

- Apache service UUID.
- Apache read characteristic UUID.
- Apache write characteristic UUID.
- Apache BLE write uses `WITH_RESPONSE` in `BleEngine`.
- Optional checksum stage occurs before Apache encryption.
- General Apache-series outgoing data passes through `encryptDataApache()`.
- The encrypted result is sent directly to the Apache write characteristic.
- The lower BLE layer ultimately calls Android `BluetoothGatt.writeCharacteristic()`.
- Cyclic data is scheduled every 2 seconds.
- Official Apache incoming frame IDs `0x10`, `0x11`, `0x12`, `0x16`, `0x18`, and `0x29` are mapped to concrete models.

### Not yet fully confirmed

- The exact implementation of `BluetoothUtil.encryptDataApache()`.
- The exact implementation/algorithm of `BluetoothUtil.addChecksum()` as used by each Apache command.
- The complete raw command-byte construction in `ApacheMobileToCluster` / `BlePacketsProvider` for every outgoing function.
- Whether a particular command requires `isChecksumNeeded = true` or `false`.
- Complete command/response mapping for all RTR 310 features.

Do not fabricate command bytes until these remaining layers have been recovered from the APK or validated against captures.

---

## 10. Next investigation target

**Highest priority:**

```text
com.tvs.bluetooth.core.utils.BluetoothUtil.encryptDataApache(...)
```

Then investigate:

```text
BluetoothUtil.addChecksum(...)
```

and:

```text
com.tvs.ble.feature.datasender.ApacheMobileToCluster.sendMobileData()
```

followed by the relevant `BlePacketsProvider.sendApacheMobileData(...)` implementation.

The goal is to establish:

```text
raw Apache command
    -> checksum rules
    -> Apache encryption
    -> exact bytes written to 00005352...
```

Only after that should the standalone RTR Companion implementation reproduce outgoing commands.
