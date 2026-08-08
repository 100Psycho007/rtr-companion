# Security Policy

## Safety-First Approach

RTR Companion interacts with the BLE peripheral of a moving vehicle. Sending incorrect
or unverified commands to the bike could have unpredictable consequences. This document
defines the safety rules that govern all BLE operations.

---

## Why WRITE Operations Are Disabled

The `CHAR_WRITE` characteristic (0x5352) is the channel through which the phone sends
commands to the bike. During Sprint 1–3, **no data is ever sent to this characteristic**.

**Reasons:**

1. The RTR 310 packet format is not yet documented. Sending unknown bytes to a vehicle
   ECU could trigger unknown behaviour.
2. Safety is non-negotiable for a vehicle-connected application.
3. The project's engineering principle is: **observe first, send later**.

WRITE operations will be enabled only after:
- The full packet format for each command is documented in `docs/BLE-Protocol.md`
- An ADR is written and approved for each new command type
- The command is verified on a stationary bike with no rider

---

## CCCD Descriptor Writes

The only write operations performed during Sprint 1–3 are **CCCD descriptor writes**
on `CHAR_NOTIFY` (0x5354). These are the standard Android BLE mechanism for enabling
notifications and are completely safe:

- They do not send any data to the bike's application layer.
- They tell the peripheral to begin sending notification data to the phone.
- They are required for passive monitoring — the app cannot receive any packets without them.

See `docs/security/BLE_WRITE_AUDIT.md` for the formal audit confirming that zero
characteristic writes exist in the codebase.

---

## Protocol Safety

| Rule | Rationale |
|------|-----------|
| Never write to `CHAR_WRITE` without a confirmed, documented format | Vehicle safety |
| CCCD writes on `CHAR_NOTIFY` are permitted | Required for passive operation |
| Every new write command requires an ADR | Ensures review before any send |
| The packet format must be confirmed from captured data, not guessed | Data integrity |
| Never fabricate packet documentation | Research honesty |

---

## BLE Write Audit

A formal audit of all write operations is maintained at:
`docs/security/BLE_WRITE_AUDIT.md`

This document must be re-run whenever:
- Any `writeCharacteristic`, `writeDescriptor`, or `setValue` call is added or modified
- A new BLE characteristic is interacted with
- Sprint 5+ begins implementing write commands

---

## Testing Methodology

All BLE testing is performed with the bike **stationary** and the rider **not present**.

Hardware tests are logged in `docs/sessions/` with:
- The bike state at time of test
- Any anomalous responses received
- Confirmation that no write commands were sent

---

## Responsible Reverse Engineering

This project follows responsible disclosure principles:

1. **Passive-first** — observe traffic before sending any commands.
2. **Document everything** — all observations go into `docs/KNOWN_FACTS.md` with strict
   confirmed/hypothesis/rejected categorisation.
3. **No speculation as fact** — nothing is moved to Confirmed without direct evidence.
4. **No cloud API investigation** — the TVS cloud/server API is out of scope.
5. **No credentials captured** — if any authentication exchange is observed, it is
   logged as opaque bytes without attempting to extract or reuse credentials.

---

## Reporting Issues

If you find a security concern in this project (e.g., a write operation added without
an ADR, a hardcoded credential, or an unsafe pattern), open a GitHub issue using the
`bug_report` template and include the word SECURITY in the title.
