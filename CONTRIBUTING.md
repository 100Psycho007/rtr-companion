# Contributing to RTR Companion

Thank you for your interest in contributing to RTR Companion. This document explains
how the project is organised and how to contribute effectively.

---

## Code of Conduct

Be respectful, constructive, and professional. This is a research and engineering project.
Speculation presented as fact, unsafe BLE operations, or fabricated protocol data will
not be accepted.

---

## Before You Start

1. Read `README.md` to understand the project scope.
2. Read `docs/KNOWN_FACTS.md` to understand what is confirmed vs hypothesised.
3. Read `docs/SECURITY.md` before touching any BLE write code.
4. Check `PROJECT_STATE.md` to understand the current sprint and what is in progress.

---

## Coding Conventions

### Language and Libraries

- Kotlin only. No Java.
- Jetpack Compose for all UI.
- Material 3 components and theming.
- `StateFlow` / `SharedFlow` for all observable state.
- Kotlin Coroutines for async work.
- Timber for all logging.

### Architecture Rules

- BLE API calls belong in `ble-core` only. Never in `app` or `protocol`.
- Protocol data structures belong in `protocol`. Never raw Android BLE types.
- Compose and ViewModel code belongs in `app` only.
- `ble-core` must not import from `app`.

### Style

- KDoc on every public class and every public function.
- BLE callbacks should explain their lifecycle position in their KDoc.
- Packet-handling code should explain its invariants.
- No magic numbers — use named constants in `BleConstants.kt`.
- No hardcoded strings in UI — string resources for user-facing text.
- `@SuppressLint("MissingPermission")` is acceptable on BLE classes where
  caller responsibility is documented in KDoc.

---

## Documentation Rules

### When to Update Documentation

| Change | Required doc updates |
|--------|---------------------|
| Any BLE observation | `docs/KNOWN_FACTS.md`, `docs/BLE-Protocol.md` |
| Architecture decision | `docs/adr/ADR-NNN.md` |
| New session / work period | `docs/sessions/Session-NNN.md` |
| Sprint changes | `PROJECT_STATE.md`, `docs/ROADMAP.md` |
| New BLE write added | `docs/security/BLE_WRITE_AUDIT.md` re-run required |
| New feature | `README.md` Features table |

### KNOWN_FACTS.md Rules

- **Confirmed** — only experimentally verified facts. Direct observation from hardware.
- **Hypothesis** — plausible based on indirect evidence. Clearly labelled.
- **Rejected** — tested and proven false. Never deleted — kept for reference.
- Never move an entry between categories without direct evidence.
- Never add packet format details without a corresponding capture file in `captures/`.

---

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: short description of new feature
fix: short description of bug fix
docs: update BLE-Protocol.md with session observations
refactor: extract helper method
test: add unit tests for PacketLogger ring buffer
chore: update dependency versions
```

Use the **imperative mood** in the subject line ("add", not "added" or "adds").

Keep the subject line under 72 characters. Add a body if the change needs explanation.

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, always builds. PRs only. |
| `develop` | Integration branch for sprint work. |
| `feature/*` | Feature development off `develop`. |
| `fix/*` | Bug fixes off `develop` (or `main` for hotfixes). |
| `docs/*` | Documentation-only changes. |
| `sprint/N-*` | Sprint-scoped work branches. |

---

## Pull Request Process

1. Branch from `develop` for features and documentation.
2. Fill in the PR template completely.
3. All CI checks must pass before merge.
4. BLE-related PRs require the `docs/security/BLE_WRITE_AUDIT.md` to be current.
5. Protocol-related PRs require references to capture files in `captures/`.
6. Squash-merge is preferred to keep history readable.

---

## Review Process

- At least one reviewer must approve before merge.
- Code reviewers should specifically check:
  - KDoc completeness on public API
  - No BLE writes without ADR and audit
  - No protocol speculation presented as fact
  - Architecture layer boundaries respected

---

## Reporting Security Issues

Do not open a public issue for security vulnerabilities. Instead:
1. Open a GitHub issue with `[SECURITY]` in the title.
2. Describe the issue without including exploit details publicly.
3. The maintainer will respond within 48 hours.
