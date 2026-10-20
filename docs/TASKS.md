# Implementation Plan and Ordered Tasks — v1.0

**Team:** one Android developer with AI coding assistance | **Platform:** native Kotlin + Jetpack Compose | **Delivery:** local-only Android MVP. No app code in this document.

## 1. Build strategy
Ship **vertical slices** instead of building every repository and every UI component upfront. Finish personal entry and reporting before introducing shared accounting. Keep code in a single Android application module initially, with packages arranged by responsibility: `core/model`, `core/data`, `core/domain`, `core/ui`, `feature/onboarding`, `feature/personal`, `feature/activity`, `feature/insights`, `feature/groups`, `feature/settlements`, `feature/settings`. If the project stays small, feature packages may share a small number of ViewModels/repositories. Do not create microservices, network interfaces, authentication or generic design-system frameworks.

**Proposed stack (verify dependency versions/APIs at implementation):** Kotlin, Compose UI, Material 3 stable, AndroidX Lifecycle/ViewModel, Compose Navigation, Room (relational SQLite), Kotlin coroutines/Flow, DataStore Preferences for theme/onboarding, Android unit and Compose UI testing. Optional simple manual dependency wiring; DI framework not a release prerequisite. Use build-tools BOM/version catalog only to the extent it reduces actual maintenance. Check latest compatibility and minSdk policy before first coding task. Proposed **minSdk 26** for implementation simplicity, subject to device-market decision; compile/target SDK follow current Play requirements at implementation time, not hard-coded speculation.

## 2. Milestones, outputs, dependencies and completion gates
| Milestone | Outcome / demo | Depends on | Gate |
|---|---|---|---|
| M0 — Foundation | launchable Compose app, theme, route shell, empty onboarding/Today | none | fresh install, dark/light, emulator pass |
| M1 — Personal capture | Add/edit/delete personal transaction and persistence | M0 | AC-PER, atomic writes, no duplicates |
| M2 — History & Insights | scoped search/month/category filters and correct personal monthly summary | M1 | AC-ACT, AC-INS, offline/restart pass |
| M3 — Group core | group/member CRUD constraints and one-payer split editor | M2 | AC-GRP, AC-SHR, rounding unit tests |
| M4 — Balances & settlements | net engine, suggested payments, settlement CRUD | M3 | AC-BAL and invariants, audit details |
| M5 — Polish & release | accessibility, responsive, failure/restore, QA, app metadata | M4 | all P0 acceptance tests, release checklist |

**Effort guidance, not commitments:** M0 2–3 focused days; M1 3–5; M2 3–4; M3 5–8; M4 4–6; M5 4–7. Expect roughly **21–33 focused development days** for one developer familiar with Compose; less experience, QA findings or device-specific bugs can increase this. AI coding tools reduce boilerplate, **not** the need for money-invariant tests and manual UI validation.

## 3. Backlog: implement in listed order
Each task has a clear deliverable and test reference. `P0` = launch-blocking. `P1` = post-MVP. Do not start P1 until M5 passes.

### M0 — Foundation
- **T-001 (P0):** Create Android Studio Kotlin + Compose app with stable compatible dependency versions and minSdk decision. _Done:_ clean build/install on emulator; dependencies documented.
- **T-002 (P0):** Define domain-independent currency parser/formatter contract (INR to/from long paise) and date/clock abstraction. _Done:_ tests for comma separator, two fractional digits, bounds and invalid input (`AC-PER-02/07`, numeric fixtures).
- **T-003 (P0):** Define local Room schema (profiles, categories if needed, personal expenses, groups, members, shared expenses, expense shares, settlements), foreign keys/indexes, creation/update timestamps and migrations strategy. _Done:_ schema test and foreign-key tests. Keep fixed categories in code unless persistence is necessary.
- **T-004 (P0):** DataStore setup for onboarding flag, profile setting if appropriate, appearance selection; define no-backup behavior and disclose data risk. _Done:_ settings survive restart (`AC-GEN-02/06`).
- **T-005 (P0):** Build theme token mapping, typography, shapes and App Scaffold with four destinations and Settings route. _Done:_ light/dark basic pass (`AC-UX-03`).
- **T-006 (P0):** Implement Welcome ONB-01 and navigation back stack behavior. _Done:_ `AC-ONB-01/02/03`.

