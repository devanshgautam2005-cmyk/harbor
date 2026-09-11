-- The words of a line the user left.

-- Harbor used to keep only that a line had happened, which made the history a
-- column of identical rows -- you could see that you had written to your mum
-- five times and not one of what you said.

-- This is a deliberate reversal of the earlier position, taken with the team.
-- The screen that collects it no longer claims the words are unstored.

-- Deliberately NOT added to study_daily_rollup or any other export view. The
-- study counts that a message happened; it has no business reading what a
-- participant said to their parent. If an export ever needs this column,
-- that is an ethics question before it is a schema one.

alter table ledger_entries
  add column if not exists note text;

comment on column ledger_entries.note is
  'Text of a line the user left. Null for calls and for lines left before '
  'v2. Never selected by study export views.';
