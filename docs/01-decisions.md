# Decisions

Architecture decision records. One per irreversible-ish choice. If you are
about to do something that contradicts one of these, change the ADR in the
same PR — don't just do the thing.

---

## ADR-001 — Android-native, Kotlin + Compose, v0.1 Android only

**Status:** accepted

The trigger depends on OS activity signals. On Android that is the Activity
Recognition Transition API; on iOS it is Core Motion. Both are native. A
cross-platform shell would still need two native modules for the only part of
the product that is hard, so it buys nothing.

Android only for v0.1 because the handoff's phasing says so, and because the
week-one study cohort is an Android cohort. iOS is v0.3.

---

## ADR-002 — The call is a plain cellular call. We do not fork Signal or any
## VoIP client.

**Status:** accepted

The original concept floated integrating with Signal so Harbour could be the
user's default caller. Three things kill that, in descending order of how
fatal they are:

1. **The parent installs nothing.** This is the whole product. A student's
   mother is not installing Signal, Jami, or Linphone to receive a call from
   an app her kid is testing for a week. Any VoIP path requires software on
   both ends. A cellular call requires software on neither.
2. **Signal blocks third-party clients.** There is no public client API, and
   Signal has refused to let forks talk to their servers since the LibreSignal
   decision. A fork would be a client with no network to connect to.
3. **Licensing.** Signal's Android client is GPLv3. Linphone and Jami are
   GPLv3 too. Forking any of them makes Harbour GPLv3 and open-source. That
   may be fine — but it should be a deliberate choice, not a side effect of
   picking a calling library.

Separately: "default caller" was never a Signal capability. It is an Android
telecom role (`ROLE_DIALER`), which Harbour can request directly.

### What we build instead

`Intent(Intent.ACTION_CALL)` with the contact's number, plus a
`TelephonyCallback` on call state to detect when the call ends so stage 7
(the reward readout) can fire. `CALL_PHONE` + `READ_PHONE_STATE` permissions.
No dialer role, no in-call UI to own, no fork.

### Open-source call apps evaluated

| Project | Licence | Why not for v0.1 |
| --- | --- | --- |
| [Signal Android](https://github.com/signalapp/Signal-Android) | GPLv3 | Servers closed to forks; parent must install it |
| [Linphone](https://gitlab.linphone.org/BC/public/linphone-android) | GPLv3 (commercial licence available) | SIP VoIP — needs a SIP account and an app on both ends. Real option *if* Harbour ever needs in-app voice; the commercial licence exists if GPL is a problem |
| [Jami](https://f-droid.org/en/packages/cx.ring/) | GPLv3 | Peer-to-peer, no server to run — genuinely nice, but still needs Jami on the parent's phone |
| [Fossify Phone](https://f-droid.org/en/packages/org.fossify.phone/) | GPLv3 | A real open-source *dialer* (Kotlin, actively maintained, Simple-Mobile-Tools successor). This is the one to fork **if** we later decide Harbour should own the in-call screen. Not needed to merely place a call |

**Revisit this ADR if** the product ever needs to own the in-call experience
(a during-call UI, a call recording, a custom ringback). Then Fossify Phone is
the starting point and GPLv3 becomes the licence conversation.

---

## ADR-003 — The device is authoritative; Supabase is sync and export

**Status:** accepted

Sensing, threshold check, suppression check and kairos refinement all run
on-device against local storage. The cue must fire on a train with no signal.
Supabase receives ledger entries after the fact.

Consequence: the suppression check (stage 3) reads the *local* ledger, not the
server. Sync conflicts are resolved by `(user_id, client_id)` upsert — the
device generates the id, so a retried upload is idempotent.

Consequence: no endpoint the app blocks on before showing a cue. If you add
one, you have broken the product on campus wifi.

---

## ADR-004 — Raw activity data never leaves the device

**Status:** accepted, non-negotiable

The handoff guardrail is "share activity data with a parent, ever" = never.
We go further: raw walking and usage streams never reach *our* server either.
Only the result of a trigger — a ledger entry — syncs.

This is also what makes the permission explainer screen honest. If we ever
upload the stream, that screen becomes a lie, and the permission ask is
already the biggest install-funnel risk in the product.

There is deliberately no parent-facing table, role, or RLS policy in the
schema. A parent-side feature gets its own reviewed migration.

---

## ADR-005 — Walking-stop trigger only in v0.1. Not Google Fit.

**Status:** accepted

Use the **Activity Recognition Transition API** (Google Play services). It is
independent of Google Fit, which is being retired — do not build against Fit,
and do not add Health Connect unless step *history* is wanted elsewhere.

The app-session trigger (`UsageStatsManager`) is v0.2. It is the
platform-harder half and the study does not need it to answer its questions.

---

## ADR-006 — Package name

**Status:** open — needs doing before anything ships

The Studio wizard generated `com.example.harbour`. `com.example.*` cannot be
published to Play and should not go out even to study participants. Rename to
`app.harbour` via Studio's refactor (not by hand) before the first build we
hand to anyone.
