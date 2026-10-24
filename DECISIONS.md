# Decisions

Accepted for the M0.1 toolchain spike on 2026-10-20. Product docs were not edited.

## Accepted

### Two modules

TASKS §1 says one Android module. ARCHITECTURE §6 adds `:domain`. Accepted so money and split logic cannot import Android or Room.

### Application id

Stays `com.deepak.paisapilot`. The `app.ledger` package in ARCHITECTURE is the working brand name Ledger, not this repository. Source packages stay under `com.deepak.paisapilot`.

### SDK and JVM

- compileSdk: 37 (`release(37)`)
- targetSdk: 36
- minSdk: 26
- Java and Kotlin bytecode: 17
- AGP 9.4.1 and Gradle 9.6.0, kept from the template

### Kotlin, KSP, and Hilt

The spike compiled and its unit tests passed with:

- Kotlin 2.4.21
- KSP 2.3.12
- Hilt 2.60.1 (newer than the 2.59.2 AGP 9 floor)
- AndroidX Hilt `hilt-lifecycle-viewmodel-compose` 1.4.0

No fallback to Kotlin 2.3.21 was required for `assembleDebug`, `test`, or `lint`. Jetifier stays off.

### UI toolkit

Compose BOM `2026.09.00`. Material 3 comes only from that BOM. No experimental Expressive APIs.

### Storage libraries

Room 2.8.5 (`room-runtime`, `room-ktx`, `room-compiler` via KSP, `room-testing`). Not Room 3. DataStore is not a dependency yet. Backup rules already exclude its files.

### Network

No Retrofit, OkHttp, or `INTERNET` permission.

### Backup

`allowBackup` is false. `data_extraction_rules.xml` excludes every database file and the `datastore` directory from cloud backup and device transfer. `backup_rules.xml` has the same excludes for older APIs. This is architecture G9's proposed default. Overturn it if device transfer should keep the ledger.

On Android 12 and newer, extraction rules override `allowBackup`. The rules therefore have to name the ledger files. Other app files can still transfer.

### Injected signature permission

AndroidX merges a signature permission, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, for an internal receiver. The app manifest removes that `<permission>` and `<uses-permission>`. v1 requests no permissions. The merged debug manifest check fails if any `<uses-permission>` remains.

### Probe schema

`ToolchainProbeDatabase` is a compile proof. It is not schema v1. `exportSchema` is false. Delete it when T-003 adds the ledger tables. The file name is `paisapilot.db`.

### Max entry amount

`MAX_ENTRY_PAISE` is 10,000,000,000 (₹10,00,00,000.00). PRODUCT’s old parenthetical of 1,000,000,000 paise was 10× too small. Acceptance fixture 6 uses the rupee figure. Defined once in `:domain`.

### Decimal display

`formatInr` always prints two decimals, including `₹125.00`. Whole amounts do not drop `.00`.

### Navigation

Navigation Compose 2.10.2 with the Kotlin serialization plugin 2.4.21 and `kotlinx-serialization-json` 1.11.0. The shell uses a stable `NavigationBar`. `NavigationSuiteScaffold` stays out until M5, and it needs an experimental-API check before use.

`TopAppBar` is still `@ExperimentalMaterial3Api` in Material 3 1.4.0. `ShellTopBar` opts in for the plain bar only. Scroll behavior stays unused.

### Date picker and form top bar (M1.2)

`DatePickerDialog`, `DatePicker`, `rememberDatePickerState` and `SelectableDates` are `@ExperimentalMaterial3Api` in Material 3 1.4.0. `core/ui/DateSelectDialog.kt` is the only file that opts in. It uses the millisecond API and converts with `ZoneOffset.UTC` (G2). The expense form draws its own plain `TopAppBar` (same opt-in as `ShellTopBar`) so its back arrow can run the discard prompt. The shell hides its top and bottom bars on that route and consumes the Scaffold insets so the IME padding in `StickySaveBar` is applied once.

### Today, detail and Undo (M1.3)

Today lists the 10 newest rows of the selected month (`RECENT_LIMIT`); the hero total counts every row. "View all activity" opens the Activity tab when more exist. The "Shared groups" link is left out until M3. The selected month is kept in `SavedStateHandle` and can move between 2000-01 and 2100-12.

Undo (G3): `UndoDeleteCoordinator` (singleton) keeps the last deleted personal expense in memory. The detail screen offers it after a successful delete. `UndoSnackbarEffect` in the shell shows the snackbar, so it works whatever screen the detail pops to. Undo calls `restore`, which re-inserts the same id and timestamps. A late dismissal cannot clear a newer snapshot. The snapshot is lost if the process dies.

`ShellTopBar` marks its title as a heading. Placeholder screens no longer do, so each screen has one heading.

### Activity feed (M2.1)

`ActivityRepository` is the single source for the feed. Today it wraps personal expenses; M3 and M4 add shared expenses and settlements to its `combine`. Filtering runs in memory (`applyActivityFilter` in `:domain`), because a local ledger is small and the search rules stay in one tested place. Search is trimmed and case-insensitive over title, note and category label; a null title shows the category, so the label has to be searchable. Category labels reach the ViewModel through `CategoryLabelProvider`.

The month filter is a plain dialog of months that have entries plus "All months", which avoids more experimental date-picker APIs. Filters live in `SavedStateHandle` and the list state uses `rememberSaveable`, so they survive tab switches, a detail screen and process death. The mixed feed shows no combined total; a total appears only when the scope is Personal.

### Insights (M2.2)

`summarizeMonth` in `:domain` builds the total, count and category rows from one month of expenses. Percentages are whole numbers rounded half up from integer paise and may sum to 99 or 101; amounts are authoritative. The view model reads one `observeMonth` query, so total, categories, count and the 5 recent rows cannot disagree. The category bars are decorative and hidden from accessibility.

Tapping a category opens Activity for that month and category through `ActivityFilterRequests`, a singleton holding one pending filter. Navigation arguments would lose to the Activity tab's saved state, so `ActivityViewModel` takes the request and applies it through its normal, persisted filter update.

### Balances and settlements (M4)

`computeExpenseNets` (E) and `computeNets` (N, with settlements) are checked-arithmetic top-level functions in `:domain`, as are `suggestSettlements` and `needsBalanceReview`. The "Review balances" trigger (G4) is signed off as written in ARCHITECTURE: flag the group if any member has N != 0 with the opposite sign to E, or E = 0 and N != 0. It is informational only and never changes a recorded settlement.

`validateSettlement` checks a new settlement against the suggestions derived from the stored expenses and the other settlements, so the pair must be a current suggested pair and the amount at most the suggestion. An edit is checked against balances that exclude the record being edited. A legacy edit keeps the same pair and may lower the amount even when later expense edits mean the pair is no longer suggested. `RoomSettlementRepository` recomputes and validates inside the write transaction.

Group detail lists settlements in their own "Settlements" section, not mixed into the shared-expense list, so they are never read as group spending. Settlements in the Activity feed and a real owner net on the group list rows are not part of M4.

## Open


- Material 3 1.5.0. Re-check at the start of M3. v1 stays on the stable 1.4.0 line from the BOM until then.
