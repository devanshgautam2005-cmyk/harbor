-- The rest of the flower library, alone in its own migration.
--
-- `flower_kind` was created in 0005 with the eight kinds the picker showed at
-- the time, when it was a grid of four suggestions. The picker has since
-- become a shelf you scroll, and the Kotlin enum grew twice: six kinds when
-- the shelf arrived, and four more to fill the corners of the palette that
-- were still empty — a true white, a deep red, an indigo and a teal.
--
-- Neither of those reached the schema, which matters because the sync layer
-- maps these by name (see the header of `domain/Model.kt`). A participant who
-- chose a lavender would have had that row rejected on upload. So this catches
-- the type up with all ten at once.
--
-- Same reason as 0004 for being its own file: Postgres will not let a value
-- added to an enum be *used* in the transaction that added it, so anything
-- referencing these has to come in a later migration.
--
-- IF NOT EXISTS so this is safe to re-run, and safe on a database where some
-- of them were added by hand while the study was being set up.

-- Added with the shelf.
alter type flower_kind add value if not exists 'lavender';
alter type flower_kind add value if not exists 'zinnia';
alter type flower_kind add value if not exists 'camellia';
alter type flower_kind add value if not exists 'periwinkle';
alter type flower_kind add value if not exists 'buttercup';
alter type flower_kind add value if not exists 'anemone';

-- The four corners: white, deep red, indigo, teal.
alter type flower_kind add value if not exists 'snowdrop';
alter type flower_kind add value if not exists 'dahlia';
alter type flower_kind add value if not exists 'iris';
alter type flower_kind add value if not exists 'hydrangea';
