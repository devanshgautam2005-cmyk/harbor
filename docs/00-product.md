# Harbour — what it is, and the build order

## The product in one paragraph

Students at college mean to call their parents and don't. Not because they
don't want to — because the moment never quite arrives. Harbour watches for a
"slack tide": the still moment right after you stop walking. When it finds
one, it surfaces a low-friction prompt with three equal options — call now,
send a reaction, propose a later time — and logs whatever you chose. It is a
trigger, not a nag: every cue is dismissible at no cost, you set your own
sensitivity, and there is a hard cap on how often it can fire.

The parent installs nothing and sees nothing. Harbour is entirely on the
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

The study needs the shortest path to a cue that fires and a resolution that
logs. Everything else waits.

1. ~~**Package rename** `com.example.harbour` → `app.harbour`~~ — done. Was ADR-006.

2. ~~**Local storage + data model.**~~ — done. `domain/Model.kt` mirrors the
   Postgres schema; `data/HarbourStore` is SharedPreferences + `org.json`
   behind `HarbourRepository`. Stages 2-4 are implemented as the pure
   `domain/CuePolicy`, with unit tests. Nothing syncs yet, and nothing calls
   `CuePolicy` yet — sensing is what wires it up.
3. **Sensing + threshold + suppression + kairos.** Activity Recognition
   Transition API, foreground service, walking→still only. This is the risky
   part; get it firing reliably before any screen is pretty.
4. **Permission + privacy explainer.** The single biggest install-funnel risk.
   Copy needs a privacy pass before it ships, not after.
5. **Cue surface + three-way resolution + reward readout + feedback pulse.**
6. **Threshold calibration + settings + contact and cue-sound picker.**
7. **The call itself.** `ACTION_CALL` plus a `TelephonyCallback` to detect
   call end so the reward can fire (ADR-002).
8. **Supabase sync.** Upsert on `(user_id, client_id)`. Last, because the
   study can run without it if it slips — a local export would do.

## Explicitly not in v0.1

App-session trigger (v0.2), iOS (v0.3), parent-side anything, and the reward
variants beyond the plain readout.

## Known risks

- **The permission ask.** Activity recognition reads as invasive. If the
  explainer screen doesn't land, nothing downstream matters.
- **Background killing.** MIUI, ColorOS and OnePlus builds kill background
  services aggressively. This threatens stage 1 on exactly the phones an
  Indian college cohort actually carries. Test on a real MIUI device early,
  not on the emulator.
- **Degraded mode is undecided.** If activity permission is denied or revoked,
  does Harbour fall back to a manual prompt or switch off? Needs a decision
  before the study, not a default arrived at by omission.
