# ARCHITECTURE.md — Ledger (Expense Tracker) v1.0

**Status:** Proposed. **Date checked:** 2026-10-20. **Scope:** Local-only Android MVP described in PRODUCT, SCREEN_SPECS, DESIGN_SYSTEM, ACCEPTANCE_CRITERIA and TASKS v1.0.

## 0. How to read this document

Every technical claim has one of these labels:

| Label | Meaning |
|---|---|
| **[Verified]** | Checked today against an official or primary source (cited). |
| **[Known – recheck]** | From engineering experience, not re-checked today. Confirm when the task starts. |
| **[Assumption]** | A choice made to keep moving. Can be changed. |
| **[Decision]** | Recommended choice. Record it in `DECISIONS.md` once accepted. |

## 1. Key decisions at a glance

| Topic | Decision |
|---|---|
| Modules | Two Gradle modules: `:app` (Android) and `:domain` (pure Kotlin). **[Decision]** |
| Pattern | MVVM with one-way data flow. Each screen has one `StateFlow<UiState>`. |
| Storage | Room for all ledger data. DataStore Preferences for profile name, onboarding flag and theme. |
| Money | `Long` paise inside a value class. Own INR parser and formatter. No floats anywhere. |
| Dates | Reporting date stored as `epochDay` (a whole number). Metadata times stored as epoch milliseconds. |
| Balances | Never stored. Always recomputed from expenses and settlements. **[Decision]** |
| DI | Hilt, kept small: about 3 modules. |
| Navigation | Navigation Compose 2.10.x with type-safe routes, as the brief asks. Navigation 3 is noted as an alternative. |
| Network | None. No Retrofit, no OkHttp, no `INTERNET` permission in v1. |
| Material 3 | Stable 1.4.0 through the BOM. Do not use Expressive APIs that are still experimental. Re-check 1.5.0 at the start of M3. |

## 2. Verified facts (as of 2026-10-20)

| Item | Fact |
|---|---|
| Material 3 versions | The release page lists stable 1.4.0 and beta 1.5.0-beta01, with the latest update on October 07, 2026. |
| Material 3 1.4.0 date | The stable channel log puts Compose Material 3 Version 1.4.0 under September 24, 2025. |
| Compose BOM | Official docs show the BOM at androidx.compose:compose-bom:2026.09.00. The Compose core libraries are at 1.12.1, and Room 2.8.5 and Room3 3.0.3 were released on September 09, 2026. |
| Navigation | Navigation 2.10.2 and Navigation3 1.2.0 were released as stable on September 23, 2026. The Compose docs now link "Navigation" to the Navigation 3 guide. |
| Other AndroidX | Lifecycle 2.11.0 (June 17, 2026), androidx Hilt 1.4.0 and Room3 3.0.0 (July 01, 2026), DataStore 1.2.1 and Activity 1.13.0 (March 2026). |
| Kotlin | Kotlin 2.4.21 was released on October 8, 2026 as a bug fix release. |
| KSP | The newest tag on the KSP GitHub releases page is 2.3.12. |
| AGP | AGP 9.4 supports up to API level 37 and needs Gradle 9.6.0 and JDK 17. AGP 9.0 added built-in Kotlin support and turns it on by default. |
| Hilt and AGP | Dagger 2.59 added AGP 9 support for the Hilt Gradle plugin, so AGP 9 and Gradle 9.1+ are now required for it. |
| Play target SDK | From 31 August 2026, new apps and app updates must target Android 16 (API level 36) or higher. |
| Back handling | One project's report says that on Android 16+, apps targeting API 36 no longer receive Activity.onBackPressed() or KEYCODE_BACK and must use the predictive-back APIs. |
| Compose 1.12 toolchain (secondary source) | One project reports that compose-bom 2026.08.00 pins the Compose 1.12.0 train, whose -android artifacts want AGP 9.1/compileSdk 37. Confirm this in T-001. |

## 3. Corrections needed in the current docs (fix before coding)

