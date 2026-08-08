## Summary

<!-- Describe what this PR changes and why. -->

## Architecture Impact

<!-- Does this change module boundaries, add dependencies, or alter data flow?
     If none, write "None." -->

## BLE Impact

<!-- Does this add, modify, or remove any BLE operation (scan, connect, write, read, notify)?
     If this PR adds a writeCharacteristic call: STOP — an ADR and BLE_WRITE_AUDIT update are required first.
     If none, write "None." -->

## Testing

<!-- How was this tested?
     - Unit test? Which test class?
     - Hardware test? Which device? What was observed?
     - Manual UI test? Which scenarios? -->

## Documentation Updated

<!-- Check all that apply: -->
- [ ] `README.md` updated (if feature-facing)
- [ ] `PROJECT_STATE.md` updated
- [ ] `docs/KNOWN_FACTS.md` updated (if BLE/protocol observation)
- [ ] `docs/BLE-Protocol.md` updated (if protocol observation)
- [ ] `docs/ROADMAP.md` updated (if sprint tasks changed)
- [ ] `docs/sessions/Session-NNN.md` created/updated
- [ ] `docs/adr/ADR-NNN.md` created (if architecture decision made)
- [ ] `docs/security/BLE_WRITE_AUDIT.md` re-run (if any write operation touched)
- [ ] No documentation update needed

## Screenshots

<!-- For UI changes, include before/after screenshots.
     For BLE changes, include Logcat output showing the expected behaviour.
     Delete this section if not applicable. -->

## Checklist

- [ ] Code follows the conventions in `CONTRIBUTING.md`
- [ ] All public classes and functions have KDoc
- [ ] No hardcoded strings, magic numbers, or UUIDs outside `BleConstants.kt`
- [ ] CI passes (`assembleDebug` + `testDebugUnitTest`)
- [ ] No writes to `CHAR_WRITE` (0x5352) without an approved ADR
- [ ] Branch is up to date with `develop`
