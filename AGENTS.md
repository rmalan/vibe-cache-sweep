# CacheSweep Agent Instructions

## Source of truth

Read these files before making implementation decisions:

1. `docs/PRD.md`
2. `docs/TECH_SPEC.md`

The PRD defines product behavior and scope.
The technical specification defines architecture and implementation constraints.

If they conflict:
- Safety requirements win.
- Then TECH_SPEC.md.
- Then PRD.md.

## Product

CacheSweep is a personal Android cache-cleaning utility.

Stack:
- Kotlin
- Jetpack Compose
- Android 11+
- Shizuku
- StorageStatsManager
- Coroutines / Flow

## Critical safety constraints

Never execute:

`pm clear PACKAGE`

unless `--cache-only` is explicitly present.

Never expose arbitrary shell execution.

Never use user-controlled strings to construct shell commands.

Never use `sh -c`.

Never intentionally delete application data.

Never add root as a requirement.

Never add INTERNET permission.

Do not add analytics, telemetry, ads, or cloud services.

## Development strategy

Do not build the entire application at once.

Work in this order:

### Phase 0 — Technical feasibility spike

Implement only enough code to verify:

1. Shizuku connection works.
2. Shizuku permission works.
3. Privileged UID can be obtained.
4. `pm` capability detection works.
5. `pm clear --cache-only PACKAGE` support can be detected.
6. `pm trim-caches` support can be detected.
7. StorageStatsManager can read per-app cache usage.
8. Selective cache cleaning preserves app data.

Create a diagnostic Compose screen for these tests.

Do not proceed to polished UI until the feasibility spike is working.

### Phase 1 — Scanner

Implement:
- Usage Access detection
- package enumeration
- StorageStats
- cache aggregation
- app list
- sorting
- search

### Phase 2 — Cleaner

Implement:
- Shizuku UserService
- AIDL
- capability detection
- selective cache cleaning
- global trimming fallback
- error handling

### Phase 3 — Cleanup coordinator

Implement:
- before snapshot
- clean operation
- post-clean settling
- rescan
- result calculation
- partial failures

### Phase 4 — Product UI

Implement the screens described in the PRD.

## Engineering rules

Prefer small testable classes.

Keep privileged operations behind:

`CacheCleaner`

UI and ViewModels must never construct shell commands.

Use strongly typed APIs between app and privileged service.

Add tests for every command builder.

Any code capable of producing a plain `pm clear PACKAGE` command is considered a critical defect.

Run tests/build after meaningful changes.

Do not silently change architecture described in TECH_SPEC.md.

If a spec assumption does not work on the target Android version/device:
1. document the failure,
2. implement a capability-gated fallback,
3. do not fake support.

## First deliverable

Build the Phase 0 technical spike.

Before moving beyond Phase 0, report:

- files created
- architecture used
- build status
- tests run
- Shizuku implementation status
- StorageStats implementation status
- capability probe implementation
- anything requiring validation on a physical Android device

## Project continuity

The chat/session is NOT the source of truth.

The repository is the source of truth.

Before starting work:

1. Read `docs/PRD.md`
2. Read `docs/TECH_SPEC.md`
3. Read `docs/ROADMAP.md`
4. Read `docs/STATUS.md`
5. Read `docs/DECISIONS.md`
6. Inspect recent git commits

After every meaningful task:

- update `docs/STATUS.md`
- mark completed checklist items
- record the current task
- record blockers
- record test/build status

After every architectural decision:

- update `docs/DECISIONS.md`

Before ending a session:

- ensure the project builds
- run relevant tests
- update STATUS.md
- commit working changes
- document the exact next action

Never rely on conversation history to continue implementation.
