-- New enum values, alone in their own migration.
--
-- This is not tidiness. Postgres runs each migration in a transaction, and
-- will not let a value added to an existing enum be *used* in the same
-- transaction that added it — you get "unsafe use of new value of enum type".
-- 0004 references both of these, so they have to be committed first.
--
--   'message'  — the prototype's fifth resolution. Sending a note is its own
--                action, distinct from a reaction, and counts toward the Jar
--                under minimum = 'any'.
--   'dispatch' — a cue raised by the Dispatch content pipeline rather than by
--                sensing.
--
-- 'session_end' is deliberately kept even though the prototype has no such
-- source: the settings screen already exposes a session threshold and calls
-- it saved for future native support. See docs/02-ui-reconciliation.md.

alter type resolution add value 'message';
alter type trigger_source add value 'dispatch';
