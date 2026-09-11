# Harbor

An Android app that catches the still moment after you stop walking, and makes
it easy to call a parent.

The parent installs nothing. Raw activity data never leaves your phone.

## Repo layout

| Path | What |
| --- | --- |
| `app/` | The Android app. Kotlin + Compose. Gradle root is the repo root — open this folder in Android Studio. |
| `backend/` | Supabase schema as SQL migrations. There is no application server. |
| `docs/` | Product overview, decision records, study protocol, and the original design handoff. |

## Getting set up

**Android**

1. Clone somewhere **outside OneDrive/Dropbox.** Gradle's file churn plus a
   sync client produces file-lock build failures. `~/AndroidStudioProjects/` is
   the convention here.
2. Open the repo root in Android Studio and let it sync. The toolchain (AGP
   9.4, Gradle 9.6, compileSdk 37, JDK 25) came from the Studio wizard — let
   Studio manage it rather than hand-editing versions.
3. Run on a device or emulator, API 26+.

**Backend**

See [`backend/README.md`](backend/README.md). You need the Supabase CLI and a
`.env` copied from `.env.example`.

## Where to start reading

- [`docs/00-product.md`](docs/00-product.md) — what it is and the build order
- [`docs/01-decisions.md`](docs/01-decisions.md) — read before changing
  architecture
- [`docs/03-week-one-study.md`](docs/03-week-one-study.md) — what the study
  has to answer
- [`docs/04-how-the-repos-fit.md`](docs/04-how-the-repos-fit.md) — why there
  are two repositories and how work moves between them. Read this first if
  you are joining
- [`docs/05-changing-the-ui.md`](docs/05-changing-the-ui.md) — where every
  screen of the prototype ended up, the palette and component vocabulary, and
  what looks like a bug but is a decision. Start here if you are restyling or
  reskinning
- [`docs/06-onboarding.md`](docs/06-onboarding.md) — the brief for a first-run
  flow that does not exist yet: what it must collect, why the order is what it
  is, and the permission screen the study lives or dies on
- [`docs/07-reskin-prompt.md`](docs/07-reskin-prompt.md) — a prompt to hand a
  coding agent with reference images when you want the app restyled, carrying
  the constraints a reskin would otherwise break
- [`docs/slack-tide-handoff.html`](docs/slack-tide-handoff.html) — the original
  pipeline spec. Open it in a browser.

## Contributing

[`CONTRIBUTING.md`](CONTRIBUTING.md). Short version: branch as
`<yourname>/<thing>`, PR into `main`, fill in the template.

## Status

Early. The Android app is still Studio wizard output; the schema and docs are
deliberately ahead of the code. `docs/00-product.md` has the ordered list of
what to build.
