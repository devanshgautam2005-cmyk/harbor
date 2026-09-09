# Harbor backend

Supabase (Postgres + auth + RLS). There is no application server: the Android
app talks to Supabase directly with the anon key, and row-level security does
the authorisation.

## Why so little backend

The trigger pipeline runs on-device. The database exists to (a) survive a
reinstall, (b) let us export the week-one study data. If you find yourself
adding an endpoint the app blocks on before showing a cue, stop — that is a
design error, not a missing feature. See `docs/01-decisions.md`, ADR-003.

## Layout

```
supabase/migrations/
  0001_init.sql           tables, enums, RLS, new-user trigger
  0002_study_export.sql   the three views the study actually needs
```

## Running it

Install the Supabase CLI, then from `backend/`:

```bash
supabase init
supabase start
supabase db reset
```

`supabase db reset` replays every migration against the local Postgres, so it
is also how you check a new migration before pushing it.

To point at the hosted project:

```bash
supabase link --project-ref <ref>
supabase db push
```

## Adding a migration

Never edit a migration that has been pushed. Add a new numbered file.

```bash
supabase migration new <short_name>
```

## Keys

Copy `.env.example` to `.env` and fill it in. `.env` is gitignored — the
service role key must never reach the repo or the Android app. The app only
ever gets the anon key, which is safe to ship precisely because RLS is on.
