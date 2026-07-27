# MedQueue UI research — what would actually help the shopkeeper

Walkthrough of the shipped UI against the job the app exists to do: save a
request at the counter, tell the customer when stock lands, close it out.
Every finding below points at real code, and is ordered by how much it costs
the shopkeeper on a normal working day.

## The UI as it stands

| Screen | File | Role |
| --- | --- | --- |
| Splash | `ui/screens/SplashScreen.kt` | Wordmark, then routes to Onboarding or Queue |
| Onboarding | `ui/screens/OnboardingScreen.kt` | Two steps: shop identity, then reminder prefs |
| Queue | `ui/screens/RequestListScreen.kt` | Landing screen: search, 3 filter pills, rows, FAB |
| New / Edit request | `ui/screens/RequestFormScreen.kt` | Phone, name, medicines, prescription photo, emergency |
| Request detail | `ui/screens/RequestDetailScreen.kt` | Hero, medicines, prescription, Call/WhatsApp/SMS, Mark delivered |
| Settings | `ui/screens/SettingsScreen.kt` | Shop details, contacts, message preview, auto-send, reminders |

Navigation is a `when` over a `Screen` sealed type held in `MedQueueApp.kt`,
with a three-tab bottom bar (Queue / New / Settings). The design system lives
in `ui/components/CommonComponents.kt` and `ui/theme/` — teal on paper,
Manrope, 18dp cards, teal eyebrows, tinted pills. It is coherent and reads
well; nearly every problem below is about flow and state, not styling.

---

## 1. "Mark delivered" is doing two different jobs

`MedQueueApp.kt:86-90` marks the request DELIVERED and then calls
`notifyOnDelivered`, which fires the WhatsApp template. That template
(`contact/ContactLauncher.kt:43-57`) reads *"Your medicines are ready… please
collect at your earliest convenience."*

So the single status flip means both "stock arrived, I told the customer" and
"customer came and took it." Those are hours or days apart in a real shop, and
between them sits the state the shopkeeper most needs to see: **told, still
waiting for pickup.** Right now that state has no representation, so the queue
cannot answer "who have I already called?" — the shopkeeper either messages
someone twice or forgets them.

**Change:** split the status into three — Pending → Notified → Collected.
Row shows "Notified 2h ago"; the primary action on a pending row is *Notify*,
and *Mark collected* only appears once notified. `RequestEntity` needs a
`notifiedAt: Long?` to back it; the queue row and detail hero both surface it.

This is the highest-value change on the list. Everything else is smaller.

## 2. Closing out a request costs four taps and two screen changes

The queue row (`RequestListScreen.kt:147`) does one thing: navigate to detail.
To notify a customer the shopkeeper taps the row, waits for detail, scrolls if
the prescription card pushed the footer down, taps WhatsApp, comes back, taps
the row again, taps Mark delivered — and gets bounced to the queue
(`MedQueueApp.kt:89`).

At a counter with a customer waiting, that is the whole interaction budget.

**Change:** put the two actions on the row itself. A trailing WhatsApp icon
button, plus swipe-to-deliver with an undo snackbar. Detail stays for
prescription photos and edits, which is what it is actually good at.

## 3. Emergency requests sink; delivered ones float

`RequestDao.kt:20` sorts strictly `createdAt DESC`, and the default filter is
`FilterTag.ALL` (`RequestListScreen.kt:50`). Two consequences:

- An emergency saved at 9am sits below every routine request saved since.
  Its only marks are a 3dp red stripe (`RequestListScreen.kt:252`) and an
  "URGENT" pill — both invisible once the row is below the fold.
- Yesterday's finished work occupies the top of the list, pushing live
  pending requests down.

**Change:** default the filter to Pending, and sort emergency-first within
pending. Add an "Urgent" filter pill alongside the existing three.

## 4. The list leads with the phone number

`RequestListScreen.kt:280` sets the phone number as the primary line and
collapses `name · medicines` into one ellipsised secondary line
(`SecondaryLine`, `maxLines = 1`). `PhoneTile` shows the last two digits.

The comment at `CommonComponents.kt:292` explains why — the name is optional,
the number is not. That reasoning holds for the *tile*, but not for the whole
row. A customer walks up and says a name, or asks about a medicine. Nobody
scans a queue by phone number. And with the name occupying the front of the
secondary line, the medicine — the thing the shopkeeper is looking for when
stock arrives — is the first casualty of truncation.

