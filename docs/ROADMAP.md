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

**Phase status:** Not started

## Project foundation

* [x] **P0-01** Create Android Studio project
* [x] **P0-02** Configure Kotlin + Jetpack Compose
* [x] **P0-03** Configure Android SDK versions and Gradle
* [x] **P0-04** Add required dependencies
* [x] **P0-05** Create initial package/project structure
* [x] **P0-06** Create diagnostic Compose screen
* [x] **P0-07** Verify debug APK builds successfully

## Usage Access

* [ ] **P0-08** Add `PACKAGE_USAGE_STATS` declaration
* [ ] **P0-09** Implement Usage Access detection
* [ ] **P0-10** Implement Usage Access settings intent
* [ ] **P0-11** Display Usage Access state on diagnostic screen

## Storage statistics

* [ ] **P0-12** Implement device storage snapshot using `StatFs`
* [ ] **P0-13** Implement installed package enumeration
* [ ] **P0-14** Implement basic `StorageStatsManager` package query
* [ ] **P0-15** Display cache/app/data size for a test package
* [ ] **P0-16** Verify StorageStats behavior on physical device

## Shizuku

* [ ] **P0-17** Add Shizuku API/provider dependencies
* [ ] **P0-18** Configure Shizuku provider
* [ ] **P0-19** Implement Shizuku Binder availability detection
* [ ] **P0-20** Implement Shizuku permission request
* [ ] **P0-21** Handle Binder received/dead events
* [ ] **P0-22** Display Shizuku connection state
* [ ] **P0-23** Display privileged UID
* [ ] **P0-24** Verify shell UID on physical device

## Privileged backend

* [ ] **P0-25** Define minimal AIDL interface
* [ ] **P0-26** Implement Shizuku UserService
* [ ] **P0-27** Bind application to UserService
* [ ] **P0-28** Verify typed privileged call from app process
* [ ] **P0-29** Ensure no arbitrary shell API is exposed

## Capability detection

* [ ] **P0-30** Implement package-manager capability probe
* [ ] **P0-31** Detect `clear --cache-only`
* [ ] **P0-32** Detect `trim-caches`
* [ ] **P0-33** Display capability results on diagnostic screen
* [ ] **P0-34** Cache capabilities for current Shizuku session

## Cache clearing safety test

* [ ] **P0-35** Create disposable test application or test fixture
* [ ] **P0-36** Generate significant cache in test application
* [ ] **P0-37** Store test SharedPreferences value
* [ ] **P0-38** Store test database/data value
* [ ] **P0-39** Implement safe package cache command builder
* [ ] **P0-40** Add test ensuring `--cache-only` is mandatory
* [ ] **P0-41** Execute selective cache clear against disposable test app
* [ ] **P0-42** Verify cache decreases
* [ ] **P0-43** Verify SharedPreferences survive
* [ ] **P0-44** Verify database/application data survive

## Global cache trimming

* [ ] **P0-45** Implement typed global trim operation
* [ ] **P0-46** Test `trim-caches` on physical device
* [ ] **P0-47** Record before/after physical storage
* [ ] **P0-48** Record before/after reported cache
* [ ] **P0-49** Validate global trim fallback behavior

## Phase 0 Gate

Phase 0 is complete only when:

* [ ] Project builds
* [ ] Usage Access works
* [ ] StorageStats returns useful values
* [ ] Shizuku connects
* [ ] Privileged UID is confirmed
* [ ] Capability probe works
* [ ] Selective cache clear is proven safe **or explicitly marked unsupported**
* [ ] Global trim is proven usable
* [ ] No complete app data is deleted
* [ ] Findings are recorded in `STATUS.md`
* [ ] Architecture deviations are recorded in `DECISIONS.md`

---

# Phase 1 — Production Cache Scanner

**Goal:** Build the production-quality application cache scanner.

**Depends on:** Phase 0

**Phase status:** Blocked by Phase 0

## Package discovery

