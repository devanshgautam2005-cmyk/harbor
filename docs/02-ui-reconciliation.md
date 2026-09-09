# Reconciling the backend with the UI prototype

**Source:** https://github.com/Bored-Kxiden/harvest-pulse — a v0-generated
Next.js prototype. The product logic lives in `lib/harbor/model.ts`; the
screens are `components/harbor/*.tsx`.

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

## Decisions needed

1. **Scope of v0.1.** The handoff phases Slack Tide alone into v0.1 and defers
   the rest. The prototype shows the whole product. Which is the target for
   the week-one study? This determines whether the schema grows by two tables
   or by eight.
2. **One repo or two.** `harvest-pulse` is v0-linked and auto-deploys on merge
   to `main`, which does not mix well with branch protection or with Android
   code living alongside it.
3. **Spelling.** The prototype says `harbor`, this repo says `harbour`, the
   GitHub repo says `harvest-pulse`. Pick one for the package, the schema and
   the product name before more of each accumulates.
4. **Is the prototype the spec?** If yes, `lib/harbor/model.ts` wins every
   disagreement in the table above and the backend follows it. Say so
   explicitly, because it is the cheapest way to settle items 1, 3, 9 and 10
   at once.

## Repo hygiene in harvest-pulse

Two files are committed that should not be: `harvest-pulse-updated.zip` and
`harbor-quick-share-beacon-game.patch`. Worth removing and gitignoring
whichever way the repo question lands.
