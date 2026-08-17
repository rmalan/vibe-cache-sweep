# CacheSweep Development Roadmap

**Product:** CacheSweep
**Status:** Pre-implementation
**Source of truth:** `PRD.md` + `TECH_SPEC.md`

---

## How to Use This Roadmap

Each task has a stable ID such as:

`P0-03`

Where:

* `P0` = Phase 0
* `03` = Task 3

Agents should work on a small number of task IDs at a time.

A task is complete only when:

* implementation is finished,
* relevant tests pass,
* the project builds,
* `STATUS.md` is updated.

Do not mark an entire phase complete until its phase gate passes.

---

# Phase 0 — Technical Feasibility

**Goal:** Prove CacheSweep's core Android/Shizuku assumptions before building the full application.

**Phase status:** Complete

## Project foundation

* [x] **P0-01** Create Android Studio project
* [x] **P0-02** Configure Kotlin + Jetpack Compose
* [x] **P0-03** Configure Android SDK versions and Gradle
* [x] **P0-04** Add required dependencies
* [x] **P0-05** Create initial package/project structure
* [x] **P0-06** Create diagnostic Compose screen
* [x] **P0-07** Verify debug APK builds successfully

## Usage Access

* [x] **P0-08** Add `PACKAGE_USAGE_STATS` declaration
* [x] **P0-09** Implement Usage Access detection
* [x] **P0-10** Implement Usage Access settings intent
* [x] **P0-11** Display Usage Access state on diagnostic screen

## Storage statistics

* [x] **P0-12** Implement device storage snapshot using `StatFs`
* [x] **P0-13** Implement installed package enumeration
* [x] **P0-14** Implement basic `StorageStatsManager` package query
* [x] **P0-15** Display cache/app/data size for a test package
* [x] **P0-16** Verify StorageStats behavior on physical device

## Shizuku

* [x] **P0-17** Add Shizuku API/provider dependencies
* [x] **P0-18** Configure Shizuku provider
* [x] **P0-19** Implement Shizuku Binder availability detection
* [x] **P0-20** Implement Shizuku permission request
* [x] **P0-21** Handle Binder received/dead events
* [x] **P0-22** Display Shizuku connection state
* [x] **P0-23** Display privileged UID
* [x] **P0-24** Verify shell UID on physical device

## Privileged backend

* [x] **P0-25** Define minimal AIDL interface
* [x] **P0-26** Implement Shizuku UserService
* [x] **P0-27** Bind application to UserService
* [x] **P0-28** Verify typed privileged call from app process
* [x] **P0-29** Ensure no arbitrary shell API is exposed

## Capability detection

* [x] **P0-30** Implement package-manager capability probe
* [x] **P0-31** Detect `clear --cache-only`
* [x] **P0-32** Detect `trim-caches`
* [x] **P0-33** Display capability results on diagnostic screen
* [x] **P0-34** Cache capabilities for current Shizuku session

## Cache clearing safety test

* [x] **P0-35** Create disposable test application or test fixture
* [x] **P0-36** Generate significant cache in test application
* [x] **P0-37** Store test SharedPreferences value
* [x] **P0-38** Store test database/data value
* [x] **P0-39** Implement safe package cache command builder
* [x] **P0-40** Add test ensuring `--cache-only` is mandatory
* [x] **P0-41** Execute selective cache clear against disposable test app
* [x] **P0-42** Verify cache decreases
* [x] **P0-43** Verify SharedPreferences survive
* [x] **P0-44** Verify database/application data survive

## Global cache trimming

* [x] **P0-45** Implement typed global trim operation
* [x] **P0-46** Test `trim-caches` on physical device
* [x] **P0-47** Record before/after physical storage
* [x] **P0-48** Record before/after reported cache
* [x] **P0-49** Validate global trim fallback behavior

## Phase 0 Gate

Phase 0 is complete only when:

* [x] Project builds
* [x] Usage Access works
* [x] StorageStats returns useful values
* [x] Shizuku connects
* [x] Privileged UID is confirmed
* [x] Capability probe works
* [x] Selective cache clear is proven safe **or explicitly marked unsupported**
* [x] Global trim is proven usable
* [x] No complete app data is deleted
* [x] Findings are recorded in `STATUS.md`
* [x] Architecture deviations are recorded in `DECISIONS.md`


---

# Phase 1 — Production Cache Scanner

**Goal:** Build the production-quality application cache scanner.

