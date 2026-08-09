# TVS Connect Apache/RTR BLE — Outgoing Write Path Addendum

> This addendum records the findings discovered after the original `TVSConnect_research_notes.md` was written. It exists so the already-documented Apache incoming protocol is not duplicated or accidentally overwritten.

## 1. Apache cyclic transmission

The official Apache service has:

```text
com.tvs.ble.feature.bikewiseblehelpers.ApacheBleService.writeCyclicDataPacket():void
```

Its implementation is effectively:

```java
@Override
public void writeCyclicDataPacket() {
    getApacheMobileToCluster().sendMobileData();
}
```

The cyclic sender is driven by the base BLE service's timer. The timer starts with a `0 ms` initial delay and repeats every `2000 ms`.

Therefore:

```text
BLE device services initialized
        ↓
startSendingCyclicData()
        ↓
0 ms initial run
        ↓
writeCyclicDataPacket()
        ↓
repeat every 2 seconds
```

For Apache:

```text
ApacheBleService.writeCyclicDataPacket()
        ↓
ApacheMobileToCluster.sendMobileData()
```

This 2-second interval is confirmed from the inspected APK code.

## 2. ApacheMobileToCluster — confirmed outgoing entry points

Class:

`com.tvs.ble.feature.datasender.ApacheMobileToCluster`

Discovered methods:

```text
calibrationWriteRequest()
sendBirthDate(int, int, int, int, int)
sendCustomTextLineTwo(String)
sendDestReachedCustomText(String)
sendFollowMeHeadLampTimer(int)
sendIdleStartStopTimer(int)
sendMobileData()
sendMobileData5(int, String)
sendRiderName(String)
updateVehicleSettings(IceSettingsModel, int, int)
writeData(byte[])
```

This class is the Apache mobile → cluster packet-building/dispatch layer.

## 3. Regular cyclic mobile-data packet — confirmed

`ApacheMobileToCluster.sendMobileData()` delegates packet construction to:

```text
com.tvs.bluetooth.core.utils.BlePacketsProvider.sendApacheMobileData(...): byte[]
```

The packet builder produces the regular Apache mobile-data packet.

Confirmed characteristics from the decompiled implementation:

- starts with `0x5B 0x4A`
- final output is a 20-byte Apache mobile-data packet
- terminates with `0xFF`
- contains phone/application state including battery/network state, overspeed setting, call/missed-call information, voice-assist state, date, time, and AM/PM
- overspeed defaults to `120` and may be replaced by the configured setting
- packet construction has bike-type-specific branches

Important distinction:

```text
5A 10 / 5A 11 / 5A 12 / 5A 16 / 5A 18
    = incoming cluster → phone frames

5B 4A ... FF
    = outgoing phone → cluster cyclic mobile-data packet
```

Do not merge the two directions.

## 4. Known Apache outgoing command paths

### Rider name

```text
ApacheMobileToCluster.sendRiderName(String)
        ↓
BluetoothUtil.createMobileToClusterArray(..., 0x52)
        ↓
writeData(byte[])
```

The `0x52` command association is confirmed from the inspected method.

### Custom text line two

```text
ApacheMobileToCluster.sendCustomTextLineTwo(String)
        ↓
BluetoothUtil.createMobileToClusterArray(..., 0x4C)
        ↓
writeData(byte[])
```

### Destination reached custom text

`sendDestReachedCustomText(String)` uses an Apache custom-text packet builder with command-related values including `0x40` and `0x4C`.

The complete payload layout is not yet documented.

### Birth date

`sendBirthDate(...)` directly constructs an Apache packet beginning with:

```text
5B 7E
```

The complete field layout still needs to be recorded from the method.

### Calibration

`calibrationWriteRequest()` directly constructs an Apache calibration request beginning with:

```text
5B 73 08
```

and uses a 20-byte packet structure ending with `FF`.

### Vehicle settings/control

`updateVehicleSettings(...)` delegates to:

```text
com.tvs.bluetooth.core.utils.BlePacketsProvider.sendApacheVehicleControl(...): byte[]
```

This is a high-priority packet builder to inspect next.

### Follow-me / idle start-stop

The Apache sender exposes:

```text
sendFollowMeHeadLampTimer(int)
sendIdleStartStopTimer(int)
```

These are Apache vehicle-control paths. Exact bytes are not yet documented and must come from their packet-builder implementation.

## 5. Final Apache BLE write path — partially confirmed

The Apache sender's generic write method reaches:

```text
com.tvs.ble.feature.handler.BleEngine.writeCharacteristicToCentral(byte[], boolean)
```

The APK search results also contain the coroutine implementation:

```text
BleEngine$writeCharacteristicToCentral$1$4
```

Its constructor receives:

```text
BleEngine
BluetoothGattCharacteristic
byte[]
Continuation
```

This strongly indicates that the coroutine eventually performs the actual GATT characteristic write.

However, the exact body of `writeCharacteristicToCentral(byte[], boolean)` and the characteristic-selection code have **not yet been inspected**.

Therefore the final transport should currently be represented as:

```text
ApacheMobileToCluster
        ↓
writeData(byte[])
        ↓
BleEngine.writeCharacteristicToCentral(byte[], boolean)
        ↓
[exact GATT write implementation still to inspect]
```

## 6. Known Apache BLE characteristic configuration

From `ApacheBLEConnectionConfig`:

```text
Service UUID:
5456534D-5647-5341-5342-454E544F5251

Read UUID:
00005354-0000-1000-8000-00805F9B34FB

Write UUID:
00005352-0000-1000-8000-00805F9B34FB

Write type:
2
```

