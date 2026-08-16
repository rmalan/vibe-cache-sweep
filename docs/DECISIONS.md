# CacheSweep Architecture & Product Decisions

This file records important decisions that should survive individual coding-agent sessions.

Use this document when a decision:

* affects architecture,
* affects security,
* affects privacy,
* changes product behavior,
* changes platform compatibility,
* contradicts or refines the original specification,
* would be expensive to repeatedly reconsider.

Do not record minor implementation details here.

---

# Decision Status

Use:

* **Accepted** — current project direction.
* **Proposed** — under consideration.
* **Superseded** — replaced by a later decision.
* **Rejected** — considered and intentionally not used.

---

# D-001 — Build the application incrementally

**Status:** Accepted

## Decision

CacheSweep will be implemented in explicit phases rather than asking a coding agent to build the entire product in one session.

Development order:

1. Technical feasibility.
2. Production scanner.
3. Cleaner.
4. Cleanup coordinator.
5. Product UI.
6. Hardening/release.

## Reason

Android storage privileges, OEM behavior, Shizuku behavior, and package-manager cache operations need real-device validation.

Building the complete application before validating these assumptions creates unnecessary implementation risk.

---

# D-002 — Repository is the source of truth

**Status:** Accepted

## Decision

Coding-agent conversation history must not be required to continue development.

Persistent context lives in:

* `AGENTS.md`
* `docs/PRD.md`
* `docs/TECH_SPEC.md`
* `docs/ROADMAP.md`
* `docs/STATUS.md`
* `docs/DECISIONS.md`
* Git history
* current source code

## Reason

Agent sessions may hit context/token limits or be replaced by new sessions.

A new agent must be able to continue development by reading the repository.

---

# D-003 — Kotlin + Jetpack Compose

**Status:** Accepted

## Decision

Primary application implementation will use:

* Kotlin
* Jetpack Compose
* Material 3

## Reason

This is a modern native Android application and Compose provides an appropriate UI model for the project.

---

# D-004 — Android 11+ initial support

**Status:** Accepted

## Decision

Initial minimum Android target is Android 11 / API 30.

## Reason

This matches the intended personal-device use case and provides a practical baseline for Shizuku's Wireless Debugging workflow.

Lower Android versions are out of scope for v1.

---

# D-005 — Use Shizuku rather than requiring root

**Status:** Accepted

## Decision

The primary privileged backend will use Shizuku running with ADB/shell privileges.

Root must not be required for CacheSweep v1.

## Reason

The application is intended for the owner's personal device but should avoid modifying/rooting the Android installation when shell-level package-manager functionality is sufficient.

A future root backend may be considered separately.

---

# D-006 — Privileged operations use a narrow UserService API

**Status:** Accepted

## Decision

Privileged operations will be isolated behind a Shizuku UserService with a small strongly typed AIDL/API surface.

Allowed API concepts include operations such as:

* query backend UID,
* query capabilities,
* clear cache for a validated package,
* request global cache trimming.

The privileged interface must not expose:

```text
execute(command)
```

or equivalent arbitrary shell functionality.

## Reason

This limits the security impact of the Shizuku privilege bridge and makes dangerous operations easier to audit and test.

---

# D-007 — Never expose arbitrary shell execution

**Status:** Accepted

## Decision

CacheSweep will not contain a terminal, shell command input, remote command execution, deep-link shell execution, or general-purpose privileged executor exposed to application-facing code.

## Reason

CacheSweep needs a very small set of known storage operations.

General shell execution is unnecessary and substantially expands risk.

---

# D-008 — Never intentionally clear complete application data

**Status:** Accepted

## Decision

CacheSweep is a cache cleaner, not an application reset utility.

It must never intentionally run the equivalent of:

```text
pm clear PACKAGE
```

without the cache-only option.

## Safety invariant

Any package-manager clear operation must include:

```text
--cache-only
```

when that mechanism is used.

Command generation must have automated tests enforcing this invariant.

---

# D-009 — Avoid `sh -c`

**Status:** Accepted

## Decision

Privileged package operations must pass command arguments individually rather than constructing shell command strings interpreted by:

```text
sh -c
```

## Reason

This avoids shell interpretation/injection and keeps package-name handling predictable.

---

# D-010 — Capability detection beats Android-version assumptions

**Status:** Accepted

## Decision

Selective cleaning and global trimming will be enabled based on runtime capability detection.

Do not assume a command exists merely because the device reports a particular Android API level.

## Reason

AOSP behavior, Android releases, and OEM implementations may differ.

The application should measure actual capability.

---

# D-011 — Selective cache cleaning is capability-gated

**Status:** Accepted

## Decision

When the runtime package manager exposes a safe cache-only package operation, CacheSweep may offer selective per-app cleaning.

Expected conceptual operation:

```text
pm clear --user <USER_ID> --cache-only <PACKAGE>
```

If this capability does not exist or fails validation, selective cleaning must be disabled.

## Reason

Selective cleanup gives the user precise control but must not be simulated or implemented by unsafe private-directory deletion.

---

# D-012 — Global cache trimming is the fallback

**Status:** Accepted

## Decision

