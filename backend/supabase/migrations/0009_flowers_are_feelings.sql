-- Flowers become feelings.
--
-- The flower sheet names its twenty blooms for how a call left you rather than
-- for a species, and the app now does the same: the screen after a call asks
-- how it went, so the shelf it offers has to be a list of answers to that
-- question. `FlowerKind` in `domain/Model.kt` is the authority; this catches
-- the type up.
--
-- The eighteen species stay in the type, and that is deliberate rather than
-- lazy. Postgres cannot remove a value from an enum, and more to the point it
-- should not here: rows already synced were written with those labels, and
-- dropping them would orphan every flower planted before this migration. The
-- app maps the old names across on read (`FlowerKind.stored`), so a phone that
-- has been in the study since the start keeps its garden; these rows keep
-- their original label until the phone next writes them.
--
-- Same reason as 0004 and 0008 for being its own file: Postgres will not let a
-- value added to an enum be *used* in the transaction that added it, so
-- anything referencing these has to come in a later migration.
--
-- IF NOT EXISTS so this is safe to re-run.

alter type flower_kind add value if not exists 'happy';
alter type flower_kind add value if not exists 'upbeat';
alter type flower_kind add value if not exists 'loved';
alter type flower_kind add value if not exists 'valued';
alter type flower_kind add value if not exists 'peaceful';
alter type flower_kind add value if not exists 'grounded';
alter type flower_kind add value if not exists 'calm';
alter type flower_kind add value if not exists 'confident';
alter type flower_kind add value if not exists 'inspired';
alter type flower_kind add value if not exists 'curious';
alter type flower_kind add value if not exists 'hopeful';
alter type flower_kind add value if not exists 'reflective';
alter type flower_kind add value if not exists 'tense';
alter type flower_kind add value if not exists 'motivated';
alter type flower_kind add value if not exists 'content';
alter type flower_kind add value if not exists 'insecure';
alter type flower_kind add value if not exists 'brave';
alter type flower_kind add value if not exists 'grateful';
alter type flower_kind add value if not exists 'lonely';
alter type flower_kind add value if not exists 'anxious';
