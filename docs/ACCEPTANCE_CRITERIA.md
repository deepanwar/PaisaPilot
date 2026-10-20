# MVP Acceptance Criteria and QA Matrix — v1.0

**Format:** Given / When / Then. **Testing layers:** domain unit tests, Room/integration tests, Compose UI tests, manual accessibility/device checks. Criteria are release gates unless flagged as performance targets. Use fixed `Clock` and predictable IDs in tests.

## Release-wide gates
- **AC-GEN-01:** With airplane mode on, first-run setup, personal CRUD, groups, splits, settlements, insights, filters and theme work without account or network.
- **AC-GEN-02:** After any successful write, force-stop/relaunch retains the committed state; app kill mid-save never yields half-created shares, missing members or unbalanced ledgers.
- **AC-GEN-03:** Financial amounts persist in integer paise; never computed from floats; every saved shared expense has `sum(shares)=amount`, every group net sums exactly to 0.
- **AC-GEN-04:** TalkBack can navigate every critical path and announces money sign/direction, field label, selected filter, validation error and dialog decision. All essential touch targets at least 48dp; core flow usable at 200% font scale.
- **AC-GEN-05:** Dark/light themes, IME, Back, edge-to-edge safe areas, rotating screen and process restoration do not hide Save, corrupt data, or crash.
- **AC-GEN-06:** No auth, internet, notification or sensitive permissions requested for v1. Local-data-loss disclosure visible onboarding/Settings.

## Onboarding and personal expense
| ID | Given | When | Then |
|---|---|---|---|
| AC-ONB-01 | Fresh install | Open app | ONB-01 appears; no login/permissions; local-only caveat shown |
| AC-ONB-02 | First launch | Enter “Sam” and Start | Today opens; name stored; onboarding does not recur on restart |
| AC-ONB-03 | Blank name | Choose Skip | Profile is “Me”, Today opens |
| AC-PER-01 | Today | Add -> type `125.50` -> Save | one expense stored for today in Other; Today and Activity show ₹125.50 |
| AC-PER-02 | Add form | Type `0`, `-1`, `12.345`, letters, or >limit | inline error and no save; existing values not silently rounded |
| AC-PER-03 | Valid amount | Set title, category, prior date, note; Save | exact fields persist and selected month total changes |
| AC-PER-04 | Recorded expense | Edit ₹125.50 → ₹225.50 | same ID, one record only, total changes by ₹100.00 |
| AC-PER-05 | Recorded expense | Delete, confirm | row absent and month/category aggregates updated; Undo returns same record where offered |
| AC-PER-06 | Dirty form | Press system Back | Discard/Keep editing prompt; Keep retains content |
| AC-PER-07 | Amount field | Paste `1,234.50` and Save | persists 123450 paise; shows valid en-IN currency presentation |
| AC-PER-08 | Duplicate tap risk | Tap Save quickly twice | only one record saved |

## Activity and monthly insights
| ID | Given | When | Then |
|---|---|---|---|
| AC-ACT-01 | Entries across dates/scopes | Open Activity | correctly dated, stable order; personal/shared/settlement labeled |
| AC-ACT-02 | Personal, group and settlement entries | Search and filter to Personal + Food + month | only matching personal records shown; count correct |
| AC-ACT-03 | No matching records | Search impossible term | no-match state and Clear filters available |
| AC-ACT-04 | Filters and scroll applied | Open detail then Back | prior filters, selection and scroll retained |
| AC-ACT-05 | Mixed feed | View All scopes | no aggregate total misleadingly combines group and personal activity |
| AC-INS-01 | Personal Food ₹100, Bills ₹50 in a month | Open that month | total ₹150, category amounts ₹100/₹50; correct count |
| AC-INS-02 | Shared expense ₹300 and settlement ₹150 also recorded | View personal Insights | neither affects ₹150 personal total |
| AC-INS-03 | No expenses in selected month | Open month | total ₹0 and useful empty state; no fake chart data |
| AC-INS-04 | Backdated expense edited to another month | Open both periods | source month decreases and destination month increases accurately |

## Groups and members
| ID | Given | When | Then |
|---|---|---|---|
| AC-GRP-01 | No groups | Open Groups | clear new-group CTA and single-device disclaimer |
| AC-GRP-02 | Group form | Name “Flat”, members Me + Alex, Create | one group and two member IDs persist, detail opens |
| AC-GRP-03 | Same name member `Alex` exists | Add ` alex ` | validation prevents duplicate (case-insensitive/trimmed) |
| AC-GRP-04 | Group has 1 person or >30 attempt | Create/add | appropriate validation; no corrupt group saved |
| AC-GRP-05 | Group with existing history | Add Bea | prior splits and balances unchanged; Bea available for new entries |
| AC-GRP-06 | Member referenced historically | Attempt remove member | no hard deletion; history preserved; inactive member omitted from future default choices |
| AC-GRP-07 | Active group has historical events | Archive | read-only thereafter, history visible, archive warning explains restrictions |

