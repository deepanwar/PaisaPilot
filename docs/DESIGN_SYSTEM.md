# Material 3 Expressive Design System — v1.0

**Brand working name:** Ledger | **Platform:** Android Compose | **Status:** proposed app tokens, not a claim that each corresponds to a built-in library token. **Goal:** distinctive but deliberately quiet financial UI.

## 1. Experience principles
1. **Amount first.** Money is the most prominent content; context is the second layer; decoration never obscures numerals.
2. **One bright action.** Add/Save receives primary emphasis. No competing colorful CTA cluster.
3. **Meaning is verbal.** “You owe ₹…” / “You're owed ₹…” always accompanies semantic color; positive vs negative signs alone are insufficient.
4. **Expressive where useful.** Larger shapes, strong type contrast, responsive motion and occasional filled icon containers; avoid wobbling currency digits, morphing chart values, and ornamental cards everywhere.
5. **System-aware.** Edge-to-edge, contrast, large fonts, reduced motion, screen readers, system theme and non-compact windows.

## 2. Color scheme
Semantic aliases map to Material color roles. Hex values below are **proposed static fallback colors**, to be audited for WCAG contrast with final font weights and actual rendered surfaces. Support Android dynamic color where available as a user setting, but preserve semantic status clarity when dynamic palettes shift. Dark and light theme must be QA verified.

| Material/app role | Light | Dark | Primary usage |
|---|---|---|---|
| primary | `#285C50` | `#9FD9C8` | save, selected control |
| onPrimary | `#FFFFFF` | `#06362C` | foreground on primary |
| primaryContainer | `#BCEBDB` | `#0C4D3F` | hero highlight |
| onPrimaryContainer | `#07372C` | `#CBF3E7` | container text |
| secondary | `#586174` | `#BBC6DF` | secondary affordances |
| onSecondary | `#FFFFFF` | `#233044` | secondary button text |
| secondaryContainer | `#DCE5FA` | `#3B465B` | selected filters |
| onSecondaryContainer | `#29364A` | `#DBE5FA` | chip text |
| tertiary | `#775A35` | `#E8C58C` | category emphasis |
| onTertiary | `#FFFFFF` | `#432C0C` | tertiary controls |
| background | `#FAFBF8` | `#101512` | app canvas |
| onBackground | `#171D1A` | `#E3EAE5` | main text |
| surface | `#FAFBF8` | `#101512` | scaffold |
| onSurface | `#171D1A` | `#E3EAE5` | surface text |
| surfaceContainerLow | `#F3F6F1` | `#191F1B` | quiet sections |
| surfaceContainer | `#EAF0E9` | `#202822` | regular cards |
| surfaceContainerHigh | `#E2EAE1` | `#2A332D` | emphasized bottom sheet |
| onSurfaceVariant | `#404942` | `#C0CAC1` | metadata |
| outline | `#707A72` | `#89948B` | focus/borders |
| outlineVariant | `#C0CBC1` | `#445046` | separators |
| error | `#B3261E` | `#FFB4AB` | invalid form, destructive |
| onError | `#FFFFFF` | `#690005` | error text on filled error |
| errorContainer | `#F9DEDC` | `#93000A` | inline errors |
| onErrorContainer | `#410E0B` | `#FFDAD6` | error container text |
| owedStatus (foreground) | `#275B50` | `#A8DCCB` | **you are owed** + full text |
| oweStatus (foreground) | `#9E453D` | `#FFB4AB` | **you owe** + full text |
| neutralStatus | `#48554C` | `#C0CAC1` | settled |

Do not hardcode colors in components; reference semantic scheme/tokens. Test each text-on-surface pairing at ≥4.5:1 for body and ≥3:1 for large text/meaningful UI boundaries. Especially recheck user-selected dynamic colors.

## 3. Typography
Use native Android default sans/Roboto family and Material 3 type scale. All values in **sp**; adapt with user font scale. Amounts use tabular figures **where font feature support is verified**, otherwise standard digits with adequate layout width; do not rely on undocumented typeface axes. No custom font download necessary.

| Role | Size / weight / line height | Use |
|---|---|---|
| displaySmall | 36 / 600 / 44 | personal month total, big balance |
| headlineMedium | 28 / 600 / 36 | primary screen headings |
| headlineSmall | 24 / 600 / 32 | detail amount/subhead |
| titleLarge | 22 / 600 / 28 | group title and section top |
| titleMedium | 16 / 600 / 24 | row main label |
| bodyLarge | 16 / 400 / 24 | form input/long descriptions |
| bodyMedium | 14 / 400 / 20 | secondary text |
| labelLarge | 14 / 600 / 20 | buttons and filter labels |
| labelMedium | 12 / 600 / 16 | chips and metadata labels |

Never force uppercase financial actions. Keep line heights scalable. Amounts may wrap at larger font; allow more lines before shrinking them; do not auto-scale down to illegible type.

## 4. Shape and spacing
**Base spacing unit:** 4dp. Values: `space1=4`, `space2=8`, `space3=12`, `space4=16`, `space5=20`, `space6=24`, `space8=32`, `space10=40`, `space12=48` dp. Screen horizontal padding compact 16–20dp, medium 24dp, expanded content bounded to ~840dp for forms and ~1200dp for dashboards. Vertical section gap 24dp. Content within card 16–20dp.

