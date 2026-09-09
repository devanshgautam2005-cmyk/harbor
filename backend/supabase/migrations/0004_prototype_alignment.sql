-- Align the schema with the UI prototype (harvest-pulse).
--
-- v0.1 targets the whole prototype, so the ten divergences catalogued in
-- docs/02-ui-reconciliation.md had to be resolved rather than deferred. Eight
-- are adopted here. The ninth (cue sound) is adopted as a superset: the global
-- default lands in user_settings and contacts.cue_sound_ref already provides
-- the per-contact override. The tenth (schedule sharing) is dropped — see
-- ADR-007: the parent gets no software, so there is no second party to share
-- a schedule with.

-- The two new enum values this migration depends on ('message', 'dispatch')
-- were added in 0003, alone, because Postgres will not let a new enum value
-- be used in the transaction that added it.

-- --------------------------------------------------------------- contacts
-- The prototype's sample set includes "Our little tribe", which is a group
-- rather than a person. It reads differently in the UI and it cannot be
-- dialled, so the distinction has to survive into the model.

create type contact_kind as enum ('person', 'group');

alter table contacts
  add column kind contact_kind not null default 'person';

-- ------------------------------------------------------------------- cues
-- A cue that fired, whether or not it produced a ledger entry.
--
-- The prototype counts the daily cap in cues, not moments, and that is the
-- right unit: a cue the user swiped away without answering still spent one of
-- their two. It is also the study's silent-dismissal signal — a cue with no
-- entry hanging off it is a cue nobody engaged with, which no
-- resolution-based count can see.
--
-- fired_date is the device's *local* day, stored rather than derived, for the
-- same reason entry_date is: the server has no idea what timezone the phone
-- was in.

create table cues (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users (id) on delete cascade,
  client_id      uuid not null,
  fired_date     date not null,
  trigger_source trigger_source not null,
  fired_at       timestamptz not null,
  synced_at      timestamptz not null default now(),

  unique (user_id, client_id)
);

create index cues_user_date_idx on cues (user_id, fired_date desc);

alter table cues enable row level security;

create policy "own cues" on cues
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- --------------------------------------------------------- ledger_entries

alter table ledger_entries
  -- Which cue this resolved. Null for a manual moment the user started
  -- themselves. on delete set null so pruning old cues cannot drop history.
  add column cue_id uuid references cues (id) on delete set null,

  -- Who it was with. Nullable because contacts can be deleted and losing the
  -- person is better than losing the moment.
  add column contact_id uuid references contacts (id) on delete set null,

  -- Whether a proposed-later reminder has actually been dealt with.
  -- Previously inferred from proposed_time > now(), which is a proxy that
  -- gets it wrong whenever the user acts early or late.
  add column reminder_done boolean not null default false;

create index ledger_entries_cue_idx on ledger_entries (cue_id);

alter table ledger_entries
  add constraint reminder_done_only_for_proposals
  check (not reminder_done or resolution = 'proposed_later');

-- ----------------------------------------------------------- user_settings
-- The table holds four numbers plus four preferences now, so "thresholds" is
-- the wrong name. Renaming while nothing has shipped.

alter table user_thresholds rename to user_settings;
alter policy "own thresholds" on user_settings rename to "own settings";

alter table user_settings
  -- Default OFF, and the prototype puts a privacy explainer in front of the
  -- switch. This is the real opt-out, and it answers the handoff's open
  -- "degraded mode" question: if the user says no, Harbor's cues are simply
  -- off, and nothing degrades silently.
  add column cues_enabled boolean not null default false,

  -- What the user counts as a connected day, for the Jar. Null = undecided,
  -- which is a real state the UI shows differently.
  add column minimum text check (minimum in ('any', 'call')),

  -- Global default cue sound. contacts.cue_sound_ref overrides it per person.
  add column sound text not null default 'chime'
    check (sound in ('chime', 'soft', 'silent')),

  add column reduced_motion boolean not null default false;

-- The new-user trigger referenced the old table name.
create or replace function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id) values (new.id);
  insert into user_settings (user_id) values (new.id);
  return new;
end;
$$;

-- ------------------------------------------------------------------- views
-- study_threshold_drift follows the rename automatically (views bind to the
-- table's oid, not its name). study_daily_rollup has to be rebuilt, because
-- cues fired is no longer the same thing as entries written — which is the
-- whole point of adding the cues table.

drop view study_daily_rollup;

create view study_daily_rollup
with (security_invoker = true) as
select
  c.user_id,
  c.fired_date as entry_date,
  count(distinct c.id)                                        as cues_fired,
  -- A cue with nothing hanging off it. Study question 2: this is the number
  -- that a resolution-only count cannot see.
  count(distinct c.id) filter (where e.id is null)            as unresolved,
  count(e.id) filter (where e.resolution = 'called')           as called,
  count(e.id) filter (where e.resolution = 'reacted')          as reacted,
  count(e.id) filter (where e.resolution = 'message')          as messaged,
  count(e.id) filter (where e.resolution = 'proposed_later')   as proposed_later,
  count(e.id) filter (where e.resolution = 'dismissed')        as dismissed,
  count(e.id) filter (where e.feedback_pulse = 'good_time')    as good_time,
  count(e.id) filter (where e.feedback_pulse = 'bad_time')     as bad_time
from cues c
left join ledger_entries e on e.cue_id = c.id
group by c.user_id, c.fired_date;

comment on view study_daily_rollup is
  'Cue-centric: rows are cues that fired. Manual moments with no cue are '
  'deliberately excluded — study question 2 asks what happens to a cue.';