When selective package cleaning is unavailable, CacheSweep may offer Android's global cache trimming operation.

Conceptually:

```text
pm trim-caches <DESIRED_FREE_SPACE>
```

## Constraint

If the user requested selective cleaning and the capability disappears, CacheSweep must not silently switch to global cleaning.

The user must explicitly approve the broader operation.

---

# D-013 — Use StorageStatsManager for per-app storage estimates

**Status:** Accepted

## Decision

Application cache/app/data usage will be measured using Android's storage statistics APIs where available.

Usage Access will be requested for this purpose.

## Reason

CacheSweep should use Android's supported statistics model rather than attempting to traverse other applications' private directories.

---

# D-014 — Use physical filesystem free space for before/after result

**Status:** Accepted

## Decision

Primary before/after cleanup measurement will use physical available filesystem storage, such as `StatFs`.

`StorageStatsManager` cache totals will be maintained as a separate reported-cache metric.

## Reason

Reported/reclaimable cache and physically available storage are different concepts and can update at different times.

The UI must not present them as identical.

---

# D-015 — Do not promise estimated cache equals reclaimable storage

**Status:** Accepted

## Decision

UI copy should use terms such as:

* reported cache,
* estimated cache,
* cache currently reported.

It must not say that the full reported value is guaranteed to be reclaimed.

## Reason

Android ultimately determines what cache can be removed and running applications may regenerate data.

---

# D-016 — Single Gradle application module for v1

**Status:** Accepted

## Decision

CacheSweep will begin as one Android application module with logical package boundaries.

Potential areas:

```text
ui/
domain/
data/
scanner/
storage/
cleaner/
shizuku/
permissions/
model/
util/
```

## Reason

The project is currently small enough that physical multi-module architecture would add overhead without clear benefit.

Modules may be split later if justified.

---

# D-017 — Manual dependency injection initially

**Status:** Accepted

## Decision

Use a lightweight application container/manual dependency injection for early development rather than immediately introducing Hilt.

## Reason

The application has a relatively small dependency graph.

This keeps the technical spike simple while retaining interfaces/testability.

This decision may be revisited if dependency complexity grows significantly.

---

# D-018 — No network dependency

**Status:** Accepted

## Decision

CacheSweep v1 will not require:

* backend services,
* accounts,
* cloud sync,
* analytics,
* advertising,
* remote configuration.

The application should not request Android's `INTERNET` permission.

## Reason

The utility can operate entirely locally and storage/package information is sensitive enough that unnecessary network capability should be avoided.

---

# D-019 — Scanning must survive individual package failures

**Status:** Accepted

## Decision

Failure to read storage statistics for one installed package must not fail the complete device scan.

Results should record:

* attempted package count,
* successful package count,
* partial failures where relevant.

## Reason

System packages and OEM packages can behave differently.

Partial information is more useful than an all-or-nothing scan.

---

# D-020 — Use bounded scanner concurrency

**Status:** Accepted

## Decision

Per-package StorageStats queries should execute with bounded concurrency rather than:

* sequentially querying every app, or
* launching hundreds of simultaneous operations.

Initial target:

```text
4–8 concurrent queries
```

Exact value may be tuned after physical-device profiling.

---

# D-021 — Cleaning may continue after individual package failure

**Status:** Accepted

## Decision

During multi-package selective cleanup, failure to clean one package should normally not terminate the remaining cleanup plan.

The final result should report:

* attempted apps,
* successfully cleaned apps,
* failed apps.

## Reason

One protected/system package should not prevent cleanup of unrelated packages.

---

# D-022 — No fake progress indicators

**Status:** Accepted

## Decision

Use determinate progress only where real progress is known.

Examples:

Valid:

```text
12 of 37 packages
```

Invalid:

```text
67% cache cleaned
```

when Android provides no byte-level progress.

Use indeterminate progress for global trimming or storage-stat settling where actual completion percentage is unavailable.

---

# D-023 — Agent tasks should be restartable

**Status:** Accepted

## Decision

Each coding-agent work session should leave the repository in a state where another fresh session can continue.

Before ending meaningful work, the agent should:

1. build the project,
2. run relevant tests,
3. update `STATUS.md`,
4. update this file if architecture changed,
5. document blockers,
6. document the exact next task.

## Reason

Agent context limits must not threaten project continuity.

---

# D-024 — Git commits serve as implementation checkpoints

**Status:** Accepted

## Decision

Meaningful working increments should be committed independently.

Recommended examples:

```text
build: initialize Android project

feat: add usage access detection

feat: add storage stats scanner

feat: add Shizuku connection manager

feat: add cache-only capability probe

test: enforce safe cache clear commands
```

Phase completion should also be tagged.

## Reason

Git gives both humans and agents a reliable history and rollback mechanism.

---

# New Decision Template

Copy this section when adding a significant decision.

---

# D-XXX — Decision title

**Status:** Proposed / Accepted / Superseded / Rejected

## Context

What problem or uncertainty caused this decision?

## Decision

What are we doing?

## Reason

Why is this the preferred approach?

## Consequences

What becomes easier, harder, enabled, or restricted?

## Supersedes

If applicable:

`D-XXX`