**Change:** medicine as the primary line, name second, phone demoted to
small muted text or left to the tile alone. Fall back to the phone number as
primary only when both name and medicine are blank.

## 5. Multi-item requests look identical to single-item ones

`medicineName` is one newline-delimited string. The row joins it with `", "`
(`RequestListScreen.kt:240`) into a line that truncates, so a five-item order
and a one-item order render the same width of grey text. Detail numbers them
(`RequestDetailScreen.kt:174-180`), which is good, but by then the shopkeeper
has already committed to opening it.

Worse, fulfilment is all-or-nothing. When three of five items arrive there is
no way to record that — the request stays wholly pending or goes wholly
delivered.

**Change:** an item-count chip on the row ("3 items"). In detail, make each
medicine line a checkbox so partial arrivals are recordable, and derive the
request status from the items.

## 6. Dark mode renders near-black text on near-black cards

`MyApplicationTheme` (`ui/theme/Theme.kt:77`) defaults `darkTheme` to
`isSystemInDarkTheme()` and a full `DarkColorScheme` is defined. But the
screens do not consume it. They mix `MaterialTheme.colorScheme.surface`
(which flips to `#1A201E` in dark) with hardcoded light tokens for content:

- `DsCard` — `CommonComponents.kt:134` surface from the scheme, and every
  caller passes `color = Ink` (`#101828`) for text on top of it.
- `ScreenHeader` — `CommonComponents.kt:52,57`, same pairing.
- `RequestRow` — `RequestListScreen.kt:246,281`, same pairing.
- `Scaffold(containerColor = Paper)` on every screen pins the page background
  to the light `#F4F6F7` regardless of scheme.

Any shopkeeper with system dark mode on — common, it is a battery habit — gets
an unreadable app. This is the one item here that is a straight defect rather
than a design tradeoff.

**Change:** either route all content colours through `MaterialTheme.colorScheme`,
or, given the mockup only ever specified a light UI, force `darkTheme = false`
and ship light-only until a dark palette is actually designed. The second is a
one-line fix and honest about where the design is.

## 7. An accidental "Mark delivered" cannot be undone

There is no confirm, no snackbar, and no path back: `RequestViewModel` exposes
`markDelivered` only — no `markPending` anywhere in the codebase — and
`EditRequestScreen` edits fields, not status. Combined with finding 1, a
mis-tap also messages the customer. Delete, by contrast, does get a
confirmation dialog (`RequestDetailScreen.kt:328`).

**Change:** an undo snackbar on delivery (needs `markPending` on the
ViewModel), which is better than a confirm dialog — it does not slow down the
99% of taps that are correct.

## 8. Errors are toasts, so they can never offer a way out

`RequestDetailScreen.kt:307-313` reports "WhatsApp not installed", "Invalid
phone number", "Could not send — try again" as `Toast`. There is no
`SnackbarHost` anywhere in the app. On Android 12+ background toasts are
truncated and rate-limited, and they cannot carry an action — so "Could not
send — try again" gives the shopkeeper nothing to tap, and "WhatsApp not
installed" cannot offer SMS instead.

**Change:** add a `SnackbarHost` to the `Scaffold`s and move failures onto it
with actions: Retry on send failure, "Send SMS instead" on WhatsApp missing.

## 9. Small touch targets, including the destructive one

Measured against the 48dp minimum:

- `HeaderAction` (`CommonComponents.kt:109-119`) — 12sp text with 6dp padding,
  roughly 28dp tall. This is what **Edit** and **Delete** are, sitting 14dp
  apart in the detail header (`RequestDetailScreen.kt:100-101`).
- The prescription clear button (`RequestFormScreen.kt:278-292`) — about 26dp.
- `FilterPill` (`RequestListScreen.kt:212-225`) — about 33dp.

A destructive action rendered as a small, low-contrast text target next to a
routine one, on a phone handled with wet or powdery hands, is worth fixing
independently of its size.

**Change:** 48dp minimum touch targets throughout. Move Delete out of the
header into an overflow menu, or down the page away from Edit.

## 10. The header eats the top third of the queue

Before the first row the queue stacks a status-bar-padded header (~80dp), an
always-on search field (~56dp), and a filter row (~47dp) —
`RequestListScreen.kt:98-122`. On a budget 5" phone that leaves room for about
three rows.