1. **The amount limit contradicts itself (blocking).** In PRODUCT §6, ₹10,00,00,000.00 is 10 crore rupees, which is **10,000,000,000 paise (1e10)**. The doc says "= 1,000,000,000 paise (1e9)", which is only ₹1,00,00,000.00 (1 crore). These differ by 10×. ACCEPTANCE fixture 6 uses the ₹ figure. Pick one value and define it once as `MAX_ENTRY_PAISE` in `:domain`. Either value fits easily in a `Long`.
2. **The Material 3 date is wrong.** DESIGN_SYSTEM §9 says stable 1.4.0 is from 2026-10-07. The Oct 7, 2026 release was 1.5.0-beta01. Stable 1.4.0 dates from September 2025 (see §2).
3. **The default title rule is unclear.** PRODUCT §5.2 can be read two ways. **[Decision]** Store `title` as nullable. When it is null, display the category label. This way a later category change never leaves a stale stored title.
4. **The decimals rule is unclear.** PRODUCT says to show "two decimals where paise nonzero". AC-PER-01 shows "₹125.50", but whole amounts and hero totals are not defined. Product must choose between always showing two decimals (better column alignment) and hiding `.00`.
5. **TASKS §1 says "single module".** This architecture adds one pure-Kotlin `:domain` module (see §6). Log this deviation in `DECISIONS.md`.

## 4. Missing requirements (with proposed defaults)

| # | Gap | Proposed default |
|---|---|---|
| G1 | Remainder order uses "ascending stable memberId", but the comparison is not defined. **[Known – recheck]** Java's `UUID.compareTo` compares signed 64-bit halves, so its order can differ from string order. | Sort by the lowercase canonical UUID **string**. Put this in a single `StableIdOrder` helper and test it. |
| G2 | The date picker returns UTC milliseconds. **[Known – recheck]** Material 3's `DatePicker` reports UTC midnight, so converting with the device zone can shift the date by a day. | Convert with `ZoneOffset.UTC` into a `LocalDate`, then store the `epochDay`. Test this under several time zones. |
| G3 | Undo semantics after process death are not defined. | Undo only for **personal** expense delete in v1. Hard-delete inside a transaction and keep the full row in ViewModel memory. Undo re-inserts the same ID and `createdAt`. If the process dies, Undo is gone (allowed, since the spec says "short-lived"). Shared expense and settlement deletes get confirmation only. |
| G4 | "Review balances" has no exact trigger. | Let E = each member's net from expenses only, and N = net including settlements. Flag the group if any member has N ≠ 0 with the opposite sign to E, or E = 0 and N ≠ 0. In plain words: settlements pushed someone past zero. New settlements can never cause this, because they are capped at the suggestion. Product sign-off given (M4.1). |
| G5 | Only suggested pairs can settle. In groups of 3+, a real payment (for example C paid B) may be impossible to record. | Keep the rule for v1. Write it down as a known limitation and add a P1 item. |
| G6 | Member-name uniqueness with inactive members is ambiguous. | Names are unique across **all** members of a group, active or not. Enforce with a unique index on `(groupId, normalizedName)`. |
| G7 | The owner member's name is not defined. | The owner member shows the profile display name. It is stored as `isLocalOwner = true`, and its name is read from the profile, not copied. |
| G8 | Archiving a group with non-zero balances is not covered. | Allowed, but the confirmation dialog warns that balances are frozen. |
| G9 | Backup behaviour is not defined in detail. **[Known – recheck]** On Android 12+, `allowBackup=false` may not stop device-to-device transfer. | Add `dataExtractionRules` that exclude the database and DataStore from both cloud backup and device transfer. Product must confirm, since device transfer could actually help users. |
| G10 | Search case rules are not defined. **[Known – recheck]** SQLite `LIKE` ignores case only for ASCII. | Store a lowercased `searchText` column, or filter in Kotlin. Data volume is small. |
| G11 | Spoken money format is not defined. | Read amounts as "125 rupees 50 paise". Put this in one tested function, `moneyContentDescription()`. |
| G12 | Dynamic color can clash with owe/owed colors. | Status colors stay fixed, even when dynamic color is on. The words carry the meaning. |
| G13 | The scope of "Delete all data" is not defined. | Clear Room tables and DataStore inside one coroutine, then restart the nav graph at onboarding. |
| G14 | Equal split with 0-paise shares: whether to persist the zero rows is not defined. | Persist the zero rows for equal splits, so the detail screen shows "Included: ₹0.00". |
| G15 | Input edge cases are not listed: ".5", "5.", leading zeros, commas after the decimal point, non-ASCII digits. | Accept ".5", "5." and leading zeros. Reject commas after the decimal point, repeated, leading or trailing commas, and any non-ASCII digit. Full table in §15. |

## 5. Architectural risks and mitigations

