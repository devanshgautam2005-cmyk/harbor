# Contributing

Small team, so this is short.

## Branches

`main` is protected — no direct pushes. Branch from `main` as
`<yourname>/<short-thing>`:

```
devansh/threshold-calibration
priya/cue-surface
```

One branch per thing. If you find yourself naming a branch `misc` or `fixes`,
it should be two branches.

## Commits

Present tense, say what changed and why if the why isn't obvious:

```
Add walking→still transition listener

Uses the Activity Recognition Transition API rather than Google Fit,
which is being retired. See ADR-005.
```

## Pull requests

Fill in the template. The "guardrails touched" line is the one that matters —
the product's guardrails (dismissibility, user-set thresholds, no data
sharing) are the kind of thing that erodes one reasonable-looking PR at a
time, so we make it explicit.

Get one review before merging. For anything touching the trigger pipeline or
the privacy boundary, get Devansh's.

## If you are changing architecture

Read `docs/01-decisions.md` first. If your change contradicts an ADR, update
the ADR in the same PR with the new reasoning. Don't leave the doc lying.

## Using Claude Code in this repo

`CLAUDE.md` at the root carries the project's context and hard constraints —
your session picks it up automatically. If you learn something the next person
would want to know (a build gotcha, a device quirk, a dead end), add it there
in your PR.
