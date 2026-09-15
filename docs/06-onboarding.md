# Onboarding — UX and UI brief

**For:** whoever designs the first run.
**Status:** not built. There is no onboarding flow in the app today — only a
comment in `HarborRepository` that says contacts stay empty "until onboarding
picks someone". This document is the brief for designing it, written from what
the app actually requires rather than from a wish list.

Everything below that says "already built" is on `main` and can be reused or
restyled. Everything else is yours.

---

## Why this screen sequence decides whether the study works

Harbor's whole product is one moment: you stop walking, and your phone offers
you your mum. That moment needs three things to be true, and **all three are
set up during onboarding and nowhere else**:

1. Someone to call, with a number that can be dialled.
2. Activity-recognition permission granted.
3. Notification permission granted.

Miss any one and the app still opens, still looks finished, and never fires a
single cue. The participant has a pleasant week with a garden that stays
empty, and their data says the trigger does not work.

So the number that matters is **the share of participants who finish
onboarding with cues actually on**. Not installs, not time-to-complete.
Everything else in this brief is subordinate to that.

The study's first question is "does the walking-stop trigger land at moments
people call good?" (`docs/03-week-one-study.md`). A participant who declined a
permission contributes nothing to it.

---

## Non-negotiables

These are not style preferences. Each one is a decision recorded in
`docs/01-decisions.md`, and several are the reason the app is allowed to ask
for what it asks for.

| Rule | Why |
| --- | --- |
| **Explain before the system dialog.** The OS permission sheet must never be the first time someone hears about activity recognition | It is the biggest funnel risk in the product. A cold dialog gets declined, and Android may never ask again |
| **Never promise the parent will see anything.** No "let them know you're thinking of them", no shared status, no invitation | The parent installs nothing. There is no other side (ADR-007). Promising one is a lie the app can never make true |
| **Never call it a call.** The cue is call-shaped — their photo, their ringtone, full screen — but the words never claim an incoming call | ADR-009. The pull is the mechanism; misrepresenting it is not |
| **Do not ask for contacts, phone, or location permission** | Harbor dials with `ACTION_DIAL` and asks whether the call happened (ADR-002); it never reads the call log. Photos come from the system picker. Location is ruled out entirely (ADR-010) |
| **Never ask for college credentials** to import a timetable | Explicitly forbidden (ADR-011). Busy times are self-entered |
| **Dismissing must be free.** If onboarding implies a streak, a score, or an obligation, it has mis-sold the app | There is no streak anywhere in the product, by design |

---

## What onboarding has to collect

Ordered by how badly the study breaks without it.

| # | Thing | Blocks a cue? | Where it lives | Already built |
| --- | --- | --- | --- | --- |
| 1 | A person: name, phone (E.164), colour, ringtone, photo | **Yes** — no number, no dial | `Contact` | `ContactScreen` |
| 2 | Activity-recognition permission | **Yes** — no trigger at all | OS | explainer in `CuesSetupScreen` |
| 3 | Notification permission (API 33+) | **Yes** — the cue posts and is silently dropped | OS | asked alongside #2 |
| 4 | Cues turned on | **Yes** — `cuesEnabled` is false until someone says yes | `UserSettings.cuesEnabled` | toggle in `CuesSetupScreen` |
| 5 | Their name | No | `UserSettings.name` | `SettingsScreen` |
| 6 | Pace: walk length, daily cap, cooldown | No — defaults are sane | `Thresholds` | `SettingsScreen` |
| 7 | Busy and free times | No — but cues will land in class without it | `WeekBlock` | the thorns-and-flowers grid in `ScheduleScreen` |

Shipped defaults for #6, so the design knows what it is showing:
**walk 10 min · at most 2 cues a day · 120 min between them.**

