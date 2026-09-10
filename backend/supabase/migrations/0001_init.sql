-- Harbor v0.1 — initial schema
--
-- Design notes (read before changing anything here):
--
--  1. The DEVICE is authoritative. The trigger pipeline (sensing, threshold,
--     suppression, kairos) runs entirely on-device against local storage.
--     This database is a sync target and the research export for the week-one
--     study. Never make the app block on a network call to decide whether to
--     fire a cue. (ADR-003.)
--
--  2. Because the device is authoritative, it also owns identity: every row
--     the app creates carries a UUID the phone generated, used directly as the
--     primary key. There is no server-generated id to map back to, so a
--     retried upload is a plain `on conflict (id) do update` and a phone that
--     has been offline for a week can push its whole backlog in one go.
--
--  3. Raw activity data never lands here. We store the RESULT of a trigger,
--     never the walking or usage stream that produced it. (ADR-004.)
--
--  4. There is deliberately no parent-facing role, table, or policy. The
--     parent installs nothing and has no account. (ADR-002, ADR-007.)
--
--  Enum values and defaults follow the UI prototype (harvest-pulse), which is
--  the reference for product behaviour. See docs/02-ui-reconciliation.md.

-- ---------------------------------------------------------------- enums

-- 'signal' and 'game' belong to prototype features whose tables are not built
-- yet. They are declared now because adding a value to an existing enum
-- cannot be done in the same transaction that uses it, which forces an
-- awkward two-migration dance later. Declaring them up front costs nothing.
create type trigger_source as enum (
  'walking_stop',   -- a walking bout ended. The only sensed source in v0.1
  'session_end',    -- an app session ended. v0.2, see ADR-005
  'dispatch',       -- raised by the Dispatch content pipeline
  'signal',         -- raised by a Quick Share signal
  'game',           -- raised by the daily family game
  'manual'          -- the user asked for the prompt themselves
);

create type resolution as enum (
  'called',
  'reacted',
  'message',
  'played',
  'proposed_later',
  'dismissed'
);

create type feedback_pulse as enum ('good_time', 'bad_time');

create type reward_shown as enum ('readout', 'jar_fill', 'wrapped_clip');

-- A group cannot be dialled, and reads differently in the UI.
create type contact_kind as enum ('person', 'group');

-- ---------------------------------------------------------------- profiles

create table profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  display_name text,
  -- Study cohort tag, e.g. 'bitsom-week1'. Null for non-study users.
  cohort       text,
  created_at   timestamptz not null default now()
);

-- ---------------------------------------------------------------- contacts
-- Someone worth calling. v0.1 assumes an ordinary cellular number: the parent
-- installs nothing. See ADR-002.

create table contacts (
  id            uuid primary key,
  user_id       uuid not null references auth.users (id) on delete cascade,
  label         text not null,
  kind          contact_kind not null default 'person',
  -- Null only for a group. The format check applies when a number is present.
  phone_e164    text check (phone_e164 ~ '^\+[1-9]\d{7,14}$'),
  -- Overrides user_settings.sound for this person. Resolved on-device.
  cue_sound_ref text,
  created_at    timestamptz not null default now(),

  constraint a_person_needs_a_number
    check (kind = 'group' or phone_e164 is not null)
);

create index contacts_user_id_idx on contacts (user_id);

-- ----------------------------------------------------------- user_settings
-- Four numbers plus four preferences. Every one of them is the user's to set:
-- ship suggestions, never lock defaults. Handoff, section 7.
--
-- The ranges match the prototype's settings form, so a value the web UI
-- accepts cannot be rejected here (or the reverse).