### M1 — Personal capture
- **T-007 (P0):** Personal expense DAO/repository atomic create/read/update/delete, Flow observation and validation. _Done:_ persistence, duplicate prevent and checked arithmetic tests.
- **T-008 (P0):** EXP-01 amount-first create/edit form with keyboard, date, category and optional fields; unsaved-changes confirmation. _Done:_ `AC-PER-01/02/03/04/06/07/08`, IME checks.
- **T-009 (P0):** Today personal hero total, recent transactions and Add FAB. _Done:_ correct current-month sum and no-group leakage.
- **T-010 (P0):** Personal detail with delete dialog and safe Undo (or remove Undo UI until durable restoration works). _Done:_ `AC-PER-05`; no incorrect Undo promise.

### M2 — Browse and understand
- **T-011 (P0):** Activity combined event presentation model with explicit type/scope; stable ordering and navigable details. _Done:_ `AC-ACT-01/05`.
- **T-012 (P0):** Activity search, month, scope, category/type filters and no-match/clear handling. _Done:_ `AC-ACT-02/03/04`.
- **T-013 (P0):** Insights local month totals and category breakdown (text fallback to any chart), navigation between periods. _Done:_ `AC-INS-01/03/04`.
- **T-014 (P0):** Cross-feature recomputation/Flow updates and tests for edits/backdating. _Done:_ all personal UI reflects committed writes without manual refresh.

### M3 — Shared group and splits
- **T-015 (P0):** Group/member persistence, unique member-name validation, owner presence, archival and no hard deletion of referenced members. _Done:_ `AC-GRP-01..07`; group deletion only with empty history.
- **T-016 (P0):** Groups list, create-group and group detail screens. _Done:_ member creation and month group total displayed separately from owner net.
- **T-017 (P0):** Pure split engine: selected beneficiaries, equal division sorted by ID, exact-amount inputs, overflow/error checks, sum invariant. _Done:_ fixtures 1–4 and `AC-SHR-02/04/05/06/09`.
- **T-018 (P0):** Shared expense transaction repository; in one Room transaction validate member references, persist parent and complete shares; edit replaces shares atomically. _Done:_ `AC-SHR-01/03/07/08`, integrity tests.
- **T-019 (P0):** SHR-01 shared form with payer/beneficiaries/segmented mode/remainder preview; SHR-02 detail and delete impact warning. _Done:_ `AC-SHR-01..10`, no double counting in Insights.
- **T-020 (P0):** Group member add, inactive-member management and archived read-only behavior. _Done:_ old entries preserved, future group entries do not default to inactive members. Member deactivation affordance may be a detail overflow item rather than a new screen.

### M4 — Net balances and settlement journal
- **T-021 (P0):** Implement pure group-net calculation from shared expenses and settlement records using checked integer arithmetic. _Done:_ `Σ(net)=0` invariant for every tested ledger, including edit/delete replay.
- **T-022 (P0):** Deterministic creditor/debtor suggestion allocator (magnitude descending then stable member IDs). _Done:_ `AC-BAL-01/07`, reproducible ordering.
- **T-023 (P0):** Settlement persistence and validation in atomic save operation; new transfers only against recomputed suggested pair and limit. _Done:_ `AC-BAL-02/04/05/06`; no payment API.
- **T-024 (P0):** Balances screen and settlement form/detail, explanation of offline recording, prefilling pair and amount. _Done:_ `AC-BAL-01..08` and zero-balance empty state.
- **T-025 (P0):** Historical-edit conflict advisory when a prior settlement no longer aligns with derived new suggestions. _Done:_ preserve recorded transfer, show review state, continue mathematically exact signed nets.