| Risk | Why it matters | Mitigation |
|---|---|---|
| Toolchain coupling | Compose 1.12 may need compileSdk 37 and AGP 9.1+. The Hilt plugin needs AGP 9. KSP must match the Kotlin version. | T-001 builds a "spike" with Room + Hilt + KSP before anything else. Fallback: an older BOM and Kotlin 2.3.x. |
| Expressive API churn | The 1.5.0 alphas renamed or removed many APIs (sliders, toggle buttons, app bar scroll behaviours, bottom sheet state). | Wrap every Material component we use in `core/ui/components`. Use no experimental Expressive APIs in v1. |
| Back interception on API 36 | The dirty-form "Discard / Keep editing" prompt (AC-PER-06) must work with predictive back. | Use Compose `BackHandler(enabled = isDirty)` only. Never override `onBackPressed`. Test this on an Android 16+ emulator. |
| Money bugs | Release-blocking. | All money math lives in `:domain` as pure functions using `Math.addExact` and `subtractExact`. Use property-style tests (random ledgers, Σnet = 0). |
| Stale balance at save | Two quick saves could read old balances. | Settlement save reads the ledger, recomputes and validates **inside** `withTransaction`. |
| SQL aggregates | **[Known – recheck]** SQLite `TOTAL()` returns a float. | Use only `SUM()` on integer columns, or sum in Kotlin. Never use `TOTAL` or `AVG`. |
| Locale formatting drift | JVM unit tests and Android ICU can format en-IN differently. | Write our own INR formatter (Indian 2-3 grouping), so output is the same everywhere. |
| Edge-to-edge and large screens | **[Known – recheck]** Targeting 35/36 enforces edge-to-edge, and large screens may ignore orientation locks. | Use insets from day one. Never lock orientation. |
| Over-engineering | One developer, about 30 days of work. | No use-case classes by default, no base ViewModel, no MVI library, no Paging until it is measured as needed. |

## 6. Project structure

```
ledger/
├─ settings.gradle.kts
├─ gradle/libs.versions.toml          # single source of versions
├─ domain/                            # kotlin("jvm"); NO Android, NO Room
│  └─ src/main/kotlin/app/ledger/domain/
│     ├─ money/   Paise.kt, MoneyParser.kt, InrFormatter.kt, MoneyLimits.kt
│     ├─ time/    MonthRange.kt, DateRules.kt (2000-01-01..2100-12-31)
│     ├─ common/  StableIdOrder.kt, Validation.kt (sealed errors)
│     ├─ personal/ Category.kt, PersonalExpense.kt, PersonalRules.kt, MonthlySummary.kt
│     └─ group/   Group.kt, Member.kt, SharedExpense.kt, Settlement.kt,
│                 SplitCalculator.kt, BalanceCalculator.kt,
│                 SettlementSuggester.kt, ReviewDetector.kt
└─ app/
   ├─ schemas/                        # Room exported schema JSON (committed)
   └─ src/
      ├─ main/java/app/ledger/
      │  ├─ LedgerApplication.kt (@HiltAndroidApp), MainActivity.kt
      │  ├─ navigation/  Routes.kt, LedgerNavHost.kt, AppShell.kt
      │  ├─ core/
      │  │  ├─ data/db/      LedgerDatabase.kt, entity/, dao/, Converters.kt
      │  │  ├─ data/prefs/   SettingsStore.kt (DataStore)
      │  │  ├─ data/repo/    PersonalExpenseRepository.kt, GroupRepository.kt,
      │  │  │                SharedExpenseRepository.kt, SettlementRepository.kt,
      │  │  │                ActivityRepository.kt, SettingsRepository.kt
      │  │  ├─ di/           DataModule.kt, TimeModule.kt
      │  │  └─ ui/           theme/, components/, format/ (a11y money text)
      │  └─ feature/
      │     ├─ onboarding/ today/ personal/ activity/ insights/
      │     ├─ groups/ shared/ settlements/ settings/
      │     └─ (each: XxxScreen.kt [stateless], XxxRoute.kt [VM wiring], XxxViewModel.kt, XxxUiState.kt)
      ├─ test/          # JVM: ViewModel tests with fakes
      └─ androidTest/   # Room DAO/transaction tests, Compose UI tests
```

**Why `:domain` is a separate module:** the compiler then guarantees that money and split logic cannot import Android or Room. Its tests run in seconds on the JVM. This is the cheapest strong boundary available.

## 7. Layer boundaries

| Layer | Contains | May depend on | Must not |
|---|---|---|---|
| **UI** (`feature/*`, `core/ui`) | Stateless screens, route wrappers, ViewModels, UI strings and a11y text | `:domain`, repositories | Touch DAOs, entities or DataStore directly. Do money math. |
| **Domain** (`:domain`) | Models, validation, parser and formatter, split, balance, suggestion and review engines, month math | Kotlin stdlib, `java.time` | Use Android, Room, Hilt or coroutines. |
| **Data** (`core/data`) | Room DB, DAOs, entities, DataStore, repositories, entity↔domain mapping | `:domain`, Room, DataStore | Expose entities upward. Skip validation on write. |

