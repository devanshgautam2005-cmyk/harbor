-- A cue that was accepted and came to nothing.

-- The ledger row is written as 'called' the moment the dialer opens, before
-- anything is known -- a call that happened has to be recorded even if the
-- user never comes back to say how it went. The cost of that trade is that
-- somebody who changes their mind at the dialer, or who rings and gets no
-- answer, was counted as having called.

-- 'not_reached' is the way to say no afterwards. Deliberately not folded into
-- 'dismissed': dismissing is declining the cue, this is accepting it and
-- coming away with nothing, and the study's second question is exactly the
-- distribution those two sit in.

-- Alone in its own migration because Postgres will not let a value added to an
-- existing enum be used in the transaction that added it.

alter type resolution add value if not exists 'not_reached';