### M5 — Release hardening
- **T-026 (P0):** Manual accessibility pass: TalkBack, 200% font, 48dp hit targets, error focus, semantic owed/owing labels. _Done:_ `AC-UX-01/02`.
- **T-027 (P0):** Responsive/layout and IME hardening across compact/landscape/tablet test configurations; Back/rotation/process death. _Done:_ `AC-GEN-05`, `AC-UX-04`.
- **T-028 (P0):** Settings and full local data deletion flow, local-only warning, app display metadata. _Done:_ `AC-UX-05/06`.
- **T-029 (P0):** Failure injection for database write/read errors, double Save, missing record, stale group balance and empty states. _Done:_ zero partial commits, actionable errors.
- **T-030 (P0):** Full regression and release audit against `ACCEPTANCE_CRITERIA.md`, privacy and backup behavior, store description reflecting offline scope. _Done:_ release criteria met, known limitations stated.

### Deliberately deferred P1 tasks
- **T-101:** recurring expense templates and quick-entry shortcuts.
- **T-102:** configurable categories and budgets.
- **T-103:** portable export/import with migration and conflict handling.
- **T-104:** optimized settle-up recommendations and additional split types.
- **T-105:** secure cloud backup and Google Sheets integration after consent, identity and conflict rules are designed.

## 4. Recommended AI-assisted execution loop
For each task: (1) provide AI assistant only that task plus relevant sections of `PRODUCT.md`, `SCREEN_SPECS.md` and acceptance IDs, (2) ask for a small change and tests, (3) review entities/queries and financial invariants yourself, (4) run tests + install and manually inspect Compose UI, (5) commit when accepted. Never generate the entire app in one prompt; avoid invented Compose/Material APIs. Keep a `DECISIONS.md` for any product-rule amendments and update all five docs in the same change when accounting rules change.

**Suggested first build prompt:** “Implement T-001 only using a current stable Android Compose/Material 3 compatible setup. Show chosen dependency versions and why they are compatible; create no feature screens beyond a minimal compile/run smoke check. Do not add network/auth libraries. Include exact run steps.”

## 5. Dependency / risk register
| Risk | Mitigation | Owner decision |
|---|---|---|
| Share totals differ by 1 paisa | pure deterministic splitter, integer math, stable-ID remainder distribution | must pass before M4 |
| Misleading personal vs group totals | separate ledger entities and labels; tests explicitly exclude group from Insights | release-blocking |
| A historical settlement becomes incongruent after an edit | preserve actual transfer, recompute net and show review advisory, never mutate history invisibly | accepted MVP trade-off |
| Data loss on uninstall | explicit messaging, controlled OS backup behavior, no false backup promise | clear release copy |
| Too many form taps | amount-first defaults, sticky Save, optional fields, usability timing test | revisit after first usability check |
| Experimental Compose APIs break | stable APIs first; verify release and compatibility before implementing any Expressive variant | implementation gate |
| Group membership confusion | copy always clarifies these are local member names, not invited accounts | release-blocking |
| Accessibility regression | build into each vertical slice, final TalkBack and font-scale sweep | release-blocking |

## 6. Definition of done for any P0 ticket
Product behavior matches named screen spec; valid/invalid/empty/error states implemented; domain rules covered by automated tests; persisted changes survive restart; Kotlin lint/build and test suite pass; at least one emulator manual review; accessible labels and theme check; no new permissions/network/account assumptions. A milestone is complete only when its dependent criteria pass, not when screenshots merely look finished.

Related docs: [Product](PRODUCT.md) · [Screens](SCREEN_SPECS.md) · [Design](DESIGN_SYSTEM.md) · [Acceptance](ACCEPTANCE_CRITERIA.md)