**Rules**

1. ViewModels validate for user feedback. **Repositories validate again on every write** using the same domain functions. This is the real guard.
2. Every multi-row write (shared expense + shares, settlement + recheck, delete-all) runs in one `withTransaction`.
3. A repository gets an interface **only** if a ViewModel test fakes it. Otherwise it is a concrete class.
4. For time, inject `java.time.Clock`. It is already an abstraction, and tests use `Clock.fixed`. For IDs, inject a `fun interface IdGenerator`.
5. Things we will not build: a use-case layer, a generic `Result` wrapper on everything, a base ViewModel, network interfaces, a cache-balance table.

### Core domain contracts (signatures only)

```kotlin
@JvmInline value class Paise(val value: Long)          // always >= 0 for entries

sealed interface AmountError { Empty; Invalid; TooManyDecimals; NotPositive; OverLimit }
fun parseInrAmount(input: String): Either<AmountError, Paise>   // or a small sealed result
fun formatInr(paise: Paise): String                       // magnitude only; words carry direction

fun splitEqually(total: Paise, memberIds: List<String>): Map<String, Paise>
fun computeNets(expenses: List<SharedExpenseWithShares>, settlements: List<Settlement>): Map<String, Long>
fun suggestSettlements(nets: Map<String, Long>): List<SuggestedTransfer>
```

## 8. Data model (Room)

| Table | Key columns | Constraints |
|---|---|---|
| `personal_expense` | `id TEXT PK`, `amount_paise INTEGER`, `title TEXT?`, `category_key TEXT`, `occurred_epoch_day INTEGER`, `note TEXT?`, `created_at`, `updated_at` | Index on `occurred_epoch_day` |
| `expense_group` | `id`, `name`, `created_at`, `archived_at?` | — |
| `member` | `id`, `group_id`, `name`, `normalized_name`, `is_local_owner`, `active` | FK group; **unique (`group_id`, `normalized_name`)** |
| `shared_expense` | `id`, `group_id`, `amount_paise`, `title?`, `category_key`, `occurred_epoch_day`, `payer_member_id`, `note?`, timestamps | FK group; FK payer **RESTRICT** |
| `expense_share` | PK (`shared_expense_id`, `member_id`), `share_paise` | FK expense **CASCADE**; FK member **RESTRICT** |
| `settlement` | `id`, `group_id`, `from_member_id`, `to_member_id`, `amount_paise`, `occurred_epoch_day`, `note?`, timestamps | FKs **RESTRICT** |

Notes. Categories are stored as stable string keys, never as enum ordinals. **[Known – recheck]** Room turns on foreign keys. The T-003 tests prove this. Before the first public release, edit schema version 1 freely. After release, freeze it, export the schema, and require migration tests.

**Idempotent save [Decision]:** the form creates the record ID when it opens and keeps it in `SavedStateHandle`. Create uses `INSERT … ABORT`. A second tap hits a primary-key conflict, which we treat as "already saved". This works together with the `isSaving` guard.

## 9. Recommended libraries

| Area | Library | Version | Status |
|---|---|---|---|
| Language | Kotlin + Compose compiler plugin | 2.4.x if KSP and Dagger support it, else 2.3.21 | Verified release; compatibility to be confirmed in T-001 |
| Build | AGP / Gradle | 9.4.0 / 9.6.0 | Verified |
| SDK levels | compileSdk / targetSdk / minSdk | 37 (if Compose 1.12 needs it) / 36 / 26 | target 36 is verified as the Play rule; minSdk 26 is an **[Assumption]** (native `java.time`) |
| Compose | Compose BOM | 2026.09.00 (UI 1.12.1) | Verified |
| Material | material3 (via BOM) | 1.4.0 stable | Verified; 1.5.0-beta01 is on watch |
| Adaptive | material3 adaptive-navigation-suite; material3-adaptive | from M3 BOM / 1.3.0 | Adaptive 1.3.0 verified; use in M5 |
| Lifecycle | lifecycle-viewmodel-compose, runtime-compose | 2.11.0 | Verified |
| Navigation | navigation-compose + kotlinx-serialization | 2.10.2 | Verified; serialization plugin = Kotlin version |
| DI | Hilt (Dagger) + androidx hilt-lifecycle-viewmodel-compose | Dagger 2.59.x+ / androidx hilt 1.4.0 | Partly verified; a newer Dagger may exist |
| DB | Room (runtime, ktx, compiler via KSP, testing) | 2.8.5 | Verified. Room3 3.0.x exists. **[Assumption]** Stay on Room 2.8 until Room 3's differences are reviewed |
| Settings | DataStore Preferences | 1.2.1 | Verified |
| Startup | core-splashscreen (hold UI until onboarding flag loads) | 1.2.0 | Verified (Nov 2025) |
| Testing | JUnit4, androidx.test runner 1.7.0, ext-junit 1.3.0, compose ui-test, Turbine, coroutines-test | — | androidx.test verified in stable log; Turbine and coroutines not checked |
| Network | Retrofit / OkHttp | **Not included** | Product rule: no network in v1 |