**Corner radius tokens:** `extraSmall=4dp`, `small=12dp`, `medium=16dp`, `large=24dp`, `extraLarge=28dp`, `full=50%`. Large hero card 28dp; transaction card 16dp; form fields follow actual Material text-field styles; buttons full/pill when clearly actionable. Use geometric variation sparingly: one rounded hero and chips; not every row a unique shape.

**Size tokens:** standard button min height 48dp; prominent Save/Add 56dp; icon-only hit area 48dp; list item minimum 64dp (grow with content); large amounts field min 72dp (expand on font scaling); selection chips min 40dp visual and 48dp touch region; bottom navigation component uses Material guidance rather than custom hardcoded height. Dividers 1dp with outlineVariant; do not use shadows as sole affordance.

## 5. Components and interaction contract
| Component | Placement and treatment | Behaviors |
|---|---|---|
| Large summary card | Home/Insights/Group detail | semantic amount label; not tappable unless navigation signified |
| Transaction row | Activity, recent lists | title, subtitle date/category, right aligned ₹ amount, type/group context; full row 48dp+ target |
| Primary action | Sticky bottom form Save | disabled only for invalid/in-flight; show short loading indicator during actual database work |
| Add FAB | Today/Activity | single obvious action; labelled `Add expense`; avoid covering last row with inset |
| Filter chips | Activity, split member selections | selected + unselected visibly distinct, pressed/focus states |
| Segmented selector | Split Equally/Exact | changes preview but never silently discards entered custom shares; warn/restore where necessary |
| Date picker | Expense/settlement form | system-friendly Material dialog; localized readable date; explicit Confirm/Cancel |
| Modal bottom sheet | category/member picker if short | title, optional search for long list, dismiss behavior; avoid for numeric forms with IME conflicts |
| Confirmation dialog | deletes/archives | specific action and impact; default focus non-destructive, clear Cancel/Delete labels |
| Snackbar | brief Save/Undo | never only evidence of success; Undo limited to safely restorable deletes |
| Navigation bar/rail | primary shell | four destinations; labels visible; current destination obvious and screen-reader-announced |
| Empty state | all lists/months | clear explanation, one prominent recovery action |
| Split summary | SHR-01 | always readable Assigned / Remaining and mismatch text; Save gated on exact sum |

### Keyboard, focus, and form specifics
- Personal Add opens amount keyboard immediately; edit does not. Group Add may also focus amount immediately, but maintain scroll so payer and split summary are easy to reach.
- Numeric amount keyboard uses decimal action; `IME Done` dismisses; submit by sticky Save, not keyboard `Done`, to prevent accidental financial record submission.
- Remove grouping separators before parse, reject second decimal point and excess precision; display localized formatted value outside edit focus; preserve caret while editing.
- On field error, send focus to field and announce inline message; on keyboard appearance auto-scroll focused field into visible space using standard Compose insets/focus facilities.
- Keep Save visible above IME if possible without overlapping controls; long split forms scroll under a persistent action area with sufficient bottom padding.

## 6. Expressive motion
Motion communicates state transition or spatial continuity, never the monetary meaning. Suggested product motion durations (not library API promises): tap response 80–120ms; chip selection 120–180ms; screen transitions 220–300ms; sheet presentation 220–300ms. Use subtle standard easing; large money values should update instantly or via non-deceptive crossfade, **never count up**. Hero/card shape changes only when conveying active selection. Respect system reduced-motion preference by removing nonessential animations. No spring bounce on Save, delete, or settlement because it can trivialize financial actions.

## 7. Responsive behavior
| Available width | Navigation | Content |
|---|---|---|
| Compact | bottom nav; single pane | one-column, edge-to-edge, sticky form action |
| Medium | nav bar or rail following adaptive guidance | centered forms max ~640dp; richer category layout |
| Expanded | rail | list/detail two-pane when Activity or Groups is selected, forms capped ~640dp |

Use available window size **rather than device name**. Handle landscape, split-screen, font scale and keyboard. Avoid hardcoded window-size cutoff unless verified against Android window size class guidance. Tablet optimization is progressive; correctness and usability on phones are release gates.

## 8. Accessibility and content design
- All fields have persistent labels, not placeholder-only meaning. Show ₹ and “Amount in rupees” accessibly; read rupees and paise naturally.
- Color not sole signal: all balances carry `You owe`, `You're owed`, or `Settled`; all errors include text; all switches announce state.
- Touch targets ≥48dp; accessible action names for edit, delete, member toggle, filter and add. Logical focus order from header to hero to content to primary action.
- Test TalkBack, keyboard navigation where possible, font scaling 200%, light/dark/high contrast and long 40/80-character labels. Maintain readable dates and INR grouping.
- Plain copy: “Paid by”, “Split among”, “Remaining to assign”, “Recorded on this device”, “This does not send money”. Avoid “wallet” and “Pay now” in local-only release.

## 9. Implementation caution / references
Compose Material 3 Expressive evolves by library release. Use **stable** supported components first; opt-in experimental components only after verifying actual availability and maintenance implications in the chosen version. As of 2026-10-20, Android release notes list stable Compose Material 3 `1.4.0` (2026-10-07); verify versions and exact component APIs when implementation begins. Components/tokens above are **design specifications**, not invented Compose API names.

Official references:
- https://developer.android.com/develop/ui/compose/designsystems/material3
- https://developer.android.com/jetpack/androidx/releases/compose-material3
- https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation

Related docs: [Product](PRODUCT.md) · [Screens](SCREEN_SPECS.md) · [Acceptance](ACCEPTANCE_CRITERIA.md) · [Tasks](TASKS.md)