Items 6 and 7 were specifically asked for in onboarding as well as in Account
(Dhimant's review). They are optional steps — a participant who skips both
still gets a working app.

---

## The shape of the flow

Seven steps, of which **four are required**. The ordering is the argument, so
here is the reasoning rather than just the list.

The temptation is to ask for permission first, on the grounds that it is the
most important thing. That is backwards. Nobody grants a movement permission
to an app they have not understood yet. The permission ask should come
**after** the person has named their mum, chosen her colour and heard her
ringtone — at which point the app is concretely about *her*, and the ask reads
as "so I can catch a good moment to call her" rather than "so I can watch you
walk".

```
1  Welcome          what this is, in one sentence          required (read-only)
2  Your name        the greeting                           skippable
3  Your person      name, number, colour, sound, photo     REQUIRED
4  A cue, shown     a real cue surface, as a preview       required (read-only)
5  Permission       explainer, then the system dialogs     REQUIRED
6  Your pace        walk length, cap, cooldown             skippable
7  When you're busy the week grid                          skippable
   → Home, garden empty, one line about the first flower
```

### 1 · Welcome

One sentence about what the app does, and one about what it will not do. Not a
feature tour — there are four screens of product here, and a carousel of
promises before anyone has seen anything is where goodwill goes to die.

Worth saying here, because it pre-empts the permission anxiety: *nothing is
shared with anyone, and your family never sees any of this.*

### 2 · Your name

One field. Feeds "Hey, Devansh." on home. Skippable; the greeting falls back
to "Hey there."

### 3 · Your person — the one screen that must not be skippable

Everything else in the app is downstream of this. Collects:

- **What you call them** — "Mom", "Dad", "Ammi". Free text, not a contact import.
- **Their number** — E.164. This is the field most likely to go wrong; see
  failure states below.
- **Their colour** — one of four tones. Becomes their plot in the garden.
- **Their sound** — the ringtone that plays on the cue. *This is the product's
  central mechanism*, not a preference: a sound that means *her* is what makes
  the cue land as a person rather than as an app. Design it as a moment, not a
  settings row. Let them hear it.
- **Their photo** — optional, via the system picker.

**Design question for the team:** the photo and the sound are what make the
cue work, and both are optional. Is there a way to make them feel worth doing
without making them feel required?

### 4 · Show them a cue

The strongest thing onboarding can do is show a real cue before asking for the
permission that produces one. `CuesSetupScreen` already has "Show me a cue
now", which fires the genuine cue surface with the real photo and ringtone.

Whether this sits before or after the permission ask is a real design
decision. Before is more persuasive and riskier — it spends the ringtone
surprise early. After is safer and less persuasive.

The cue must be clearly a preview here. It has three real paths on it, and
"Call now" in a preview would either dial for real or lie.

### 5 · Permission — the screen the study lives or dies on

Two asks in one breath: activity recognition, and notifications on API 33+.
Activity recognition is the one that decides whether sensing runs at all;
refusing notifications does not stop sensing but does make the cue invisible.

The existing explainer in `CuesSetupScreen` answers three questions, and the
structure is worth keeping even if every word changes:

- **What Harbor reads** — whether the phone thinks you are walking or still.
  Not where you are, not what you are doing, not which apps you use.
- **Where it stays** — on this phone. Never sent to us, never shared with
  family, not even as a summary.
- **What you keep control of** — every cue can be dismissed and dismissing
  costs nothing; at most 2 a day with at least 120 minutes between them; you
  can turn it off whenever.

Three hard requirements:

1. **The system dialog only appears after a deliberate tap.** Never on screen
   entry.
2. **Declining must be a real, respected answer** — not a dead end, not a
   wall, not a re-ask. The app works without cues; the user can start a moment
   themselves. Say so and move on.
3. **Handle the second refusal.** Android stops asking after two declines. At
   that point the only route is system settings, and the app has to notice the
   permission was granted when they come back. There is currently a "check
   again" button for this, deliberately dull because it cannot break. Design
   something better if you can, but it must not silently claim cues are on
   when they are not.

**This screen deserves the most attention of anything in the flow.** If you
redesign one thing, redesign this.

### 6 · Your pace — skippable

Walk length, daily cap, cooldown. Frame it as *how often should I speak up*,
not as configuration. Defaults are good; most people should be able to tap
past.

### 7 · When you're busy — skippable

The week grid: press and drag to lay down blocks. It exists and works
(`ScheduleScreen`). It is genuinely useful in onboarding because a student's
timetable is the single biggest source of bad cue timing.

Do not let this become the reason someone abandons onboarding. Make skipping
obvious.

### Landing

Home, with an empty garden and one line about the first call planting the
first flower. Do not fake a flower to make it look populated — the garden
being empty *is* the invitation.

---

## States and failures to design

Easy to forget and each one is real.

| State | What should happen |
| --- | --- |
| Phone number typed wrong or without a country code | `ACTION_DIAL` will happily open the dialer with a number that cannot connect. Validate and show the E.164 form back |
| Activity permission declined once | Accept it. Explain that cues stay off and nothing else changes. Offer to continue |
| Declined twice — Android stops asking | Route to system settings, and detect the grant on return |
| Notifications declined, activity granted | Sensing works, cue is invisible. This is the worst silent failure in the app. Say something |
| Play services unavailable | Sensing cannot start. The app must say so rather than pretending. Copy exists |
| Onboarding abandoned halfway | Where do they land, and what do they see next launch? Currently undefined. **Needs a decision** |
| Returning after a reinstall | The garden is gone with the data. Not currently addressed |

---

## What already exists to build on

Reuse the vocabulary in `ui/theme/Harbor.kt` rather than styling by hand —
`PageIntro`, `Surface`, `SoftSurface`, `SectionHeading`, `SmallCopy`,
`Notice`, `Pill`, `PrimaryAction`, `QuietAction`, `Avatar`. Full map in
`docs/05-changing-the-ui.md`.

Screens that already do part of this job and could be lifted into a flow:
`ContactScreen` (the person), `CuesSetupScreen` (the explainer and the
switch), `SettingsScreen` (name and pace), `ScheduleScreen` (busy times).

None of them are currently sequenced, none have a progress indicator, and none
know they are part of a first run.

Two things the UI team should know before designing pixels:

- **The typefaces in the build are not Lora and DM Sans.** `Type.kt` uses the
  device's system serif and sans; no font files ship. Sizes and weights are
  right, faces are not. See `docs/05-changing-the-ui.md`.
- **Flowers are placeholder circles** until the real artwork lands.

---

## Open questions for the team

1. Cue preview before or after the permission ask?
2. Progress indicator, or let it feel like a conversation?
3. Can someone add a second person during onboarding, or is that deferred to
   later? The garden supports many; onboarding only needs one.
4. What does a resumed, half-finished onboarding look like?
5. For the study only: does consent live in onboarding, or entirely on paper
   before install? Right now the protocol says in writing, before install.

---

## Definition of done

Onboarding is finished when a participant who has never seen the app can, in
one sitting and without help:

- name someone and enter a number that dials;
- hear that person's sound;
- understand what is sensed and where it stays, in their own words;
- grant both permissions, or decline knowingly and still have a usable app;
- reach home knowing that the first call plants the first flower.

If they reach home with `cuesEnabled` false and no idea that happened,
onboarding has failed, however good it looked.
