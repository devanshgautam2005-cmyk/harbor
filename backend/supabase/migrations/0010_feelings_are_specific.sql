-- Flowers become call-specific feelings.
--
-- The moods 0009 added lasted one day. Usability testing on 2026-09-15 found
-- them still reading as a generic mood board rather than an answer to "how
-- did that call feel" — the screen after a call now asks the feeling
-- directly, and a shelf of adjectives untethered from any call was a weaker
-- answer to that question than one grown for it. `FlowerKind` in
-- `domain/Model.kt` is the authority; this catches the type up.
--
-- The eighteen species and the twenty moods both stay in the type, and that
-- is deliberate rather than lazy. Postgres cannot remove a value from an
-- enum, and more to the point it should not here: rows already synced were
-- written with those labels, and dropping them would orphan every flower
-- planted before this migration. The app maps every old name across on read
-- (`FlowerKind.stored`), so a phone that has been in the study since the
-- start keeps its garden; these rows keep their original label until the
-- phone next writes them.
--
-- Same reason as 0004, 0008 and 0009 for being its own file: Postgres will
-- not let a value added to an enum be *used* in the transaction that added
-- it, so anything referencing these has to come in a later migration.
--
-- IF NOT EXISTS so this is safe to re-run.

alter type flower_kind add value if not exists 'glad_we_talked';
alter type flower_kind add value if not exists 'lighter_now';
alter type flower_kind add value if not exists 'felt_loved';
alter type flower_kind add value if not exists 'she_remembered';
alter type flower_kind add value if not exists 'easy_silence';
alter type flower_kind add value if not exists 'steadier_now';
alter type flower_kind add value if not exists 'worth_slowing_down';
alter type flower_kind add value if not exists 'said_what_i_meant';
alter type flower_kind add value if not exists 'want_to_try_something';
alter type flower_kind add value if not exists 'asked_more_than_usual';
alter type flower_kind add value if not exists 'looking_forward';
alter type flower_kind add value if not exists 'still_thinking_about_it';
alter type flower_kind add value if not exists 'hard_to_shake_off';
alter type flower_kind add value if not exists 'time_to_actually_do_it';
alter type flower_kind add value if not exists 'nothing_left_unsaid';
alter type flower_kind add value if not exists 'wondering_if_that_landed';
alter type flower_kind add value if not exists 'said_the_hard_thing';
alter type flower_kind add value if not exists 'glad_she_picked_up';
alter type flower_kind add value if not exists 'wished_it_was_longer';
alter type flower_kind add value if not exists 'dreaded_this_one';