* [ ] **P1-01** Implement production package enumeration
* [ ] **P1-02** Load application display names
* [ ] **P1-03** Load application icons
* [ ] **P1-04** Classify user/system apps
* [ ] **P1-05** Exclude CacheSweep where appropriate

## StorageStats repository

* [ ] **P1-06** Implement production `StorageStatsRepository`
* [ ] **P1-07** Handle default/internal storage
* [ ] **P1-08** Handle packages whose stats cannot be queried
* [ ] **P1-09** Implement per-package cache/app/data model

## Scanner

* [ ] **P1-10** Implement `CacheScanner` interface
* [ ] **P1-11** Add bounded concurrency
* [ ] **P1-12** Add progressive scan state
* [ ] **P1-13** Add partial-failure handling
* [ ] **P1-14** Calculate aggregate reported cache
* [ ] **P1-15** Track successful vs attempted measurements
* [ ] **P1-16** Measure scan duration

## User experience

* [ ] **P1-17** Build cache application list
* [ ] **P1-18** Sort by cache size
* [ ] **P1-19** Sort by total size
* [ ] **P1-20** Sort alphabetically
* [ ] **P1-21** Implement application search
* [ ] **P1-22** Implement pull-to-refresh
* [ ] **P1-23** Build application detail screen
* [ ] **P1-24** Add native Android storage-settings shortcut

## Tests

* [ ] **P1-25** Scanner unit tests
* [ ] **P1-26** Partial failure tests
* [ ] **P1-27** Sorting tests
* [ ] **P1-28** Search tests
* [ ] **P1-29** Test 300+ application scan if possible

## Phase 1 Gate

* [ ] Scanner works independently of Shizuku
* [ ] One bad package does not fail the scan
* [ ] UI remains responsive
* [ ] Cache list can be searched/sorted
* [ ] Build and tests pass

---

# Phase 2 — Production Cleaner

**Goal:** Build the safe privileged cache-cleaning backend.

**Depends on:** Phase 0 and preferably Phase 1

**Phase status:** Blocked

## Cleaner abstraction

* [ ] **P2-01** Implement `CacheCleaner`
* [ ] **P2-02** Implement `CleanerCapabilities`
* [ ] **P2-03** Implement typed cleanup plans
* [ ] **P2-04** Implement typed cleaner errors

## Selective cleaning

* [ ] **P2-05** Productionize package cache clear
* [ ] **P2-06** Validate package against scanned package set
* [ ] **P2-07** Prevent CacheSweep self-clean during operation
* [ ] **P2-08** Implement multi-package cleaning
* [ ] **P2-09** Implement package-count progress
* [ ] **P2-10** Continue after individual package failure
* [ ] **P2-11** Report failed packages

## Global fallback

* [ ] **P2-12** Productionize global trim
* [ ] **P2-13** Calculate desired free-storage target
* [ ] **P2-14** Require explicit user consent before selective → global fallback
* [ ] **P2-15** Handle unsupported global trim

## Security

* [ ] **P2-16** Ensure no `sh -c`
* [ ] **P2-17** Ensure no arbitrary command execution
* [ ] **P2-18** Test command argument generation
* [ ] **P2-19** Test plain `pm clear PACKAGE` cannot be generated
* [ ] **P2-20** Audit exported Android components

## Phase 2 Gate

* [ ] Selective cleaning works when supported
* [ ] Unsupported devices degrade gracefully
* [ ] Global fallback works
* [ ] Partial failures work
* [ ] Safety tests pass
* [ ] No arbitrary privileged shell interface exists

---

# Phase 3 — Cleanup Coordinator & Results

**Goal:** Connect scanning and cleaning into a reliable workflow.

**Phase status:** Blocked

