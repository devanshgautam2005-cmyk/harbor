# Harbor — what it is, and the build order

## The product in one paragraph

Students at college mean to call their parents and don't. Not because they
don't want to — because the moment never quite arrives. Harbor watches for a
"slack tide": the still moment right after you stop walking. When it finds
one, it surfaces a low-friction prompt with three equal options — call now,
send a reaction, propose a later time — and logs whatever you chose. It is a
trigger, not a nag: every cue is dismissible at no cost, you set your own
sensitivity, and there is a hard cap on how often it can fire.

The parent installs nothing and sees nothing. Harbor is entirely on the
student's side of the relationship.

## The pipeline

Nine stages, specced in full in `slack-tide-handoff.html` section 3.

```
1 Sensing            OS activity signal, no UI
2 Threshold check    against the user's OWN calibrated value
3 Suppression check  today's ledger — already called? cooldown? cap hit?
4 Kairos refinement  is the transition actually complete?
5 Cue surface        sound + screen, always dismissible
6 Resolution         call / react / propose later — equal weight, no default
7 Reward             "caught it" readout
8 Investment         one-tap good-time / bad-time pulse
9 Logged             write the ledger entry
```

Stages 1–4 are on-device and must work offline (ADR-003).

## Build order

v0.1 is the **whole prototype**, not Slack Tide alone. But the order still
puts the trigger first: it is the risky part, it is what the study actually
measures, and every other screen can be demoed by hand if the week runs short.

1. ~~**Package rename** `com.example.harbor` → `app.harbor`~~ — done.
2. ~~**Local storage + data model.**~~ — done. `domain/Model.kt` mirrors the
   Postgres schema; `data/HarborStore` is SharedPreferences + `org.json`
   behind `HarborRepository`. Stages 2-4 are the pure `domain/CuePolicy`, with
   unit tests. Nothing syncs yet, and nothing calls `CuePolicy` yet — sensing
   is what wires it up.
3. ~~**Sensing.**~~ — written and unit-tested; **never yet run against a real
   transition.** Activity
   Recognition Transition API via a broadcast receiver; no foreground service
   (ADR-008). `sensing/BoutTracker` turns the transition stream into
   walk-ended-in-stillness bouts as a pure state machine, with 12 tests
   covering the messy cases — dropped events, duplicates, a walk that becomes
   a commute, a backwards clock. `TransitionReceiver` runs it and hands the
   result to `CuePolicy`. A fired cue is recorded; surfacing it is item 5.
4. ~~**Permission + privacy explainer.**~~ — done and verified on an emulator
   2026-09-10; still needs a privacy read before the study. `ui/CuesSetupScreen` explains what is read and where it
   stays *before* the system dialog appears, and is also the switch that turns
   cues on. It reports paused rather than on when the permission has been
   revoked behind the app's back. The copy still wants a human pass — it is
   the single biggest install-funnel risk, and I wrote it, not a designer.
5. ~~**Cue surface + resolution + reward readout + feedback pulse.**~~ —
   written, not yet seen fire. `cue/CueActivity` is full-screen, over the lock
   screen, with the contact's photo and their ringtone (ADR-009).
   `cue/CueNotifier` posts it and degrades to a heads-up notification where
   the full-screen permission is refused. Stages 5-9 complete: a cue fires,
   the user answers, a row is written.
6. **The call itself.** `ACTION_DIAL` hands off to the phone's dialer with the
   number filled in; the user presses call. Completion is self-reported via a
   "Mark call completed" button, exactly as the prototype does it. No
   permissions (ADR-002).
7. **Settings + calibration + contacts + cue sounds.** Partly done:
   `ui/ContactScreen` covers the person, their number, their ringtone and
   their photo — the parts the cue depends on. Threshold calibration, the
   global sound and "what counts as enough" are still missing.
8. **Home / Inbox, Garden and the Jar.** Where a logged moment goes to live.
   Without this the reward stage is a dead end.
9. **Conversation and Harvest, reshaped.** One-sided notes, self-entered
   availability. See ADR-007 — and change the copy that currently promises
   mutual consent before anyone sees it.
10. **Dispatch, Beacon, Quick Share, the daily game.** The prototype's later
    additions. Beacon is nearly free — it derives from moments already
    stored. Quick Share needs a table and media storage; the game needs a
    small day-keyed one. Neither is built.
11. **Supabase sync.** Upsert on the row's own id, which the device generated.
    Last, because the study can run without it if it slips — a local export
    would do.

## Explicitly not in v0.1

App-session trigger (v0.2), iOS (v0.3), and any parent-facing surface at all
(ADR-007). The Wrapped-clip reward variant.

## Known risks

- **The permission ask.** Activity recognition reads as invasive. If the
  explainer screen doesn't land, nothing downstream matters.
- **Background killing.** MIUI, ColorOS and OnePlus builds kill background
  services aggressively. This threatens stage 1 on exactly the phones an
  Indian college cohort actually carries. Test on a real MIUI device early,
  not on the emulator.
- **Degraded mode is undecided.** If activity permission is denied or revoked,
  does Harbor fall back to a manual prompt or switch off? Needs a decision
  before the study, not a default arrived at by omission.
