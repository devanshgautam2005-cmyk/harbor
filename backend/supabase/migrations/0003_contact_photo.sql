-- A photo for the cue surface.
--
-- The cue is call-shaped (ADR-009): the contact's face and their ringtone are
-- what make the prompt land as that person rather than as the app.
--
-- This is a URI the user picked with the system photo picker, resolved
-- on-device. Deliberately NOT read from the contact's system entry: that would
-- need READ_CONTACTS, and a contacts permission sitting next to the activity
-- one would cost far more trust than a picked photo is worth.
--
-- The image itself never leaves the phone. Only this reference syncs, and it
-- is meaningless anywhere else — which is the intended behaviour, not a
-- limitation to fix later with an upload.

alter table contacts
  add column photo_ref text;

comment on column contacts.photo_ref is
  'Device-local URI from the system photo picker. The image never leaves the '
  'phone; this reference is meaningless on any other device.';
