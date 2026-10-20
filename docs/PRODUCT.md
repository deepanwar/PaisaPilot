# Expense Tracker — Product Requirements (v1.0)

**Status:** Build-ready product specification | **Platform:** Android, Kotlin + Jetpack Compose | **Locale:** en-IN | **Currency:** INR only in MVP | **Data:** local-first, single device | **Updated:** 2026-10-20

## 1. Vision and product promise
Record a payment in seconds, see exactly where this month's money went, and understand who owes whom for household expenses. The app is a **personal finance journal with an optional local shared ledger**, not a payment service, bank connector, or multi-user collaboration service.

**North-star task:** Save a straightforward personal expense in ≤10 seconds after becoming familiar with the app (product target, not a measured claim). **Primary product qualities:** speed, clarity, mathematical correctness, privacy, recoverability, accessibility.

### Target users and jobs-to-be-done
| Persona | Situation | Job | Friction addressed |
|---|---|---|---|
| Individual tracker | Records purchases throughout the day | “Capture the amount now, understand patterns later” | Complex forms, forgotten payments |
| Flatmate/household organizer | Often pays for groceries/rent/utilities | “Keep one ledger of who paid and who benefited” | Chat-based calculations, disputed sums |
| Shared-expense participant | Uses the same device with the organizer to review entries | “See what I owe and which transfers were settled” | Opaque balances and inconsistent rounding |

**Important limitation:** Other members do **not** get access to the data on their own devices in v1; they are names in a single on-device ledger. UI copy must explicitly say “Saved on this device; members aren't notified.”

## 2. Success definition and instrumentation
No analytics SDK or account required. Measure during manual QA or opt-in local diagnostic sessions: median add flow duration from tap on Add to success, number of taps for typical entry, successful save rate, ledger consistency, offline success, and accessibility coverage. Suggested release gates: common add path ≤4 taps excluding number-key presses; no persisted partial transactions; sums and balances exact to the paisa; zero blocker crashes in a fixed 50-scenario regression suite; large-text and TalkBack critical paths pass. Targets need real-device validation, not promises.

## 3. MVP boundaries and explicit decisions
| P0 / included in initial release | P1 / next release | Later / out of scope |
|---|---|---|
| Local personal expenses with amount, title, category, date, optional note | Recurring templates, budget alerts, enhanced charts | Cloud backup, multi-device sync, login, collaboration/invites |
| Add, inspect, edit, delete with undo where safe | CSV export/import, portable backup | Google Sheets sync, bank/SMS ingestion, receipt OCR |
| Personal transaction list, search, scope/category/month filters | Custom categories, attachments, widgets, shortcuts | Automatic payment detection, AI categorization |
| Monthly totals and category breakdown (personal only) | More report periods and trends | Multi-currency, income accounting, tax support |
| Local groups and members; equal/custom splits | Percentage splits, default shares | Real-time group membership, notifications, online group IDs |
| Group expense with one payer, settlement records, net balances | Split suggestions optimized for minimum transfers | Payments/transfers inside app, partial multi-payer expense |
| Offline persistence, theme, accessibility, local deletion warning | Encrypted export and device backup settings | Server-side audit and cross-device conflict resolution |

**Scope guard:** Manual settlement records are P0 because owed amounts must be actionable; payment integration is not. Onboarding is one brief setup page, not a tour. Category and group names are local.

## 4. Information architecture
- **Today** (`/home`): personal month-to-date total, recent personal transactions, strong Add entry point, shortcut to shared groups. Shared spending never enters personal total.
- **Activity** (`/activity`): combined timeline of *personal entries and group expenses/settlements* with clearly labeled types and scopes; search and filters. Totals are shown only for explicit scopes, not as a sum of mixed records.
- **Groups** (`/groups`): group list, local balances; details, members, group activity, group add, settle flows.
- **Insights** (`/insights`): personal monthly spending and category breakdown; period picker. Group totals remain in group detail.
- **Settings** (`/settings`): display name, theme, privacy/local-storage explanation, delete-all-data action, app information.

On compact width, 4 bottom destinations: **Today, Activity, Groups, Insights**. Settings in Today top-app-bar menu. An Add floating action button on Today and Activity; in Group detail, explicit “Add shared expense” action. On wide windows, use navigation rail and place list/detail side-by-side when appropriate. Full hierarchy and screen inventory in `SCREEN_SPECS.md`.

