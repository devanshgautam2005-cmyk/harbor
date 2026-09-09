-- Harbour v0.1 — initial schema
--
-- Design notes (read before changing anything here):
--
--  1. The DEVICE is authoritative. The trigger pipeline (sensing, threshold,
--     suppression, kairos) runs entirely on-device against local storage.
--     This database is a sync target and the research export for the week-one
--     study. Never make the app block on a network call to decide whether to
--     fire a cue.
--
--  2. Raw activity data never lands here. We store the RESULT of a trigger
--     (a ledger entry), never the walking/usage stream that produced it.
--     See docs/01-decisions.md, ADR-004.
--
--  3. There is deliberately no parent-facing role, table, or policy. The
--     handoff guardrail is "share activity data with a parent, ever" = never.
--     If a parent-side feature is ever built, it gets its own reviewed
--     migration, not a widened policy here.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------- enums

create type trigger_source as enum ('walking_stop', 'session_end', 'manual');
create type resolution     as enum ('called', 'reacted', 'proposed_later', 'dismissed');
create type feedback_pulse as enum ('good_time', 'bad_time');
create type reward_shown   as enum ('readout', 'jar_fill', 'wrapped_clip');

-- ---------------------------------------------------------------- profiles

create table profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  display_name text,
  -- study cohort tag, e.g. 'bitsom-week1'. Null for non-study users.
  cohort      text,
  created_at  timestamptz not null default now()
);

-- ---------------------------------------------------------------- contacts
-- The person being called. v0.1 assumes a normal cellular number: the parent
-- installs nothing. See ADR-002.

create table contacts (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users (id) on delete cascade,
  label       text not null,
  phone_e164  text not null check (phone_e164 ~ '^\+[1-9]\d{7,14}$'),
  -- filename/uri of the cue sound chosen for this contact; resolved on-device
  cue_sound_ref text,
  created_at  timestamptz not null default now()
);

create index contacts_user_id_idx on contacts (user_id);

-- ---------------------------------------------------------- user_thresholds
-- User-calibrated, never a locked product default. Ship suggested values,
-- let the user move them. Guardrail from the handoff, section 7.

create table user_thresholds (
  user_id          uuid primary key references auth.users (id) on delete cascade,
  walking_minutes  integer not null default 12 check (walking_minutes between 1 and 240),
  session_minutes  integer not null default 20 check (session_minutes between 1 and 240),
  daily_cap        integer not null default 2  check (daily_cap between 0 and 10),
  cooldown_minutes integer not null default 180 check (cooldown_minutes between 0 and 1440),
  updated_at       timestamptz not null default now()
);

-- ----------------------------------------------------------- ledger_entries
-- Written at pipeline stage 9. Read by stage 3 (suppression) and by the
-- study export. client_id is the device-generated id, so a retried upload
-- is idempotent rather than a duplicate row.

create table ledger_entries (
  id                 uuid primary key default gen_random_uuid(),
  user_id            uuid not null references auth.users (id) on delete cascade,
  client_id          uuid not null,
  entry_date         date not null,
  trigger_source     trigger_source not null,
  threshold_snapshot jsonb not null,
  resolution         resolution not null,
  proposed_time      timestamptz,
  feedback_pulse     feedback_pulse,
  reward_shown       reward_shown,
  -- when the cue actually fired on the device, not when it synced
  occurred_at        timestamptz not null,
  synced_at          timestamptz not null default now(),

  unique (user_id, client_id),

  -- a proposed time only makes sense for the propose_later resolution
  constraint proposed_time_matches_resolution check (
    (resolution = 'proposed_later') = (proposed_time is not null)
  )
);

create index ledger_entries_user_date_idx on ledger_entries (user_id, entry_date desc);

-- -------------------------------------------------------------------- RLS
-- Every table is owner-only. No cross-user reads, no parent role.

alter table profiles        enable row level security;
alter table contacts        enable row level security;
alter table user_thresholds enable row level security;
alter table ledger_entries  enable row level security;

create policy "own profile"    on profiles        for all using (auth.uid() = id)      with check (auth.uid() = id);
create policy "own contacts"   on contacts        for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own thresholds" on user_thresholds for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own ledger"     on ledger_entries  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- --------------------------------------------------------------- triggers
-- New user gets a profile row and a default threshold row, so the app never
-- has to handle a missing-settings state on first run.

create function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id) values (new.id);
  insert into user_thresholds (user_id) values (new.id);
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

create trigger user_thresholds_touch
  before update on user_thresholds
  for each row execute function touch_updated_at();