create table user_settings (
  user_id          uuid primary key references auth.users (id) on delete cascade,

  walking_minutes  integer not null default 10  check (walking_minutes between 1 and 120),
  session_minutes  integer not null default 20  check (session_minutes between 1 and 180),
  daily_cap        integer not null default 2   check (daily_cap between 1 and 10),
  cooldown_minutes integer not null default 120 check (cooldown_minutes between 1 and 1440),

  -- The real opt-out. Off until the user turns it on behind a privacy
  -- explainer. Also the answer to the handoff's open "degraded mode"
  -- question: if activity permission is refused, cues are off and say so,
  -- rather than degrading into something the user did not choose.
  cues_enabled     boolean not null default false,

  -- What the user counts as a connected day, for the Jar. Null is a real
  -- state — "I haven't decided yet" — and the UI shows it differently.
  minimum          text check (minimum in ('any', 'call')),

  -- Global default cue sound; contacts.cue_sound_ref overrides it per person.
  sound            text not null default 'chime'
                     check (sound in ('chime', 'soft', 'silent')),

  reduced_motion   boolean not null default false,

  updated_at       timestamptz not null default now()
);

-- -------------------------------------------------------------------- cues
-- A cue that fired, whether or not the user answered it.
--
-- Separate from ledger_entries because the daily cap counts cues, not
-- moments: a cue the user swiped away without answering still spent one of
-- their two. It is also the study's silent-dismissal signal — a cue with no
-- entry hanging off it is one nobody engaged with, and no resolution-based
-- count can see those.
--
-- fired_date is the device's LOCAL day, stored rather than derived, because
-- the server has no idea what timezone the phone was in.

create table cues (
  id             uuid primary key,
  user_id        uuid not null references auth.users (id) on delete cascade,
  fired_date     date not null,
  trigger_source trigger_source not null,
  fired_at       timestamptz not null,
  synced_at      timestamptz not null default now()
);

create index cues_user_date_idx on cues (user_id, fired_date desc);

-- ----------------------------------------------------------- ledger_entries
-- Written at pipeline stage 9. Read by stage 3 (suppression) and by the
-- study export.

create table ledger_entries (
  id                 uuid primary key,
  user_id            uuid not null references auth.users (id) on delete cascade,

  -- The cue this resolved. Null when the user started the moment themselves.
  -- on delete set null so pruning old cues cannot destroy history.
  cue_id             uuid references cues (id) on delete set null,

  -- Who it was with. Nullable because contacts can be deleted, and losing the
  -- person is better than losing the moment.
  contact_id         uuid references contacts (id) on delete set null,

  entry_date         date not null,
  trigger_source     trigger_source not null,
  -- The thresholds in force when the cue fired, so a later recalibration
  -- cannot rewrite the past.
  threshold_snapshot jsonb not null,
  resolution         resolution not null,
  proposed_time      timestamptz,

  -- Whether a proposed-later plan has been dealt with. Stored rather than
  -- inferred from proposed_time having passed, which guesses wrong whenever
  -- the user acts early or late.
  reminder_done      boolean not null default false,

  feedback_pulse     feedback_pulse,
  reward_shown       reward_shown,

  -- When the moment happened on the device — not when it synced.
  occurred_at        timestamptz not null,
  synced_at          timestamptz not null default now(),

  constraint proposed_time_matches_resolution check (
    (resolution = 'proposed_later') = (proposed_time is not null)
  ),

  constraint reminder_done_only_for_proposals check (
    not reminder_done or resolution = 'proposed_later'
  )
);

create index ledger_entries_user_date_idx on ledger_entries (user_id, entry_date desc);
create index ledger_entries_cue_idx on ledger_entries (cue_id);

-- -------------------------------------------------------------------- RLS
-- Every table is owner-only. No cross-user reads, no parent role.

alter table profiles       enable row level security;
alter table contacts       enable row level security;
alter table user_settings  enable row level security;
alter table cues           enable row level security;
alter table ledger_entries enable row level security;

create policy "own profile"  on profiles       for all using (auth.uid() = id)      with check (auth.uid() = id);
create policy "own contacts" on contacts       for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own settings" on user_settings  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own cues"     on cues           for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own ledger"   on ledger_entries for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- --------------------------------------------------------------- triggers
-- A new user gets a profile row and a default settings row, so the app never
-- has to handle a missing-settings state on first run.

create function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id) values (new.id);
  insert into user_settings (user_id) values (new.id);
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

create function touch_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger user_settings_touch
  before update on user_settings
  for each row execute function touch_updated_at();