## 5. Key journeys (canonical success paths)
1. **First launch:** Launch → one-screen explanation “Your data stays on this device” → enter display name (or retain “Me”) → Start tracking → Today empty state → Add. Permission and sign-in prompts: none.
2. **Add personal:** Today → Add → amount focused with numeric keyboard → optional title/category/date → Save → Today amount and recent list update; success feedback. Default title derived from chosen category only if user doesn't specify one; otherwise “Expense” as explicit generic label.
3. **Edit/delete:** Activity or Today row → Expense detail → Edit → change fields → Save atomically → see updated list/insights; detail → Delete → confirmation → removal with Undo (short-lived) if available; Undo restores same ID.
4. **Browse:** Activity → All scopes by default → choose Personal or specific Group → optional month/category/search/type filters → select row → detail. Back preserves list state; “Clear filters” resets.
5. **Monthly:** Insights → selected calendar month (defaults current local month) → total paid personal spending, category amounts/shares, recent personal list → previous/next month; empty month explains no records.
6. **Create group:** Groups → New group → name → add local members (including “Me” by default) → Create → group detail. A group needs ≥2 active members to record shared expenses.
7. **Add shared:** Group detail → Add shared expense → amount/title/date/category → single payer → members and split method (equal by default, editable exact amounts) → live per-member shares and validation → Save atomically → group ledger/balances refresh. Members get **no notification**.
8. **Owed and settle:** Group detail → Balances → see net “A owes B ₹…” recommendations and per-person net amount → Record settlement → payer (debtor), receiver (creditor), amount/date/note → Save → balances updated. Transfer occurs *outside* the app; this is a record, not a payment.

### Primary design trade-offs
- **Speed vs completeness:** only amount required to start; less-used fields progressively disclosed; amount/title/category/date still reviewable before save.
- **Household support vs collaboration:** single-device group ledger shipped first; do not portray local member names as connected accounts.
- **Correct accounting vs simplicity:** integer paise, deterministic rounding, explicit payer and beneficiaries, immutable IDs, transactional recomputation; accept a small amount of domain logic rather than shipping misleading balances.
- **Deletion vs trust:** editing shared expense recomputes balances immediately; removing group members with history is restricted to avoid silent ledger corruption.
- **Expressiveness vs finance legibility:** expressive hero hierarchy and shapes, restrained surfaces; money, signs and dates always plainly legible.

## 6. Domain model
| Concept | Essential fields | Rules |
|---|---|---|
| Profile | id, displayName | Exactly one local owner, no authentication |
| Category | stable key, display label, icon | Fixed starter categories: Food, Transport, Shopping, Bills, Home, Health, Entertainment, Other |
| PersonalExpense | id, amountPaise, title, categoryKey, occurredAt, note?, createdAt, updatedAt | Positive amount; separate from group ledger |
| Group | id, name, createdAt, archivedAt? | At least 2 active members for new shared expense; archive rather than erase history |
| Member | id, groupId, name, isLocalOwner, active | Group-local identity; names need not be globally unique, but must be unique case-insensitively within group |
| SharedExpense | id, groupId, amountPaise, title, categoryKey, occurredAt, payerMemberId, note?, createdAt, updatedAt | One payer; ≥1 beneficiary; cannot link to personal expense |
| ExpenseShare | sharedExpenseId, memberId, sharePaise | Unique per expense/member; sum exactly equals expense amount |
| Settlement | id, groupId, fromMemberId, toMemberId, amountPaise, occurredAt, note?, createdAt, updatedAt | Positive recorded transfer; distinct members; no actual money transfer |

IDs are stable UUIDs; all money **signed 64-bit integer paise** (no floating point storage). Times stored as instants for metadata and as a local calendar date for the expense's reporting day; user-selected date must not change when timezone changes. INR formatted per `en-IN`, two decimals where paise nonzero and consistent grouping; don't silently round or truncate input. Upper limit per entry **₹10,00,00,000.00** (=1,000,000,000 paise), including guard for aggregate overflow. Negative expense/refund and income are deferred; no zero-amount entries.

## 7. Accounting rules (single source of truth)
### Personal
Personal month total = sum of PersonalExpense.amountPaise whose local `occurredDate` falls within the month. Group payments, group shares and settlement transfers are **excluded**. Group month spending = sum of SharedExpense.amountPaise in that group/month, regardless of who paid.

### Shared splits
For each shared expense, let `A` be amount in paise, `payer` one member, `s_i` each beneficiary share, and `Σ s_i = A`. Per-member net contribution for that expense: `net_i = (i == payer ? A : 0) - s_i`. Positive means credit (“is owed”), negative means debit (“owes”). Example: A pays ₹100.00 and equally splits between A, B, C: shares ₹33.34, ₹33.33, ₹33.33 in stable member-ID ascending order for extra paise. Contributions: A +₹66.66, B −₹33.33, C −₹33.33.

**Equal split:** calculate quotient `floor(A/n)`; distribute remaining paise 1 each to included members in ascending stable `memberId` order, not visual ordering. At least one member selected, and amount must be ≥ number of included members **if zero shares are disallowed**; MVP instead permits zero-paize individual equal-split shares for tiny amounts and labels them explicitly, keeping `Σ=A`. **Custom amount:** explicit integer-paise shares for selected beneficiaries, nonnegative, sum equals `A`; zero-share members may be excluded automatically on save after confirmation if none benefits. Unselected members have no share. Payer may or may not be a beneficiary; if payer excluded, payer credit is full amount.