**Depends on:** Phase 0

**Phase status:** Complete

## Package discovery

* [x] **P1-01** Implement production package enumeration
* [x] **P1-02** Load application display names
* [x] **P1-03** Load application icons
* [x] **P1-04** Classify user/system apps
* [x] **P1-05** Exclude CacheSweep where appropriate

## StorageStats repository

* [x] **P1-06** Implement production `StorageStatsRepository`
* [x] **P1-07** Handle default/internal storage
* [x] **P1-08** Handle packages whose stats cannot be queried
* [x] **P1-09** Implement per-package cache/app/data model

## Scanner

* [x] **P1-10** Implement `CacheScanner` interface
* [x] **P1-11** Add bounded concurrency
* [x] **P1-12** Add progressive scan state
* [x] **P1-13** Add partial-failure handling
* [x] **P1-14** Calculate aggregate reported cache
* [x] **P1-15** Track successful vs attempted measurements
* [x] **P1-16** Measure scan duration

## User experience

* [x] **P1-17** Build cache application list
* [x] **P1-18** Sort by cache size
* [x] **P1-19** Sort by total size
* [x] **P1-20** Sort alphabetically
* [x] **P1-21** Implement application search
* [x] **P1-22** Implement pull-to-refresh
* [x] **P1-23** Build application detail screen
* [x] **P1-24** Add native Android storage-settings shortcut

## Tests

* [x] **P1-25** Scanner unit tests
* [x] **P1-26** Partial failure tests
* [x] **P1-27** Sorting tests
* [x] **P1-28** Search tests
* [x] **P1-29** Test 300+ application scan if possible

## Phase 1 Gate

* [x] Scanner works independently of Shizuku
* [x] One bad package does not fail the scan
* [x] UI remains responsive
* [x] Cache list can be searched/sorted
* [x] Build and tests pass

---

# Phase 2 — Production Cleaner

**Goal:** Build the safe privileged cache-cleaning backend.

**Depends on:** Phase 0 and Phase 1

**Phase status:** Complete

## Cleaner abstraction

* [x] **P2-01** Implement `CacheCleaner`
* [x] **P2-02** Implement `CleanerCapabilities`
* [x] **P2-03** Implement typed cleanup plans
* [x] **P2-04** Implement typed cleaner errors

## Selective cleaning

* [x] **P2-05** Productionize package cache clear
* [x] **P2-06** Validate package against scanned package set
* [x] **P2-07** Prevent CacheSweep self-clean during operation
* [x] **P2-08** Implement multi-package cleaning
* [x] **P2-09** Implement package-count progress
* [x] **P2-10** Continue after individual package failure
* [x] **P2-11** Report failed packages

## Global fallback

* [x] **P2-12** Productionize global trim
* [x] **P2-13** Calculate desired free-storage target
* [x] **P2-14** Require explicit user consent before selective → global fallback
* [x] **P2-15** Handle unsupported global trim

## Security

* [x] **P2-16** Ensure no `sh -c`
* [x] **P2-17** Ensure no arbitrary command execution
* [x] **P2-18** Test command argument generation
* [x] **P2-19** Test plain `pm clear PACKAGE` cannot be generated
* [x] **P2-20** Audit exported Android components

## Phase 2 Gate

* [x] Selective cleaning works when supported
* [x] Unsupported devices degrade gracefully
* [x] Global fallback works
* [x] Partial failures work
* [x] Safety tests pass
* [x] No arbitrary privileged shell interface exists

---

# Phase 3 — Cleanup Coordinator & Results

**Goal:** Connect scanning and cleaning into a reliable workflow.

**Phase status:** Complete

* [x] **P3-01** Implement cleanup state machine
* [x] **P3-02** Validate capability before every cleanup
* [x] **P3-03** Capture pre-clean physical storage
* [x] **P3-04** Capture pre-clean reported cache
* [x] **P3-05** Execute cleanup plan
* [x] **P3-06** Implement bounded storage-stat settling
* [x] **P3-07** Rescan affected packages
* [x] **P3-08** Capture post-clean physical storage
* [x] **P3-09** Capture post-clean reported cache
* [x] **P3-10** Calculate physical free-space delta
* [x] **P3-11** Calculate reported cache delta
* [x] **P3-12** Clamp negative freed values
* [x] **P3-13** Add noise/significance threshold
* [x] **P3-14** Build cleanup confirmation screen
* [x] **P3-15** Build cleaning progress screen
* [x] **P3-16** Build cleanup result screen
* [x] **P3-17** Display partial failures

