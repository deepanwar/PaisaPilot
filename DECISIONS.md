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

## Open

- Max entry amount. PRODUCT §6 says ₹10,00,00,000.00 and also 1,000,000,000 paise. Those differ by 10×. Pick before M0.2.
- Decimal display. Always two decimals, or hide `.00`. Pick before the formatter.
- Review-balances trigger (G4). Pick before M4.
- Navigation Compose 2.10.2 or Navigation 3. Default when navigation is added: 2.10.2.
- Material 3 1.5.0. Re-check at the start of M3. v1 stays on the stable 1.4.0 line from the BOM until then.