### Settlements and balances
Settlement from A to B for `T` paise changes `net_A += T`, `net_B -= T` (reduces A's debt to B when A is debtor). The group net position is sum of all expense nets plus all settlement nets. **Invariant `Σ net_i = 0`.** For display, derive debt suggestions greedily from sorted debtors and creditors (largest outstanding absolute value, then stable member ID), transfer the min of their residual amounts, repeat until zero. Suggested pairs are **a derived allocation, not historical bilateral obligations**; label as “Suggested settlements”. Display aggregate “You owe / You're owed” based on owner's net position; do not claim a group member literally paid another without a settlement record.

**Settlement validation:** from ≠ to; amount > 0; amount ≤ current suggested debt between selected from/to **in the preview computed at save time**. Only suggested debtor→creditor pairs selectable in MVP; otherwise show instruction to use a current suggested pair. Serialize saves; recompute under a database transaction to avoid stale balances. If old edits cause historic settlement to exceed a now-computed suggestion, preserve the settlement record and recalculate signed net positions; mark group “Review balances” instead of deleting or fabricating transfers. During ordinary settlement creation, enforce positive existing suggestion.

### Edit/delete and membership
Editing expense amount, payer or shares overwrites the original expense and its shares **within one database transaction**, then recomputes current balances; no duplicate reversal transaction. Editing a settlement likewise replaces its fields atomically; deletion removes it and recomputes. Deleting a shared expense with later settlements may materially change net obligations; show clear warning. Undo deletion is available only for entries whose full snapshot can be safely restored; implementation may use soft-deletion pending snackbar and finalize afterward. Group members referenced by expenses/settlements cannot be permanently deleted; mark inactive (hide from new split defaults), preserve prior history. No rename of group member after first shared record by default (deferred to avoid identity confusion); new members may be added. Group with history can be archived, not hard-deleted in MVP. A group with no history may be deleted after confirmation.

## 8. Edge cases and privacy
- Offline is normal operating mode: all core operations must work without internet; do not show network spinners.
- One local device is the **source of truth**. Uninstall, data clear or device loss can remove entries; display this in onboarding and Settings. Do not promise automatic backup; exclude unintended OS auto-backup until explicit backup semantics and encryption are defined.
- Require a usable empty state for zero personal expenses, zero groups, empty month, no search matches, and group with zero balances.
- Support same-date entries and deterministic secondary order by createdAt then ID; month based on stored reporting date, not current timezone conversion.
- Android process death during entry preserves draft as saved UI state when feasible but **never** persists an incomplete ledger record; after app termination completed data remains.
- Confirm destructive actions; provide accessible messages, not only color or transient snackbars. Avoid showing group owed amounts as the user's monthly spending.

## 9. Open questions and chosen defaults
| Ambiguity | MVP assumption | Revisit when |
|---|---|---|
| Multi-device sharing? | Single-device local group book; no member login or notification | Cloud collaboration is planned |
| Personal paid for group expense? | Group transaction **only**, never double-count in personal Insights | Consolidated cashflow report exists |
| Income, refunds and negative entries? | Not supported | Financial model expands |
| Multiple payers in one expense? | Exactly one payer | Multi-payer workflow validated |
| Balances pairwise or netted? | Net group positions + derived suggested settlement pairs | Users request strict bilateral accounting |
| Default date/time? | Today's device-local date; no time-entry field | Time-of-day analysis desired |
| Is every member included in split? | All active members initially selected; may deselect any | Default split preferences added |
| Currencies and amount precision? | INR, two decimal places, integer paise | Multi-currency support |
| Date limits? | Any valid date from 2000-01-01 through 2100-12-31 | Import requires wider history |
| Delete app data? | Settings destructive confirmation deletes all local records | Backup exists |

## 10. Security, integrity and implementation guidance
Use Room for related transactions/shares and referential integrity; DataStore for lightweight settings such as onboarding completion and theme. No cloud, permissions for contacts/SMS or financial credentials. Repository/domain rules should be testable independent of Compose, with simple screen ViewModels and unidirectional state. Avoid network, payments, DI framework requirement or premature modularization. Follow Android current APIs and verify exact dependency versions before writing code. See `TASKS.md` for dependency milestones.

## 11. Reference guidance
- Android official Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- DataStore guidance: https://developer.android.com/topic/libraries/architecture/datastore
- Adaptive navigation: https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation

Related docs: [Screens](SCREEN_SPECS.md) · [Design](DESIGN_SYSTEM.md) · [Acceptance](ACCEPTANCE_CRITERIA.md) · [Tasks](TASKS.md)