## 10. Material 3 Expressive: APIs that need version checks

**Policy [Decision]:** v1 uses only APIs that are non-experimental in the **stable** release we ship. Expressive styling (hero shapes, type contrast) is done with our own tokens on stable components. Check again at the start of M3: if 1.5.0 is stable by then, adopt its newly promoted components.

What the 1.5.0 alpha and beta notes show (all from the official release page):

| Component area | In 1.5.0 pre-releases | Our use / action |
|---|---|---|
| Top app bar scroll behaviour | TopAppBarScrollBehavior and its related methods became stable, no longer needing the @ExperimentalMaterial3Api opt-in | Treat as experimental on 1.4.0. Use only plain `TopAppBar`. |
| Expressive theme | materialExpressiveTheme and expressiveLightColorScheme were promoted | Do not use on 1.4.0. Use our own `MaterialTheme` setup. |
| FAB, buttons, toggle buttons | FAB and FAB Menu graduated from Experimental Expressive, and expressive button APIs were promoted | Use the standard `ExtendedFloatingActionButton` and `Button` only. |
| Shapes and loading | MaterialShapes and LoadingIndicator were reverted from stable back to experimental | Do not use. |
| Bottom sheets | rememberBottomSheetState became the unified state API, and rememberModalBottomSheetState was deprecated | Category and member pickers use a bottom sheet. Isolate it in one wrapper. |
| Text fields | TextFieldLabelPosition.Attached was deprecated in favour of new Inside and Cutout subtypes | The amount field is wrapped in `AmountField` so this change is local. |
| Date picker | DatePicker APIs that use Java Time classes are no longer tagged experimental (in 1.5.0) | On 1.4.0, use the millisecond API with UTC conversion (G2). Check its opt-in status. |
| App bar with search | AppBarWithSearch APIs were given the @ExperimentalMaterial3Api annotation again | Activity uses a plain `OutlinedTextField` for search. |

**Also check on 1.4.0 [Known – recheck]:** `SegmentedButton` (Equally / Exact), `ModalBottomSheet`, `DatePickerDialog`, `NavigationSuiteScaffold` vs `ShortNavigationBar` / `WideNavigationRail`, and `FilterChip`. Rule: any `@OptIn(Experimental…)` in a pull request needs a note in `DECISIONS.md`.

## 11. Screen state pattern

```kotlin
// Read screens
sealed interface TodayUiState {
  data object Loading : TodayUiState
  data class Content(val month: YearMonth, val total: Paise, val recent: List<RowUi>) : TodayUiState
  data object Error : TodayUiState
}
// ViewModel: repo.observe(...).map{...}.stateIn(viewModelScope, WhileSubscribed(5_000), Loading)

// Form screens
data class ExpenseFormUiState(
  val recordId: String, val isEdit: Boolean,
  val amountText: String, val amountError: AmountError?,
  val title: String, val category: Category, val date: LocalDate, val note: String,
  val isDirty: Boolean, val isSaving: Boolean, val saveFailed: Boolean,
  val saved: Boolean,              // UI navigates, then calls onSavedHandled()
)
```

Rules:

1. One `StateFlow` per screen. The UI collects it with `collectAsStateWithLifecycle`.
2. User actions are plain ViewModel functions, such as `onAmountChange` and `onSave`.
3. One-time results (saved, deleted) are **fields in state** that the UI clears after acting, not hidden event channels. This matches Android's UI-state guidance. **[Known – recheck]**
4. Draft fields (raw text) live in `SavedStateHandle`, so they survive process death. Committed data lives only in Room.
5. Each screen has a stateless `XxxScreen(state, callbacks)` used by tests and previews, plus a thin `XxxRoute` that wires the ViewModel.
6. `isSaving` disables Save. The client-generated ID protects against double taps.
7. Keyboard `Done` only clears focus. It never saves (AC-UX-04).

## 12. Navigation pattern

