-- Week-one study export.
--
-- The questions this study has to answer (docs/03-week-one-study.md):
--   1. Does the walking-stop trigger fire at moments people call good?
--   2. What share of cues get dismissed vs resolved any other way?
--   3. Where do users move their threshold from the suggested default?
--
-- These views answer those three and nothing else. They are owner-scoped like
-- the tables underneath, so a participant can see their own data and no one
-- can see anyone else's. Aggregate research reads happen with the service key,
-- deliberately out-of-band.

create view study_daily_rollup
with (security_invoker = true) as
select
  user_id,
  entry_date,
  count(*)                                              as cues_fired,
  count(*) filter (where resolution = 'called')          as called,
  count(*) filter (where resolution = 'reacted')         as reacted,
  count(*) filter (where resolution = 'proposed_later')  as proposed_later,
  count(*) filter (where resolution = 'dismissed')       as dismissed,
  count(*) filter (where feedback_pulse = 'good_time')   as good_time,
  count(*) filter (where feedback_pulse = 'bad_time')    as bad_time
from ledger_entries
group by user_id, entry_date;

-- How far each user drifted from the shipped suggestion. A big spread here
-- means the default is wrong; everyone sitting on the default means either
-- it is right or the calibration screen is not discoverable.
create view study_threshold_drift
with (security_invoker = true) as
select
  user_id,
  walking_minutes,
  walking_minutes - 12 as walking_delta_from_default,
  session_minutes,
  daily_cap,
  cooldown_minutes,
  updated_at
from user_thresholds;
