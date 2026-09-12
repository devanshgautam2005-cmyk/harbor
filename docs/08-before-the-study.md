# Before you give this to people

A readiness list, written from the code as it stands rather than from
intentions. Everything marked **unproven** has genuinely never run on real
hardware; everything marked **hole** is a thing the study would measure wrongly
if it ran tomorrow.

The short version: **do not run the full cohort until one real walking-stop cue
has fired on a real phone, and until the app can tell you whether sensing was
alive.** Everything else on this list is smaller than those two.

---

## 1. The measurement hole that matters most

`Sensing.isActive` is `cuesEnabled && hasPermission`. That is all it is. It
does not know whether Google Play services ever registered the transitions,
whether the receiver is still alive, or whether the OS put the app to sleep
three days ago.

`SensingStore` keeps exactly one thing — `walking_since`. Nothing anywhere
records *the last time the system told us anything at all*.

So at the end of the week, a participant with zero cues is indistinguishable
between:

- the trigger works, and they simply never took a ten-minute walk;
- the trigger works, fired, and they dismissed it before it registered;
- Play services never delivered a single transition and the app was dead from
  day one, while telling them "Cues are on" the whole time.

The study's first question is *does the walking-stop trigger land at moments
people call good*. With this hole, a null result cannot be interpreted — and a
null result is the likeliest outcome on aggressive Android skins.

**Fix before the study.** Stamp a `last_transition_at` every time
`TransitionReceiver` runs, whatever the transition. Then:

- show it in the app ("last noticed you moving: 20 minutes ago"), so the
  participant can see it is alive and tell you when it is not;
- put it in the study export, so silence in the data can be told apart from
  silence in the world.

It is a handful of lines, and without it a week of fieldwork may produce
nothing you can defend.

---

## 2. Never run on real hardware

The whole sensing path has been exercised only by unit tests and by manual
cues. As of now, on a real phone:

| | Status |
| --- | --- |
| A real `walking_stop` cue firing | **unproven** — every cue ever recorded on a real device has been `manual` |
| The cue appearing over the lock screen | **unproven** |
| The ringtone actually playing from a cold, pocketed phone | **unproven** |
| The heads-up fallback when full-screen intent is refused | **unproven** |
| Sensing surviving a reboot (`BootReceiver`) | **unproven** |
| Sensing surviving a day of Doze and app standby | **unproven** |
| The export writing a file through the system save dialog | **unproven** on a phone |

The first one is the study. The rest are the difference between "it fired" and
"it fired and they saw it".

**Test protocol for one person, before anybody else gets it:** set it up, walk
for twelve unbroken minutes with the phone in a pocket and the screen off,
stop, and wait two minutes without touching the phone. Then reboot, repeat the
next day, and leave it untouched overnight to see whether it still works on day
two. That last part is where OEM battery management usually shows up.

---

## 3. Holes in the data itself

**`called` is recorded at the moment of dialling, not after the call.**
`CueActivity.placeCall` writes `Resolution.CALLED` before the dialer even
opens. Someone who taps *Call now*, sees the dialer, thinks better of it and
presses back is recorded as having called. There is no way to correct it —
the reflection asks how the call felt, not whether it happened.

The study's second question is the distribution across called / reacted /
proposed-later / dismissed. This inflates `called` by exactly the number of
people who changed their mind, which is not a small number.

**Fix:** an escape on the first reflection step — *we did not get to talk* —
that rewrites the row to `dismissed` or a new resolution. `docs/03` already
promises to write this up as "reported a call" rather than a measured one;
that phrasing only stays honest if there is a way to report *no*.

**No crash reporting and no telemetry, by design.** If Harbor crashes on a
participant's phone on day two, nobody finds out until the debrief, and the
week is gone. That is the correct privacy posture and it has a cost: plan to
check in with people mid-week rather than assuming silence means it is working.

---

## 4. Device and OS risk

Recruit for this deliberately — `docs/03` already says so, and it matters more
than it sounds.

- **One UI, MIUI, ColorOS** put unused apps to sleep aggressively. Harbor runs
  no foreground service on purpose (ADR-008), which is the right call for
  battery and for not being creepy, and it is exactly what makes it vulnerable
  to being killed.
- **Battery optimisation exemption is never requested.** Consider asking for
  it during onboarding on the devices that need it, or at minimum walking
  participants through the OEM setting by hand at setup. An app that is asleep
  is not a trigger that failed.
- **Android 16 / SDK 36** is what the test phone runs. Older devices in the
  cohort are a different code path for notifications and full-screen intents.
- The cue uses a **full-screen intent**, which recent Android versions restrict.
  Check the fallback on every OEM in the cohort, not just one.

---

## 5. Getting the data back

The app uploads nothing and holds no `INTERNET` permission. The only route out
is a participant opening Account and saving a file.

- **Collect it in person, at the debrief.** An instruction to send it later
  will lose the tail of the cohort.
- **An uninstall, a factory reset, or "clear data" destroys the week.** There
  is no backup and no server copy. Tell people not to clear the app, and get
  the file before anything else happens to the phone.
- **Only the last 90 entries are retained.** Fine for a week at a cap of two a
  day; worth knowing before anyone runs a longer study.
- One file per phone, named by day and a short opaque participant id. Check
  you can actually open and parse one *before* the cohort, not after.

---

## 6. The human side

- **Consent in writing, before install** — already the plan in `docs/03`. It
  should say what is sensed, that the raw stream never leaves the phone, that
  their family are never contacted and never see anything, and that the file
  they hand over carries no names, numbers or words.
- **The copy has never been read by anyone but its author.** The permission
  explainer is the single biggest install-funnel risk in the product and I
  wrote it. Get a person who is not on this team to read it cold.
- **What the participant tells their parent** is not nothing. Someone who
  starts calling home daily will be asked why. Decide whether you want that in
  the debrief questions; it is probably the most interesting thing the study
  could learn and it is not currently asked.
- **A cue during something awful.** Activity recognition cannot tell a walk
  home from a walk out of a hospital. Nothing in the app can fix that, but the
  debrief should ask whether a cue ever landed badly, and the consent should
  make clear it can be turned off in one tap.

---

## 7. Rough order to fix

**Before anyone outside the team installs it**

1. Record and surface `last_transition_at`, and carry it into the export.
2. Get one real `walking_stop` on one real phone.
3. Add the *we did not get to talk* escape.

**Before the full cohort**

4. Run a two-person, two-day pilot including an overnight and a reboot.
5. Battery-optimisation guidance or prompt, per OEM.
6. An outside read of the permission and privacy copy.
7. Ship the real typefaces — the app currently uses whatever the phone has, so
   it looks different on every device in the cohort.

**Nice, not blocking**

8. Onboarding currently restarts at the welcome screen if it is backgrounded
   half way. Saved data survives, so it is harmless, but it is untidy on a
   first run.
9. `GardenCanvas` is dead code since the field landed.

---

## 8. The pilot is the thing

Two people, two days, one of them on a Samsung or Xiaomi, with a reboot and an
overnight in the middle. Then read their exports with the same script you will
use for the real thing.

Nearly everything on this list is the kind of problem that a two-day pilot
surfaces immediately and that a one-week cohort surfaces only in the debrief,
when it is too late to do anything but write it up.