```kotlin
@Serializable data object OnboardingRoute
@Serializable data object TodayRoute; @Serializable data object ActivityRoute
@Serializable data object GroupsRoute; @Serializable data object InsightsRoute
@Serializable data object SettingsRoute
@Serializable data class ExpenseFormRoute(val expenseId: String? = null)
@Serializable data class ExpenseDetailRoute(val expenseId: String)
@Serializable data class GroupDetailRoute(val groupId: String)
@Serializable data class SharedFormRoute(val groupId: String, val expenseId: String? = null)
@Serializable data class BalancesRoute(val groupId: String)
@Serializable data class SettlementFormRoute(val groupId: String, val settlementId: String? = null,
                                             val fromId: String? = null, val toId: String? = null)
```

1. **Start destination:** the splash screen stays up until DataStore emits the onboarding flag. Then the start is `OnboardingRoute` or `TodayRoute`. Onboarding pops itself with `inclusive = true`.
2. **Tabs:** four top-level routes inside the app shell, using `saveState` / `restoreState`. This keeps Activity filters and scroll position (AC-ACT-04).
3. **Arguments:** only IDs. Screens load their own data. A missing record shows an "unavailable" state (EXP-02, SHR-02).
4. **After delete:** pop the detail screen. Edit returns to detail, unless the detail was deleted.
5. **Wide screens (M5):** list/detail with `material3-adaptive` for Activity and Groups. Phone behaviour is the release gate.
6. **Back on dirty forms:** use `BackHandler`, so it works with predictive back.

## 13. Dependency pattern (Hilt)

| Module | Provides | Scope |
|---|---|---|
| `DataModule` | `LedgerDatabase`, DAOs, `DataStore<Preferences>`, repositories bound to interfaces where faked | `@Singleton` |
| `TimeModule` | `Clock.systemDefaultZone()`, `IdGenerator { UUID.randomUUID().toString() }` | `@Singleton` |

ViewModels use `@HiltViewModel` with `SavedStateHandle`. Navigation arguments come from `savedStateHandle.toRoute<…>()`. **[Known – recheck]** Most Compose UI tests target stateless screens and need no Hilt. A few end-to-end tests use `HiltAndroidRule` with `@TestInstallIn` to swap in an in-memory database, a temporary-file DataStore and a fixed clock.

## 14. Milestones (small and buildable)

Each milestone ends with a green build, passing tests and something installable.

| ID | Outcome | Tasks | Gate |
|---|---|---|---|
| M0.1 | Toolchain spike: empty app with Hilt + Room + KSP + Compose compiles and runs | T-001 | Clean build on CI and emulator; merged manifest has no permissions |
| M0.2 | `:domain` money and date core | T-002 | Parser, formatter and month tests green (§15) |
| M0.3 | Theme tokens, app shell, 4 tabs + Settings route | T-005 | Light/dark check; nav UI tests |
| M0.4 | DataStore settings + ONB-01 + splash gate | T-004, T-006 | AC-ONB-01/02/03 |
| M0.5 | Room database foundation and full schema with FK tests | T-003 | Schema and FK tests green |
| M1.1 | Personal repository (atomic, idempotent) | T-007 | Duplicate-save and round-trip tests |
| M1.2 | EXP-01 form | T-008 | AC-PER-01..04, 06, 07, 08 |
| M1.3 | Today + EXP-02 + delete with Undo | T-009, T-010 | AC-PER-05 |
| M2.1 | Activity feed + filters | T-011, T-012, T-014 | AC-ACT-01..05 |
| M2.2 | Insights | T-013 | AC-INS-01..04 |
| M3.1 | Split engine, written test-first | T-017 | Fixtures 1–4, AC-SHR-02/09 |
| M3.2 | Groups and members | T-015, T-016, T-020 | AC-GRP-01..07 |
| M3.3 | Shared expense save, form and detail | T-018, T-019 | AC-SHR-01..10 |
| M4.1 | Balance engine, suggestions, review detector | T-021, T-022, T-025 | Σnet = 0 property tests; AC-BAL-01/07/08 |
| M4.2 | Settlements and Balances screen | T-023, T-024 | AC-BAL-02..06 |
| M5 | A11y, adaptive, failure injection, release audit | T-026..T-030 | All P0 acceptance tests |

## 15. Tests for the first milestone (M0)

### 15.1 Unit tests — `:domain` (JVM, fast)

**`MoneyParserTest`** (use `MAX_ENTRY_PAISE` from §3.1)

