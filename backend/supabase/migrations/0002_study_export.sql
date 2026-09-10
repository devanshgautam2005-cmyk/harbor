-- Week-one study export.
--
-- The questions this study has to answer (docs/03-week-one-study.md):
--   1. Does the walking-stop trigger fire at moments people call good?
--   2. What share of cues get dismissed, or ignored entirely?
--   3. Where do users move their thresholds away from the suggestion?
--
-- These views answer those three and nothing else. They are owner-scoped like
-- the tables underneath, so a participant can see their own data and no one
-- can see anyone else's. Aggregate research reads happen with the service
-- key, deliberately out-of-band.

-- Cue-centric on purpose: rows are cues that fired. A manual moment with no
-- cue behind it is excluded, because question 2 asks what happens to a cue.
create view study_daily_rollup
with (security_invoker = true) as
select
  c.user_id,
  c.fired_date,
  count(distinct c.id)                                       as cues_fired,

  -- A cue with nothing hanging off it: shown, and never answered either way.
  -- This is the number a resolution-only count cannot see, and the one most
  -- likely to say the trigger is landing badly.
  count(distinct c.id) filter (where e.id is null)           as ignored,

  count(e.id) filter (where e.resolution = 'called')          as called,
  count(e.id) filter (where e.resolution = 'reacted')         as reacted,
  count(e.id) filter (where e.resolution = 'message')         as messaged,
  count(e.id) filter (where e.resolution = 'proposed_later')  as proposed_later,
  count(e.id) filter (where e.resolution = 'dismissed')       as dismissed,
  count(e.id) filter (where e.feedback_pulse = 'good_time')   as good_time,
  count(e.id) filter (where e.feedback_pulse = 'bad_time')    as bad_time
from cues c
left join ledger_entries e on e.cue_id = c.id
group by c.user_id, c.fired_date;

-- How far each user drifted from the shipped suggestion. A wide spread means
-- the default is wrong; everyone sitting exactly on it means either it is
-- right or the calibration screen is not discoverable — the exit interview is
-- what separates those two.
--
-- The baselines are read from the column defaults rather than hardcoded, so
-- this cannot silently disagree with 0001 when a suggestion is retuned.
create view study_threshold_drift
with (security_invoker = true) as
select
  s.user_id,
  s.walking_minutes,
  s.walking_minutes - d.walking_default   as walking_delta,
  s.session_minutes,
  s.daily_cap,
  s.cooldown_minutes,
  s.cooldown_minutes - d.cooldown_default as cooldown_delta,
  s.cues_enabled,
  s.minimum,
  s.updated_at
from user_settings s
cross join (
  select
    (select column_default::integer
       from information_schema.columns
      where table_schema = 'public'
        and table_name = 'user_settings'
        and column_name = 'walking_minutes')  as walking_default,
    (select column_default::integer
       from information_schema.columns
      where table_schema = 'public'
        and table_name = 'user_settings'
        and column_name = 'cooldown_minutes') as cooldown_default
) d;