## Shared expense and rounding
| ID | Given | When | Then |
|---|---|---|---|
| AC-SHR-01 | Group Me/Alex | Record Me paid ₹200 equally for both | shares ₹100 each, Me net +₹100, Alex −₹100 |
| AC-SHR-02 | Group with 3 members with stable IDs | Record payer A ₹100.00 equally for all | shares deterministic ₹33.34, ₹33.33, ₹33.33; net sum 0 |
| AC-SHR-03 | A pays ₹10; only B selected | Save | A full ₹10 credit, B ₹10 debit, zero self-share |
| AC-SHR-04 | Exact split ₹100 entered shares ₹60 + ₹30 | Save | blocked with Remaining ₹10.00 and clear error |
| AC-SHR-05 | Exact split ₹100 entered ₹60 + ₹40 | Save | share sum exact, payer balance and group total update |
| AC-SHR-06 | Exact split input established | Change total ₹100→₹110 | shares stay ₹60/₹40 and Remaining ₹10 until user resolves |
| AC-SHR-07 | Valid group expense exists | Edit amount/payer/selected shares and Save | same expense ID, shares replaced atomically, no duplicated balance contribution |
| AC-SHR-08 | Expense with downstream settlement | Delete expense after warning | group nets recomputed; settlement record remains and review advisory shown if applicable |
| AC-SHR-09 | Smallest amount ₹0.01 split among 3 | Save equal | shares ₹0.01/₹0/₹0 deterministic; sum equals ₹0.01 |
| AC-SHR-10 | Group entry present | Inspect personal Insights | no group entry included |

## Balances and recorded settlements
| ID | Given | When | Then |
|---|---|---|---|
| AC-BAL-01 | A net +₹100, B net −₹60, C net −₹40 | View suggestions | B→A ₹60, C→A ₹40; remaining net 0 after both |
| AC-BAL-02 | A paid and split expense | B records suggested B→A ₹40 | B net increases ₹40, A net decreases ₹40; expense and group spend unchanged |
| AC-BAL-03 | Group is balanced | Open Balances | Everyone is settled, new settlement disabled |
| AC-BAL-04 | Suggested B→A ₹60 | Try B→A ₹61 or A→B ₹10 | invalid; no settlement created |
| AC-BAL-05 | Existing settlement B→A ₹20 | Edit to ₹25 | same settlement ID; group nets change by ₹5, no duplicate record |
| AC-BAL-06 | Existing settlement B→A ₹20 | Delete | nets revert by ₹20, expense spending unchanged |
| AC-BAL-07 | Multiple expense/settlement lines | Sort input rows differently | exact same final nets and suggested pairs (stable ties by ID) |
| AC-BAL-08 | A historical settlement remains after expense edit | Reopen Balances | signed positions still sum zero; Review balances message when previous pair no longer justified; no mutation/deletion of settlement |

## Accessibility, privacy and durability
| ID | Given | When | Then |
|---|---|---|---|
| AC-UX-01 | 200% font scale | Use add, shared split and settlement forms | labels, amounts and Save not clipped/occluded |
| AC-UX-02 | TalkBack enabled | Run personal and shared flows | fields, errors, amount direction and list context announced |
| AC-UX-03 | Dynamic theme/light/dark | Switch appearance | semantic meaning does not depend on color and foreground remains legible |
| AC-UX-04 | Keyboard open on amount/split input | Scroll and Save | IME does not cover focused field or Save; Done never unexpectedly submits |
| AC-UX-05 | Settings | Delete all data and confirm | all user records and settings removed, onboarding appears on fresh launch; no Undo claim |
| AC-UX-06 | App offlined and group member added | Inspect copy | never suggests remote invitations or money transfers occurred |

## Numeric test fixtures (unit tests, exact paise)
1. Equal `10000 / 3`: shares `3334,3333,3333`, deterministic by sorted stable member IDs.
2. Equal `1 / 3`: `1,0,0`; no negative or dropped paise.
3. Custom `12345`: shares `8000,4345`, exact total.
4. One-payer Me and Me omitted as beneficiary: `payer +10000`, beneficiary `-10000`.
5. Three-member settlement sequence: initial `[+10000,-6000,-4000]`, B pays A `2500` => `[+7500,-3500,-4000]`; C pays A `4000` => `[+3500,-3500,0]`; B pays A `3500` => `[0,0,0]`.
6. Boundary `₹10,00,00,000.00` allowed; higher rejected; sums computed with checked arithmetic.
7. Editing the same shared expense repeatedly does not change group net sum from zero.
8. Independent personal and shared records of matching title/date remain separate, never double counted.

## Release verification checklist
- Unit tests passing for parsing, rounding, nets, settlement allocation, input validation, calendar periods, overflow prevention.
- Room tests passing for atomic create/update/delete, foreign keys, migration from empty database, process reopen, duplicate Save handling.
- Compose UI tests passing for P0 navigation, form errors, personal CRUD, group creation, split editor, settlement and filtering.
- Manual smoke tests on at least one compact phone and one larger-window emulator, with light/dark, TalkBack and large fonts.
- Product targets from `PRODUCT.md` measured and exceptions documented; no unresolved accounting correctness defect.

Related docs: [Product](PRODUCT.md) · [Screens](SCREEN_SPECS.md) · [Design](DESIGN_SYSTEM.md) · [Tasks](TASKS.md)
