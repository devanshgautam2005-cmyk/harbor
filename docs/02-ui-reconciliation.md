# Reconciling the backend with the UI prototype

**Source:** https://github.com/Bored-Kxiden/harvest-pulse — a v0-generated
Next.js prototype. The product logic lives in `lib/harbor/model.ts`; the
screens are `components/harbor/*.tsx`.

**Settled 2026-09-10:** v0.1 targets the whole prototype, not Slack Tide
alone. The prototype is the reference for product behaviour, resolved
divergence by divergence below. The spelling is **Harbor**. The Android app
and backend stay in this repo; `harvest-pulse` remains the v0-owned web
prototype.

Read this before extending the schema or the Kotlin model. The schema in
`backend/supabase/migrations/0001_init.sql` was derived from the Slack Tide
handoff alone, and the prototype turns out to describe a **substantially
larger product**. Slack Tide is one of eight screens.

## The prototype's shape

| Screen | What it holds |
| --- | --- |
| Home / Inbox | Per-person notes and unread state |
| Harvest | Mutual schedule sharing and the privacy/consent surface |
| Garden | Jar of Happiness — `jarDays`, seasons, milestones |
| Conversation | Per-person chat (`ChatMessage[]`) |
| Dispatch | The content pipeline — headline/photo/audio, draft→delivered |
| Slack Tide | The cue simulator |
| Settings | Thresholds, sound, reduced motion, "what counts as enough" |

The handoff explicitly put Dispatch, the Jar, and parent-side surfaces
**out of scope**. The prototype builds all of them. That gap is the main thing
to resolve — see "Decisions needed" below.

## Divergences from what is built

Confirmed by reading `lib/harbor/model.ts`. Each of these is a real
disagreement, not a naming difference.

| # | Prototype | Built here | Impact |
| --- | --- | --- | --- |
| 1 | `Resolution` has five variants — adds `message` | Four variants | DB enum + Kotlin enum |
| 2 | Four people (mom, dad, aanya, "our little tribe"); every `Moment` carries `person` | A single `Contact`; entries have no person | Schema change: contacts become a set, `ledger_entries` needs a person FK |
| 3 | `source: manual \| walking_stop \| dispatch` | `manual \| walking_stop \| session_end` | Enums disagree in both directions |
| 4 | `cues[]` tracked separately from `moments[]`; the daily cap counts **cues**, and moments carry `cueId` | Cap counts ledger entries | A cue that produces no moment is invisible to the cap here |
| 5 | `settings.cuesEnabled` — a global off switch, default **off**, gated behind a privacy dialog | No such flag | Answers the handoff's open "degraded mode" question |
| 6 | `settings.minimum: any \| call \| null` — user defines what fills the Jar | Not modelled | Needed if the Jar ships |
| 7 | `settings.sound` (chime/soft/silent), `reducedMotion` | `cueSoundRef` on the contact | Different shape |
| 8 | `sharing`, `momConsent`, `schedules[day] = {you, mom}`, `sharedWindows()` overlap | Nothing | **See the ADR-004 note below** |
| 9 | `proposed_later` sets `reminderDone`; the reminder is in-app on next open, not a notification | No `reminderDone` | Suppression can't tell a done reminder from a pending one |
| 10 | "Already connected today" counts `called`, `reacted` **and** `message` | Counts `called` only | Behavioural difference in suppression |

### One bug this surfaced, now fixed

The prototype takes its cooldown from the last cue on **any** day
(`state.cues.at(-1)`). The Kotlin version derived it from today's entries,
so a cue at 23:55 would not suppress one at 00:05 — "today" is empty by then.
`CuePolicy.decide` now takes `lastCueAt` separately, with a regression test.

### ADR-004 needs amending, not overturning

ADR-004 says "no parent-side read path anywhere in the schema". The prototype
has `schedules[day] = { you, mom }` and computes shared free windows, gated on
`sharing && momConsent`.

That is **availability**, not activity — and it is mutual and consented, which
is a different thing from the guardrail the handoff was protecting. The
guardrail stands as written for activity data. ADR-004 should be tightened to
say exactly that, rather than being read as forbidding the Harvest screen.