## Phase 3 Gate

* [x] Full scan → clean → rescan workflow works
* [x] Results never claim guaranteed reclaim amounts
* [x] Negative values are handled
* [x] Storage-stat delay is handled
* [x] Interrupted Shizuku session fails safely

---

# Phase 4 — Product UI & Persistence

**Goal:** Turn the technical application into the complete CacheSweep product.

**Phase status:** Complete

## Onboarding

* [x] **P4-01** Welcome screen
* [x] **P4-02** Usage Access onboarding
* [x] **P4-03** Shizuku onboarding
* [x] **P4-04** First scan flow

## Dashboard

* [x] **P4-05** Device storage visualization
* [x] **P4-06** Aggregate cache summary
* [x] **P4-07** Largest cache consumers
* [x] **P4-08** Shizuku status
* [x] **P4-09** Last scan information
* [x] **P4-10** Primary cleanup action

## Settings

* [x] **P4-11** Show system apps preference
* [x] **P4-12** Show zero-cache apps preference
* [x] **P4-13** Sort preference
* [x] **P4-14** Theme preference
* [x] **P4-15** Clear local history

## Persistence

* [x] **P4-16** Configure DataStore
* [x] **P4-17** Persist settings
* [x] **P4-18** Persist limited cleanup history
* [x] **P4-19** Limit cleanup history size

## Appearance/accessibility

* [x] **P4-20** Material 3 theme
* [x] **P4-21** Light mode
* [x] **P4-22** Dark mode
* [x] **P4-23** Font scaling review
* [x] **P4-24** TalkBack semantics
* [x] **P4-25** Touch-target review

## Phase 4 Gate

* [x] Complete product flow works
* [x] Product copy matches PRD
* [x] Settings persist
* [x] Light/dark themes work
* [x] Basic accessibility review passes

---

# Phase 5 — Hardening & Release

**Goal:** Produce a reliable personal release APK.

**Phase status:** In progress

## Failure scenarios

* [x] **P5-01** Shizuku absent
* [x] **P5-02** Shizuku stopped
* [x] **P5-03** Shizuku dies during cleanup
* [x] **P5-04** Permission denied
* [x] **P5-05** Permission revoked
* [x] **P5-06** Usage Access revoked
* [x] **P5-07** App process recreation
* [x] **P5-08** Device reboot
* [x] **P5-09** Individual package query failure
* [x] **P5-10** Individual package cleanup failure

## Performance

* [x] **P5-11** Test large installed-app count
* [x] **P5-12** Review icon memory usage
* [x] **P5-13** Review main-thread blocking
* [x] **P5-14** Measure scanner performance

## Security/privacy

* [x] **P5-15** Confirm no INTERNET permission
* [x] **P5-16** Confirm no analytics
* [x] **P5-17** Confirm no telemetry
* [x] **P5-18** Confirm no arbitrary shell interface
* [x] **P5-19** Review production logs
* [x] **P5-20** Review exported components

## Compatibility

* [x] **P5-21** Test target physical device
* [x] **P5-22** Record Android build/device results
* [x] **P5-23** Test additional OEM/device if available
* [x] **P5-24** Document unsupported behavior

## Release

* [ ] **P5-25** Configure release signing
* [ ] **P5-26** Build release APK
* [ ] **P5-27** Install clean release APK
* [ ] **P5-28** Run release smoke test
* [ ] **P5-29** Write installation instructions
* [ ] **P5-30** Tag `v1.0.0`

## Final Release Gate

CacheSweep 1.0 is ready when:

* [ ] Build passes
* [ ] Tests pass
* [ ] Physical-device validation passes
* [ ] Selective cleanup is either validated or capability-disabled
* [ ] Global fallback works
* [ ] No intentional app-data deletion occurs
* [ ] No internet permission exists
* [ ] Release APK installs and runs
* [ ] Documentation is current

---

# Milestone Tags

Recommended Git tags:

```text
phase-0-complete
phase-1-complete
phase-2-complete
phase-3-complete
phase-4-complete
phase-5-complete

v1.0.0
```

---

# Agent Rule

Agents should not automatically begin the next phase after completing a phase gate.

Finish the assigned task(s), update `STATUS.md`, build/test the project, and leave the exact next task documented.
