# Session 004 — First Hardware Capture & Checksum Analysis

## Goal

Analyse the first real hardware capture (shutdown burst, ignition OFF) and update
protocol docs from actual byte-level evidence instead of Jupiter-derived assumptions.

## Work Completed

### Capture Analysed
- `captures/rtr-capture-20260808-150945.txt` — 21 packets, captured via RTR Companion
  app while bike was parked/ignition OFF
- Confirmed all packets are 20 bytes, terminated with `0xFF`
- Grouped into 6 distinct (frame type, message ID) pairs: `5A/10`, `5A/11`, `5A/12`,
  `5A/5F`, `5A/7D`, `5B/42`

### Structural Corrections to `KNOWN_FACTS.md`
- Packet length corrected: 19 → **20 bytes**
- Payload range corrected: bytes 2–16 → **bytes 2–17** (byte 17 changes in the `0x5F`
  pair, so it isn't a fixed/reserved byte)
- Checksum candidate position corrected: byte 17 → **byte 18**
  (found by diffing the two `0x11` variants — the byte that moves in lockstep with
  the one changed data byte is index 18, not 17 as originally assumed)

### Checksum Algorithm — Investigated
- **`0x11` pair:** clean result. `sum(bytes[0:18])` changes by `0x20`, byte 18 changes
  by `0x20` in the opposite direction. Consistent with a simple
  `checksum = (C − sum(bytes[0:18])) % 256`.
- **`0x5F` pair:** does NOT fit the same formula. Sum diff was 342 (mod 256 = 86),
  but the checksum diff was 134 — no simple additive relationship.
- Brute-forced every `(start, end)` sum range (start 0–4, end up to 18) crossed with
  additive/subtractive constants 0–255, against all 21 packets at once:
  **zero formulas satisfied every packet.**
- Tested XOR-based checksum on the `0x5F` pair: no match.
- Tested 5 common CRC-8 polynomials (0x07, 0x31, 0x1D, 0x9B, 0x2F; init 0x00 and
  0xFF) on the `0x5F` pair: no match.
- **Conclusion:** the checksum is not a single global formula applied identically
  to every message ID (or `0x5F` has an extra hidden input, e.g. a frame counter).
  Downgraded from "verified" to "partially verified" in `KNOWN_FACTS.md`.

## Files Changed

- `docs/KNOWN_FACTS.md` — packet structure and checksum sections corrected
- `docs/sessions/Session-004.md` — created (this file)

## Problems Encountered

- The Jupiter-derived checksum formula (`255 - sum(bytes[0..17]) % 256`) does not
  reproduce RTR 310 values. The RTR 310 protocol shares UUIDs and framing with
  Jupiter but the checksum is not a drop-in match.
- Only one capture session exists, taken entirely at ignition OFF. `0x5F` (the one
  dynamic message ID seen) only has 2 distinct value-sets to compare, which isn't
  enough to isolate its checksum formula with confidence.

## Testing Performed

- Python-based packet parser + grouping script over the full capture
- Brute-force search over checksum formula space (sum range × constant × mode)
- XOR-checksum test
- CRC-8 test (5 common polynomials × 2 init values)

## Next Session Goals

- Capture a live-ride session (ignition ON) to get many more `0x5F` variants —
  needed to properly solve its checksum via single-byte-diff pairs
- Try to capture the `0x9A`/`0xF2` auth challenge on connect, to confirm/deny the
  Jupiter-shared AES key hypothesis
- Decode `0x7D` (fully packed, static) and `0x12` (static) — likely device identity
  or config, not yet attempted
- Once checksum is solved for at least one message type, wire it into
  `PacketAnalyzer.kt`