| Input | Expected |
|---|---|
| `"125.50"` | 12550 |
| `"125.5"` / `"125"` / `".5"` / `"5."` | 12550 / 12500 / 50 / 500 |
| `"0.01"` | 1 |
| `" 125.50 "` | 12550 (trimmed) |
| `"1,234.50"` | 123450 (AC-PER-07) |
| `"1,23,450.50"` | 12345050 |
| `""` | `Empty` |
| `"0"`, `"0.00"` | `NotPositive` |
| `"-1"`, `"abc"`, `"₹5"`, `"१२"` | `Invalid` |
| `"12.345"` | `TooManyDecimals` (no rounding) |
| `"1.2.3"`, `"1,,2"`, `",12"`, `"12,"`, `"1.2,3"` | `Invalid` |
| exactly max | valid |
| max + 1 paisa | `OverLimit` |
| `"99999999999999999999"` | `OverLimit`, with no exception |

**`InrFormatterTest`**: 12550 → `₹125.50`; 123450 → `₹1,234.50`; 10000000 → `₹1,00,000.00` (decimals rule per §3.4); 1 → `₹0.01`; max value groups correctly. Results must be the same no matter the default locale (run the test with `Locale.GERMANY` set as default).

**`MonthRangeTest`**: 2026-10-20 → Oct 1..31. Leap Feb 2028 → 29 days. Dec 2026 → next is Jan 2027. Previous and next stay correct across year boundaries.

**`DateRulesTest`**: 2000-01-01 and 2100-12-31 are allowed. 1999-12-31 and 2101-01-01 are rejected.

**`UtcPickerConversionTest`**: UTC midnight millis for 2026-10-20 → `LocalDate` 2026-10-20 while the default zone is set to `America/Los_Angeles`, `Asia/Kolkata` and `Pacific/Kiritimati`.

**`DisplayNameRulesTest`**: `"  Sam  "` → `"Sam"`; 40 characters is OK; 41 is an error; blank with Start is an error; blank with Skip → `"Me"`.

### 15.2 Unit tests — `:app` JVM

**`SettingsRepositoryTest`** (DataStore on a temporary file; datastore-preferences-core runs on the JVM **[Known – recheck]**): default onboarding flag is false and theme is System. `completeOnboarding("Sam")` sets the flag and the name. A new DataStore instance on the same file reads the same values (restart simulation). `setTheme(Dark)` persists.

**`OnboardingViewModelTest`** (fake settings repository, `StandardTestDispatcher`, Turbine):

| Case | Expected state |
|---|---|
| Initial | name field = `"Me"`, no error |
| Start with valid name | one write, then `completed = true` |
| Start tapped twice quickly | exactly **one** write |
| Write throws | `saveFailed = true`, still on screen, input kept |
| Retry after failure | succeeds |
| Skip with blank | stores `"Me"` |

**`ThemeModeTest`**: the function that maps `ThemeMode` + system dark flag → dark/light gives the correct result for all 6 combinations.

### 15.3 Room tests (androidTest, in-memory DB)

`SchemaCreatesTest`. `ForeignKeyTest`: inserting a share for a missing expense fails; deleting a member referenced by a share fails (RESTRICT); deleting a shared expense removes its shares (CASCADE). `UniqueMemberNameTest`: `"Alex"` then `"alex"` in the same group fails, but works in another group. `PersonalRoundTripTest`: the max paise value and nullable title round-trip exactly.

### 15.4 Compose UI tests (androidTest)

| Test | Steps | Assert |
|---|---|---|
| `onboarding_shownOnFreshInstall` | Launch with empty DataStore | Title, privacy footer text and "Start tracking" are displayed |
| `onboarding_startGoesToToday_andDoesNotRecur` | Type "Sam", tap Start, then `ActivityScenario.recreate()` | Today shown both times; onboarding not shown again |
| `onboarding_backFromTodayExits` | Complete onboarding, press Back | Activity finishes (back stack cleared) |
| `onboarding_skipUsesMe` | Clear field, tap Skip | Profile name = "Me" |
| `onboarding_tooLongNameShowsError` | Type 41 characters, tap Start | Error text shown; field is focused |
| `shell_fourTabsWithLabels` | Open shell | 4 items with visible text labels; Today `assertIsSelected()` |
| `shell_tabSwitching` | Tap each tab | Matching screen heading shown; selected state moves |
| `shell_settingsFromTodayMenu` | Open overflow, tap Settings | Settings heading shown |
| `a11y_touchTargets` | Onboarding and shell | Buttons and tabs `assertTouchHeightIsAtLeast(48.dp)` |
| `a11y_headings` | Each screen | Title has `heading()` semantics |
| `fontScale200_startReachable` | Wrap with font scale 2.0 **[Known – recheck: `DeviceConfigurationOverride`]** | `performScrollTo()` then Start `assertIsDisplayed()` |
| `manifest_noPermissions` | Read `PackageInfo.requestedPermissions` | Null or empty (AC-GEN-06) |