* [ ] **P3-01** Implement cleanup state machine
* [ ] **P3-02** Validate capability before every cleanup
* [ ] **P3-03** Capture pre-clean physical storage
* [ ] **P3-04** Capture pre-clean reported cache
* [ ] **P3-05** Execute cleanup plan
* [ ] **P3-06** Implement bounded storage-stat settling
* [ ] **P3-07** Rescan affected packages
* [ ] **P3-08** Capture post-clean physical storage
* [ ] **P3-09** Capture post-clean reported cache
* [ ] **P3-10** Calculate physical free-space delta
* [ ] **P3-11** Calculate reported cache delta
* [ ] **P3-12** Clamp negative freed values
* [ ] **P3-13** Add noise/significance threshold
* [ ] **P3-14** Build cleanup confirmation screen
* [ ] **P3-15** Build cleaning progress screen
* [ ] **P3-16** Build cleanup result screen
* [ ] **P3-17** Display partial failures

## Phase 3 Gate

* [ ] Full scan → clean → rescan workflow works
* [ ] Results never claim guaranteed reclaim amounts
* [ ] Negative values are handled
* [ ] Storage-stat delay is handled
* [ ] Interrupted Shizuku session fails safely

---

# Phase 4 — Product UI & Persistence

**Goal:** Turn the technical application into the complete CacheSweep product.

**Phase status:** Blocked

## Onboarding

* [ ] **P4-01** Welcome screen
* [ ] **P4-02** Usage Access onboarding
* [ ] **P4-03** Shizuku onboarding
* [ ] **P4-04** First scan flow

## Dashboard

* [ ] **P4-05** Device storage visualization
* [ ] **P4-06** Aggregate cache summary
* [ ] **P4-07** Largest cache consumers
* [ ] **P4-08** Shizuku status
* [ ] **P4-09** Last scan information
* [ ] **P4-10** Primary cleanup action

## Settings

* [ ] **P4-11** Show system apps preference
* [ ] **P4-12** Show zero-cache apps preference
* [ ] **P4-13** Sort preference
* [ ] **P4-14** Theme preference
* [ ] **P4-15** Clear local history

## Persistence

* [ ] **P4-16** Configure DataStore
* [ ] **P4-17** Persist settings
* [ ] **P4-18** Persist limited cleanup history
* [ ] **P4-19** Limit cleanup history size

## Appearance/accessibility

* [ ] **P4-20** Material 3 theme
* [ ] **P4-21** Light mode
* [ ] **P4-22** Dark mode
* [ ] **P4-23** Font scaling review
* [ ] **P4-24** TalkBack semantics
* [ ] **P4-25** Touch-target review

## Phase 4 Gate

* [ ] Complete product flow works
* [ ] Product copy matches PRD
* [ ] Settings persist
* [ ] Light/dark themes work
* [ ] Basic accessibility review passes

---

# Phase 5 — Hardening & Release

**Goal:** Produce a reliable personal release APK.

**Phase status:** Blocked

## Failure scenarios

* [ ] **P5-01** Shizuku absent
* [ ] **P5-02** Shizuku stopped
* [ ] **P5-03** Shizuku dies during cleanup
* [ ] **P5-04** Permission denied
* [ ] **P5-05** Permission revoked
* [ ] **P5-06** Usage Access revoked
* [ ] **P5-07** App process recreation
* [ ] **P5-08** Device reboot
* [ ] **P5-09** Individual package query failure
* [ ] **P5-10** Individual package cleanup failure

## Performance

* [ ] **P5-11** Test large installed-app count
* [ ] **P5-12** Review icon memory usage
* [ ] **P5-13** Review main-thread blocking
* [ ] **P5-14** Measure scanner performance

## Security/privacy

* [ ] **P5-15** Confirm no INTERNET permission
* [ ] **P5-16** Confirm no analytics
* [ ] **P5-17** Confirm no telemetry
* [ ] **P5-18** Confirm no arbitrary shell interface
* [ ] **P5-19** Review production logs
* [ ] **P5-20** Review exported components

## Compatibility

* [ ] **P5-21** Test target physical device
* [ ] **P5-22** Record Android build/device results
* [ ] **P5-23** Test additional OEM/device if available
* [ ] **P5-24** Document unsupported behavior

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