Note also: `cueEligibility` and the Slack Tide screen both state the privacy
boundary in user-facing copy. That copy is a commitment. Whatever the schema
ends up allowing, it must not contradict those sentences.

## Resolved: the parent gets no software

**Decided 2026-09-10 — see ADR-007.** Conversation becomes one-sided (notes
the user writes, handed off to SMS or WhatsApp to actually send) and Harvest
becomes self-entered (the user's own guess at when their person is free).
`sharing`, `momConsent` and the schedules table are dropped. The UI copy
promising mutual consent has to change before the study.

The reasoning, kept because it is the argument to re-open if the study pushes
back:

### Why it was a blocker

ADR-002 says the parent installs nothing, and that is load-bearing — it is the
argument that killed the whole VoIP-fork category.

Two prototype screens assume otherwise:

- **Conversation** is two-way chat. `ChatMessage.mine: boolean` means someone
  on the other end is sending.
- **Harvest** stores `schedules[day] = { you, mom }` and computes overlapping
  free windows. Mom's intervals have to come from somewhere.

In the prototype both are local fakes — a seeded sample family in
`localStorage`. That is fine for a demo and impossible for a week-long study
with real people. Either the parent gets a surface, or these two screens mean
something different in v0.1 than they appear to.

This gated roughly half the tables in a whole-prototype build, which is why it
was settled before migration 0003 was written.

## Divergence walkthrough

All ten resolved 2026-09-10 and implemented in migrations 0003 and 0004.
"Adopt" means the prototype's behaviour is the target and the schema follows
it.

| # | Divergence | Decision | Notes |
| --- | --- | --- | --- |
| 1 | Fifth resolution, `message` | **Adopt** | A distinct user action, and `minimum: 'any'` counts it toward the Jar. Enum add on both sides |
| 2 | Per-person entries | **Adopt** | `contacts` is already multi-row; needs `ledger_entries.contact_id`. Note the sample set includes a *group* ("Our little tribe"), so contacts need a `kind` of person vs group |
| 3 | `dispatch` source; prototype has no `session_end` | **Adopt `dispatch`, keep `session_end`** | Settings already exposes `sessionMinutes` and calls it "saved for future native support". Keeping it costs nothing; removing it means a migration to add it back in v0.2 |
| 4 | Cues tracked separately from moments | **Adopt** | New `cues` table, `ledger_entries.cue_id`. The cap counts cues, and a cue that produced no moment is exactly the silent-dismissal signal the study wants |
| 5 | `cuesEnabled`, default off, behind a privacy dialog | **Adopt** | This answers the handoff's open "degraded mode" question, and a real global opt-out is a guardrail in its own right |
| 6 | `minimum: any \| call \| null` | **Adopt** | Needed for the Jar |
| 7 | Global 3-option `sound` vs per-contact `cueSoundRef` | **Open** | The two disagree. See below |
| 8 | `sharing`, `momConsent`, schedules | **Dropped** | No second party to share with. ADR-007 |
| 9 | `reminderDone` | **Adopt** | Current code infers "pending" from `proposedTime > now`, which is a proxy that breaks when the user acts early or late. Store the fact instead of guessing it |
| 10 | "Already connected today" counts `called`, `reacted`, `message` | **Adopt** | A real product judgement — it treats sending a heart as connecting. That matches the no-pressure design and the copy ("You already connected today. Enjoy the quiet.") |

### 7, in more detail

The handoff lists a per-person cue sound as an MVP screen: "the parent's
ringtone or a chosen song". That personalisation is arguably the point of the
cue — a sound that means *this person*, not a generic notification.

The prototype instead has one global choice of chime / soft / silent, which
reads like a v0 simplification rather than a design decision.

Recommendation: keep both. A global default in settings, an optional
per-contact override. It is a superset, so neither design is foreclosed.

## Schema consequences

Once the above lands, `user_thresholds` holds `cuesEnabled`, `minimum`,
`sound` and `reducedMotion` alongside the four numbers. Rename it
`user_settings`; the name is already wrong.

## Repo hygiene in harvest-pulse

Two files are committed that should not be: `harvest-pulse-updated.zip` and
`harbor-quick-share-beacon-game.patch`. Worth removing and gitignoring.
