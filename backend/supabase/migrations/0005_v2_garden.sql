-- The prototype's v2 model: calls become flowers, and life reads as weather.
--
-- The prototype was rebuilt around the garden (harvest-pulse, "Rebuild Harbor
-- around the garden, the cue and the flower a call leaves"). It is the
-- authority on product behaviour, so the schema follows.
-- See docs/02-ui-reconciliation.md for the full v1 → v2 delta.

-- ---------------------------------------------------------------- enums

-- How a call left the user, asked once afterwards. This is the reward and
-- also the input to it: each feeling grows a particular flower.
create type feeling as enum ('light', 'warm', 'steady', 'tender');

-- What a call becomes. There is no score and no streak — a flower that grew
-- stays grown, which is the whole point of the garden as a reward surface.
create type flower_kind as enum (
  'daisy', 'marigold', 'cosmos', 'poppy',
  'tulip', 'bluebell', 'aster', 'sunflower'
);

-- How life feels, on a scale the user sets. Replaces the earlier "season".
-- Weather rather than a rating on purpose: weather happens to you and passes,
-- which is a kinder frame for a hard week than a number.
create type weather as enum ('clear', 'bright', 'cloudy', 'rain', 'storm');

-- The colour a person is drawn in, across their avatar and their garden plot.
create type tone as enum ('green', 'gold', 'orange', 'sky');

-- ---------------------------------------------------------------- contacts

alter table contacts
  add column tone tone not null default 'green';

-- ----------------------------------------------------------- user_settings

alter table user_settings
  add column weather weather not null default 'clear';

-- "What counts as enough" is gone from the prototype's v2 model: the garden
-- replaced the Jar, and a garden has no threshold to meet.
alter table user_settings
  drop column minimum;

-- ----------------------------------------------------------- ledger_entries

alter table ledger_entries
  -- How long the call ran. Feeds the "calls with her usually run ~12 min" line
  -- on the cue, which exists so the ask has a known size before anyone commits
  -- to it.
  add column call_minutes integer check (call_minutes is null or call_minutes between 0 and 1440),

  add column feeling feeling,

  -- Derived from feeling at the time and then kept, rather than recomputed.
  -- The garden must not rearrange itself because a mapping changed in a later
  -- release.
  add column flower flower_kind,

  -- The shape the user gave the call before it started: "Catch up", "Ask for
  -- help", "Share news", "Just because". Free text rather than an enum — it is
  -- a prompt, not a taxonomy, and the list will be edited on the way.
  add column topic text;

-- Only a call has a length, a feeling or a flower.
alter table ledger_entries
  add constraint call_fields_only_for_calls check (
    resolution = 'called'
    or (call_minutes is null and feeling is null and flower is null)
  );

comment on column ledger_entries.reward_shown is
  'Vestigial. The v1 reward variants (readout / jar fill / wrapped clip) were '
  'replaced by the garden flower. Left nullable rather than dropped so no '
  'historical row loses data; nothing writes it.';
