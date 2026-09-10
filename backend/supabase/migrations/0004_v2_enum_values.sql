-- New trigger sources, alone in their own migration.
--
-- Postgres will not let a value added to an existing enum be *used* in the
-- transaction that added it, and 0005 references both of these.
--
--   'note' — a moment left from a note or snapshot the user sent
--   'game' — a moment left from the daily question
--
-- These replace 'dispatch' and 'signal' in the prototype's v2 model. Those two
-- values are deliberately NOT removed: dropping a value from a Postgres enum
-- means recreating the type and every column using it, which is a great deal
-- of risk to retire two labels nothing writes. They stay as vestigial, and the
-- Kotlin enum simply has no counterpart for them.

alter type trigger_source add value 'note';
alter type trigger_source add value 'game';
