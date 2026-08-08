---
name: Protocol Discovery
about: Report a new BLE packet observation or protocol finding
title: "[PROTOCOL] "
labels: protocol-research
assignees: ''
---

## Discovery Summary

<!-- One sentence: what did you find? -->

## Evidence

<!-- How was this observed? nRF Connect? HCI log? RTR Companion packet export?
     Attach capture files if available. -->

## Packet Data

<!-- Paste raw packet bytes. Format: one packet per line, space-separated hex.
     Example: AA BB CC DD 01 00 FF -->

```
(paste here)
```

## Observation Conditions

- **Bike state:** (e.g. engine on / off, moving / stationary, ride mode)
- **Observation time:** (e.g. 30s after connecting, immediately on connect)
- **Observed pattern:** (e.g. packet repeats every 1s, only on ignition ON)

## Hypothesis

<!-- What do you think this packet means?
     Be clear this is a hypothesis — do not present speculation as fact.
     It will be added to docs/KNOWN_FACTS.md under Hypothesis until confirmed. -->

## Is This Confirmed?

- [ ] Yes — I have multiple independent observations confirming this
- [ ] No — This is a hypothesis based on limited data

## Capture File

<!-- Is the raw capture file attached or available?
     Captures belong in the captures/ directory. -->

- [ ] Capture file attached / available
- [ ] No capture file — observation was manual only