Manual checks for M0: airplane mode first run, dark and light themes, TalkBack reads the onboarding footer, Android 16+ emulator.

## 16. Open decisions for the product owner

1. Maximum entry amount: 1e9 or 1e10 paise (§3.1).
2. Decimal display rule (§3.4).
3. "Review balances" trigger (G4).
4. Device-transfer backup on or off (G9).
5. Navigation Compose 2.10 or Navigation 3 (default: 2.10, per the brief).
6. Material 3 1.5.0 adoption point (default: re-check at M3 start).

---

# Ordered implementation checklist

**M0.1 — Toolchain spike**
1. [ ] Create the project with `:app` and `:domain`, a version catalog, and the BOM 2026.09.00.
2. [ ] Pick a Kotlin version that the current KSP and Dagger releases support. Compile one `@Entity` and one `@HiltViewModel` to prove it.
3. [ ] Confirm the compileSdk that Compose 1.12 needs (36 vs 37). Set targetSdk 36 and minSdk 26.
4. [ ] Add `dataExtractionRules` and backup rules (product to confirm G9).
5. [ ] Add a CI task: `assembleDebug`, `test`, `lint`, and a merged-manifest check for no permissions.
6. [ ] Write `DECISIONS.md` with the decisions from §1 and the doc fixes from §3.

**M0.2 — Money and time core**
7. [ ] Add `Paise`, `MoneyLimits` (agreed max), `parseInrAmount` and `formatInr`, with the §15.1 tests.
8. [ ] Add `MonthRange`, `DateRules`, the UTC picker conversion and display-name rules, with tests.

**M0.3 — Theme and shell**
9. [ ] Map DESIGN_SYSTEM tokens to `ColorScheme`, typography, shapes and fixed status colors. Add the tabular-figures check.
10. [ ] Build `core/ui/components` wrappers: `AmountField`, `MoneyText`, `StickySaveBar`, `ConfirmDialog`, `EmptyState`.
11. [ ] Build the app shell with 4 tabs, Settings in the Today menu and type-safe routes. Add the shell UI tests.

**M0.4 — Settings and onboarding**
12. [ ] Add `SettingsStore` and `SettingsRepository` (onboarding flag, name, theme), with JVM tests.
13. [ ] Add the splash gate and conditional start destination.
14. [ ] Build ONB-01 (screen, ViewModel, tests) to pass AC-ONB-01/02/03.

**M0.5 — Database**
15. [ ] Create the full Room schema (§8) with exported schema and foreign keys. Add the Room tests (§15.3).

**M1 — Personal capture**
16. [ ] Personal repository: create, update, delete, observe; repository-side validation; idempotent insert.
17. [ ] EXP-01 form: amount-first, IME rules, `BackHandler` discard prompt, `SavedStateHandle` draft.
18. [ ] Today screen (HOM-01) with month total and recent rows.
19. [ ] EXP-02 detail screen with delete and in-memory Undo (G3).

**M2 — Browse and understand**
20. [ ] Activity repository combining personal, shared and settlement flows with a stable sort (date, createdAt, id).
21. [ ] ACT-01 with search, scope, month, type and category filters; state kept across tabs and process death.
22. [ ] INS-01 with month total, category rows and percentages, and a text-first layout.

**M3 — Groups and splits**
23. [ ] Write the `StableIdOrder` and `SplitCalculator` tests first (fixtures 1–4, AC-SHR-02/09).
24. [ ] Group and member repository with unique names, deactivation and archive.
25. [ ] GRP-01..04 screens.
26. [ ] Shared expense repository: parent + shares in one transaction; edit replaces shares atomically.
27. [ ] SHR-01 form (Equally / Exact, Remaining summary) and SHR-02 detail.
28. [ ] Re-check Material 3 1.5.0 status and decide whether to adopt it.

**M4 — Balances and settlements**
29. [ ] Write `BalanceCalculator`, `SettlementSuggester` and `ReviewDetector`, with property tests that Σnet = 0.
30. [ ] Settlement repository: recompute and validate inside the transaction.
31. [ ] BAL-01, SET-01 and SET-02, including the "Review balances" advisory.

**M5 — Hardening and release**
32. [ ] TalkBack pass, 200% font, 48dp targets, spoken money format (G11).
33. [ ] Adaptive layouts, landscape, IME, rotation and process-death checks on Android 16+.
34. [ ] CFG-01 Delete all data (G13).
35. [ ] Failure injection: write errors, double save, missing records.
36. [ ] Freeze schema v1, run the full acceptance regression, write the store description.