**Change:** collapse search behind an icon in the header, or let the header
scroll away with the list. The filter pills earn their place; the search field
does not until the queue is long.

## 11. Two affordances for "new request", none for "call"

The FAB (`RequestListScreen.kt:82`) and the bottom bar's New tab
(`MedQueueBottomBar.kt:57`) do the same thing, and the list reserves 96dp of
bottom padding to keep them from colliding. Meanwhile `PhoneTile` — the most
prominent element on each row — is inert.

**Change:** drop one of the two add affordances (keep the FAB; it is where
the thumb is), and reclaim the space.

## 12. The UI is English-only, though the messages are not

The customer-facing template is bilingual English/Kannada
(`ContactLauncher.kt:49-51`), and the placeholder shop name is "Ainapur
Medical Store" — so the intended user is a Kannada speaker. The interface they
operate is entirely English: one `res/values/strings.xml`, no `values-kn` or
`values-hi`.

The strings are already fully externalised, so this is translation work
rather than refactoring.

**Change:** add `values-kn` (and `values-hi`).

*Correction to the above:* the font is not a blocker. Manrope carries no
Kannada glyphs, but Android falls back to the platform's Noto Sans Kannada
per glyph, so no font needs bundling and no per-locale theme switch is
required — only the metrics differ slightly from the Latin UI.

## 13. Fixed heights will clip at large font scales

`PrimaryButton` is pinned to `.height(50.dp)` (`CommonComponents.kt:332`) with
15sp label text, and `Pill` sets `softWrap = false, maxLines = 1`
(`CommonComponents.kt:281-283`). Older shopkeepers commonly run system font
scale at 1.3–2.0, where both clip.

**Change:** swap fixed heights for `defaultMinSize` + padding, and test the
queue and detail screens at 2.0 font scale.

---

## Suggested order of work

**Do first — cheap, high impact**
1. Default the queue filter to Pending; sort emergencies to the top (3)
2. Fix or disable dark mode (6)
3. Undo snackbar on delivered, plus `markPending` (7)
4. 48dp touch targets; move Delete out of the header (9)

**Then — the real workflow fix**
5. Split Pending / Notified / Collected, with `notifiedAt` (1)
6. Inline notify + swipe-to-deliver on the queue row (2)
7. Rework the row hierarchy: medicine first, item count chip (4, 5)

**Then — polish**
8. Snackbars with actions in place of toasts (8)
9. Collapse the search field; drop the duplicate add affordance (10, 11)
10. Kannada/Hindi locales with a font fallback (12)
11. Large-font-scale pass (13)
12. Per-item checkboxes for partial fulfilment (5)

## Implementation status

Findings 1–12 and part of 13 are implemented on this branch. Two places where
the implementation deliberately departs from the recommendation above:

- **Finding 11 — which add affordance to keep.** The recommendation was to keep
  the FAB. The FAB was dropped instead and the bottom bar's New tab kept: the
  tab is visible on every screen rather than just the queue, and removing the
  FAB gives the list back the ~72dp of bottom padding reserved to stop the two
  colliding — which serves finding 10 at the same time. The empty state gained
  its own button so first-run still has an obvious way in.
- **Finding 1 — how to model the middle state.** Rather than adding a third
  `RequestStatus` value, the stage is *derived* from `status` plus a new
  `notifiedAt` column. A stored third value would let a row hold an impossible
  status/timestamp combination, and would need every existing row converted.

The Kannada and Hindi files (finding 12) were written without a native
speaker and **need review before release** — each file carries a header
listing the recurring term choices to check first. `MedQueue`, `WhatsApp`,
`SMS` and `+91` are deliberately absent from both so they fall back to
English.

Still open: the remainder of 13. The `Pill` composable sets
`softWrap = false`, so it clips rather than wraps at large font scales.
Letting it wrap reintroduces the row-stretching bug its comment warns about,
so this needs a different fix than the min-height change applied to the
buttons.

## What is already right

Worth not breaking: the phone tile as identity when names are optional; the
prescription slot that renders nothing rather than an empty box
(`RequestFormScreen.kt:248-254`); the live message preview in Settings; the
always-visible Call/WhatsApp/SMS row that replaced a bottom sheet; the SMS
part count in the confirmation toast (`RequestDetailScreen.kt:322`), which is
honest about what a tap costs on a prepaid plan.
