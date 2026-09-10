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

The original concept floated integrating with Signal so Harbor could be the
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
   GPLv3 too. Forking any of them makes Harbor GPLv3 and open-source. That
   may be fine — but it should be a deliberate choice, not a side effect of
   picking a calling library.

Separately: "default caller" was never a Signal capability. It is an Android
telecom role (`ROLE_DIALER`), which Harbor can request directly.

### What we build instead

`Intent(Intent.ACTION_CALL)` with the contact's number, plus a
`TelephonyCallback` on call state to detect when the call ends so stage 7
(the reward readout) can fire. `CALL_PHONE` + `READ_PHONE_STATE` permissions.
No dialer role, no in-call UI to own, no fork.

### Open-source call apps evaluated

| Project | Licence | Why not for v0.1 |
| --- | --- | --- |
| [Signal Android](https://github.com/signalapp/Signal-Android) | GPLv3 | Servers closed to forks; parent must install it |
| [Linphone](https://gitlab.linphone.org/BC/public/linphone-android) | GPLv3 (commercial licence available) | SIP VoIP — needs a SIP account and an app on both ends. Real option *if* Harbor ever needs in-app voice; the commercial licence exists if GPL is a problem |
| [Jami](https://f-droid.org/en/packages/cx.ring/) | GPLv3 | Peer-to-peer, no server to run — genuinely nice, but still needs Jami on the parent's phone |
| [Fossify Phone](https://f-droid.org/en/packages/org.fossify.phone/) | GPLv3 | A real open-source *dialer* (Kotlin, actively maintained, Simple-Mobile-Tools successor). This is the one to fork **if** we later decide Harbor should own the in-call screen. Not needed to merely place a call |

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

**Scope of this ADR, tightened 2026-09-10.** As originally written this read
as "no data about the user may ever reach anyone else", which would forbid
things the product is supposed to do. The guardrail is specifically about
**activity data** — walking, app usage, and anything derived from sensing.
Those never leave the device.

It is not a general ban on the user sharing something deliberately. If a
future feature lets someone share a moment, a note, or their own availability,
that is a product question decided on its own merits, not a violation of this
ADR. What it may never become is a channel through which a parent learns
something about the user's movements or phone habits.

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

**Status:** done (2026-09-10)

The Studio wizard generated `com.example.harbor`, which cannot be published
to Play and should not go out even to study participants. Renamed to
`app.harbor` across sources, tests and the Gradle namespace/applicationId.

---

## ADR-007 — The parent gets no software. Two prototype screens change shape.

**Status:** accepted (2026-09-10)

v0.1 targets the whole prototype (docs/02-ui-reconciliation.md), and two of its
screens quietly assume a second participant:

- **Conversation** is two-way chat. `ChatMessage.mine: boolean` means someone
  on the other end is sending.
- **Harvest** stores `schedules[day] = { you, mom }` and computes overlapping
  free windows. The mother's intervals have to come from somewhere.

In the prototype both are local fakes — a seeded sample family in
`localStorage`. That is fine for a demo and impossible for a week-long study
with real people, who will notice within a day that nobody is on the other
end.

### Decision

The parent installs nothing, and gets no web surface either. ADR-002 stands
unchanged, and the two screens are reshaped rather than the constraint being
relaxed:

- **Conversation becomes one-sided.** Notes the user writes, kept for
  themselves or handed off to the phone's own SMS or WhatsApp to actually
  send. Harbor is not a messenger and should stop implying it is.
- **Harvest becomes self-entered.** The user records when they think their
  person is usually free. It is their own guess, labelled as such, and there
  is no consent flow because there is no second party. `sharing` and
  `momConsent` disappear from the model; so does the schedules table.

### Why

The reciprocity in the prototype's copy is genuinely nicer. But building it
means a parent client, an identity for that parent, an invitation flow, and a
second consent surface — and every one of those is a place where activity data
could leak toward a parent, which is the one thing ADR-004 exists to prevent.
That is a large amount of new risk in exchange for a feature the study is not
even trying to measure.

The study's three questions are all about the trigger. None of them needs the
mother to be online.

### Cost, stated plainly

The Harvest screen gets weaker: a guess at someone's routine is worth less
than their actual availability. If the study says people want real
reciprocity, that is the moment to revisit this — with the parent surface
designed deliberately rather than arrived at by a prototype's convenience.

### Consequence for the copy

**Not a problem in the web prototype.** Checked 2026-09-10: it already labels
every simulated element as one — "Simulated consent · demo only", "You are
editing sample data, not a real parent's calendar", "This demo does not send
data to Mom", and an aria-label of "Simulate Mom consent". Nothing there
claims a consent that does not exist. Do not "fix" it.

**It becomes a problem the moment these screens are rebuilt natively.** A
study build has no demo framing to hang that honesty on. A "Mom's permission"
toggle in a real app, with no Mom behind it, stops being a labelled simulation
and becomes a false claim — the exact kind that costs trust when a participant
works out it is not true.

The rule for the native build, then: no consent switch, no "waiting for Mom",
no mutual-window language. Availability is the user's own note about someone
else's routine, and should read like one.

---

## ADR-008 — No foreground service for sensing

**Status:** accepted (2026-09-10)

The build order and the handoff both assumed a foreground service owning the
sensing loop, carried over from the sibling project where the shield genuinely
needed one.

Harbor does not. The Activity Recognition Transition API delivers to a
`PendingIntent` whether or not the app is running — that is the whole point of
it, and it is why it exists separately from the sampling API. A service of our
own would add a permanent notification, a battery footprint, a
`FOREGROUND_SERVICE` permission and, on API 34+, a foreground service *type*
that Harbor would struggle to justify, all in exchange for nothing the
platform is not already doing.

So: `TransitionReceiver` is a plain broadcast receiver, woken by Play
services, using `goAsync()` for the few milliseconds it takes to read the
ledger and write a cue.

### What this costs

A permanent notification is also a *disclosure* — the user can see sensing is
running. Without one, the only surface saying so is the settings screen. Given
`cues_enabled` defaults off and is gated behind a privacy explainer, that is a
defensible trade, but it puts more weight on that explainer being honest.

### What it does not fix

Aggressive OEM battery management. MIUI, ColorOS and OnePlus builds suspend
background delivery, and a foreground service would not reliably survive them
either — those OEMs kill those too. This remains the single largest threat to
the week-one study, and it fails *silently*: the data just looks like a user
who never walked. Test on a real MIUI device before recruiting, not on the
emulator.