This means we already know the intended Apache write characteristic from the official bike configuration. The remaining task is to verify exactly how `BleEngine.writeCharacteristicToCentral()` selects and writes that characteristic.

## 7. Other useful APK search results

Apache-specific or transport-relevant symbols found:

```text
com.tvs.ble.feature.handler.BleEngine.writeCharacteristicToCentral(byte[], boolean)
com.tvs.ble.feature.handler.BleEngine$writeCharacteristicToCentral$1$4
com.tvs.ble.feature.framework.BluetoothWriteCharacteristicDataSourceImpl.writeCharacteristics(byte[], boolean)
com.tvs.bluetooth.layer.peripheral.BluetoothPeripheral.writeCharacteristic(...)
com.tvs.bluetooth.layer.peripheral.BluetoothPeripheral.writeCharacteristicApache$lambda$66(...)
com.tvs.ble.feature.handler.BleEngine.sendApacheNavigationData(Pair<byte[], byte[]>)
com.tvs.ble.feature.handler.BleEngine.updateApacheMissedCall(...)
com.tvs.ble.feature.handler.BleEngine.missedCallStateForApacheVehicle()
com.tvs.ble.feature.utils.TelephonyUtils.writeApacheCallerInformationToCluster(...)
```

## 8. Apache crypto symbols

The APK contains Apache-specific symbols:

```text
encryptDataApache
decryptDataForApache
getKeyByteArrayApache
getKeyByteArrayForApache
```

These prove that an Apache-specific crypto layer exists in the APK, but they do **not** by themselves prove that every outgoing packet is encrypted.

The exact call sites and algorithm remain to be traced.

## 9. Apache navigation path

The APK contains:

```text
com.tvs.ble.feature.handler.BleEngine.sendApacheNavigationData(Pair<byte[], byte[]>)
```

Navigation/manoeuvre paths in `ApacheMobileToCluster` pass through checksum/encryption-related helpers before the final write.

The complete navigation packet structure and crypto transformation are still open research items.

## 10. Apache call/notification paths

The APK contains Apache-specific call handling:

```text
TelephonyUtils.writeApacheCallerInformationToCluster(...)
BleEngine.processIncomingCallState(...)
BleEngine.updateApacheMissedCall(...)
BleEngine.missedCallStateForApacheVehicle()
BleEngine.processingApacheIncomingCallDataToCluster...
```

The regular cyclic `5B 4A` packet also carries call/missed-call-related state.

## 11. Search results that are NOT needed right now

Do not spend time opening these unless we later want cross-platform comparisons:

```text
JupiterBleService
NtorqBleHelper
U347BleHelper
U388BleHelper
U467BleHelper
JupiterMobileToCluster
NtorqMobileToCluster
```

They are other TVS platform implementations.

## 12. Current status after this addendum

### Confirmed

- Apache BLE service UUID
- Apache read characteristic UUID
- Apache write characteristic UUID
- Apache write type
- incoming Apache frame IDs 10/11/12/16/18/29
- all five Apache speedometer model layouts
- `BtData`
- `ParsingMeta`
- `FrameParameterType`
- `DataParser`
- `ApacheClusterDataReceiver`
- Apache cyclic sender
- cyclic interval = 2 seconds
- `ApacheMobileToCluster`
- regular `5B 4A` outgoing packet exists
- regular cyclic packet is 20 bytes and terminates with `FF`
- rider-name command path
- custom-text command path
- birth-date command path
- calibration command path
- vehicle-control entry point
- final write method entry point

### Still open

- exact body of `BleEngine.writeCharacteristicToCentral(byte[], boolean)`
- exact characteristic selection inside that method
- exact checksum algorithm
- exact Apache crypto algorithm/call sequence
- exact `sendApacheVehicleControl()` packet layout
- complete rider-name packet layout
- complete birth-date packet layout
- complete navigation packet layout
- meanings of capture-only `5A 5F`, `5A 7D`, and `5B 42`

## 13. Next exact search order

Do not go back to `ApacheSpeedOMeter1`–`5`; those are already documented.

### Next #1

```text
com.tvs.ble.feature.handler.BleEngine.writeCharacteristicToCentral(byte[], boolean)
```

If the method body delegates to generated coroutine code, inspect:

```text
BleEngine$writeCharacteristicToCentral$1$4
```

### Next #2

```text
com.tvs.bluetooth.core.utils.BlePacketsProvider.sendApacheVehicleControl
```

### Next #3

```text
createMobileToClusterArray
```

### Next #4

```text
addChecksum
```

### Next #5

Search call sites for:

```text
encryptDataApache
decryptDataForApache
getKeyByteArrayApache
getKeyByteArrayForApache
```

### Next #6

```text
BleEngine.sendApacheNavigationData
```

Only after these are mapped should we claim the two-way outgoing protocol is ready for implementation.

## 14. Build readiness

### Read-only companion

**Ready to begin.**

The official APK gives enough information to implement:

```text
BLE scan
 ↓
Apache service discovery
 ↓
read/notify characteristic
 ↓
5A 10 / 11 / 12 / 16 / 18 parsing
 ↓
vehicle state
 ↓
ride/dashboard UI
```

### Full two-way companion

**Not fully ready yet.**

The missing pieces are concentrated in the outgoing transport and command layer rather than the incoming speedometer protocol.

The most important remaining class is now:

```text
BleEngine.writeCharacteristicToCentral
```
