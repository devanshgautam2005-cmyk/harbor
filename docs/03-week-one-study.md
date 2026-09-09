# Week-one study

## What we are trying to learn

Three questions, in priority order. If the build slips, protect question 1.

1. **Does the walking-stop trigger land at moments people call good?**
   Measured by the stage-8 feedback pulse: share of cues marked "good time".
2. **What happens to a cue?** Distribution across called / reacted /
   proposed-later / dismissed. A high dismiss rate is not automatically a
   failure — a *silent* dismiss rate with no feedback pulse is.
3. **Where do people move their threshold?** If everyone sits on the shipped
   default of 12 minutes, either it is right or the calibration screen is
   invisible. The exit interview separates those.

`backend/supabase/migrations/0002_study_export.sql` defines the views that
answer exactly these three and nothing else.

## Shape

- One week, Android only, a cohort we can talk to in person.
- Sideloaded APK. No Play listing — which is fine, but it means the package
  name still must not be `com.example.*` (ADR-006).
- Recruit for phone diversity, not convenience. At least a few MIUI/ColorOS
  devices, because background-service killing is the risk most likely to
  silently produce "the app just never fired" data.

## Consent

Participants are told, in writing, before install:

- what is sensed (walking → still transitions), and that the raw stream never
  leaves their phone;
- what syncs (that a cue fired, what they chose, and their settings);
- that their parent is never contacted by us and never sees anything;
- that they can uninstall at any time and ask for their data to be deleted.

Set `profiles.cohort` for participants so study rows are separable from ours.

## Instrumenting without breaking the promise

Do not add analytics that upload raw activity. If a question cannot be
answered from the ledger, the answer is an exit interview, not a new event
stream. ADR-004 is not negotiable for the convenience of the study.

## Exit interview

Ten minutes, in person, at the end of the week. Worth more than the numbers at
this sample size. Ask at minimum:

- Tell me about a cue you dismissed. What was happening?
- Did you ever change the threshold? Did you know you could?
- Did it ever fire at a moment that felt wrong? What made it wrong?
- Did you call anyone you wouldn't have otherwise?
