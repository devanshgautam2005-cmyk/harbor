# How the two repositories fit together

Harbor lives in two places. This is what each one is for, and how work moves
between them.

If you have just joined: read this, then `docs/00-product.md`, then
`docs/01-decisions.md` before changing anything architectural.

## They do not merge

The two repositories share no files and no language. `git merge` between them
would produce a directory containing both, not an application. Nothing in this
document describes a branch operation.

One is a **specification**. The other is the **product**.

## `harvest-pulse` — the specification

https://github.com/Bored-Kxiden/harvest-pulse — public, Next.js + React,
generated and maintained through v0, auto-deploying on every merge to its
`main`.

Eight screens: Home/Inbox, Harvest, Garden, Conversation, Dispatch, Slack
Tide, Settings, plus Lighthouse Beacon, Quick Share and a daily family game.
It stores everything in `localStorage` against a seeded sample family, and
labels every simulated element as simulated.

The product logic lives in `lib/harbor/model.ts` — the state shape,
`cueEligibility()`, `addMoment()`, `beaconWarmth()`, `gameForDay()`. That file
is the **authority on product behaviour**. Where it and the original design
handoff disagree, it wins; `docs/02-ui-reconciliation.md` records how each of
the ten known disagreements was settled.

What it is not: it is not a prototype in the throwaway sense, and it is not the
thing users will install. It is where behaviour gets decided cheaply, before
anyone spends a week building it in Kotlin.

## `harbor` — this repository, the product

Kotlin + Compose, backed by Supabase. Private.

| Layer | What is there |
| --- | --- |
| `domain/` | The model and `CuePolicy` — pipeline stages 2-4 as one pure function |
| `sensing/` | Stage 1: `BoutTracker` plus the Play services plumbing |
| `data/` | `HarborRepository` and its SharedPreferences implementation |
| `ui/` | `CuesSetupScreen` — the permission explainer and the cue switch |
| `backend/` | The Postgres schema as migrations |

It implements about one of the eight screens, plus the entire trigger pipeline
that the prototype can only simulate with a form field. That asymmetry is the
point: the hard part of Harbor is the part the web cannot do.

## The three flows

### 1. Behaviour: prototype to native

The main flow, and one-directional. For each screen: read the `.tsx` and the
model functions it calls, implement it in Compose, and record any divergence
in `docs/02-ui-reconciliation.md` with the reasoning.

Port behaviour, not structure. A React component tree is not a Compose one,
and copying the shape of one into the other produces bad Kotlin. What must
survive intact is what the user experiences and what gets written down.

### 2. The model, kept in lockstep three ways

`lib/harbor/model.ts` ↔ `domain/Model.kt` ↔ `backend/supabase/migrations/`.

These three describe the same objects in three languages. They agree today.
When one moves, the other two move in the same pull request — a resolution
that exists in TypeScript but not in the Postgres enum is a row the app cannot
sync, and it will not be discovered until someone is standing in a corridor
trying to use it.

### 3. Native to prototype, rarely

Only when platform reality kills a design. ADR-007 is the live example: the
parent installs nothing, so Conversation's two-way chat and Harvest's shared
schedule cannot be built as drawn. Those screens get reshaped rather than
ported, and the prototype should either follow or be marked as no longer the
target for them.

## The pin

The schema and the Kotlin model track a **named commit** of the prototype, not
its moving `main`. The current pin is recorded at the top of
`docs/02-ui-reconciliation.md`.

This exists because the prototype moves faster than the implementation. Two
pull requests landed there during the reconciliation that produced the current
schema, and the second changed the model's shape. Without a pin, "the app
matches the prototype" is a sentence with no fixed meaning.

Bumping the pin is a deliberate act: read the diff of `lib/harbor/model.ts`
since the pin, decide what each change means for the schema, update the
divergence table, and bump. Do not chase the prototype commit by commit —
some of what lands there will be cut before the study, and migrating twice for
a feature that never ships is wasted work.

## What is ported, and what is left

| Prototype screen | Native status |
| --- | --- |
| Slack Tide | Trigger is real and done. The cue surface is build-order item 5 |
| Settings | Partial — `CuesSetupScreen`. Still needs thresholds, sound, and "what counts as enough" |
| Resolution | Item 5 |
| Home / Inbox, Garden | Item 8. Without these the reward stage is a dead end |
| Conversation, Harvest | **Reshape, do not port.** ADR-007 |
| Dispatch, Beacon, Quick Share, daily game | Item 10. Beacon is nearly free — it derives from moments already stored |

## Working agreement

- Design decisions get made in the prototype, where they are cheap.
- Porting happens here, against a pinned commit.
- **A prototype change that alters `lib/harbor/model.ts` gets flagged, not
  silently absorbed.** Say so in the PR description or tell whoever is holding
  the native side. Everything else can be picked up at the next pin bump.

The risk this guards against is not merge conflicts — there can be none. It is
the specification moving out from under an implementation while everyone
assumes they still agree.
