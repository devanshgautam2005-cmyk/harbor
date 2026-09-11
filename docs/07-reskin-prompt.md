# The reskin prompt

A prompt to hand a coding agent along with reference images, to restyle Harbor
without breaking it.

**How to use it:** paste everything below the line into a fresh session in this
repo, attach your reference images, and add one sentence of your own about what
you want ("the warmth of image 2 with the type from image 4", "everything
colder", "this, but it must still read at night"). The prompt does the rest.

It is written to be re-used. If a reskin goes wrong in a way the prompt did not
prevent, add the lesson here rather than in the chat.

---

You are restyling **Harbor**, an Android app in Kotlin and Jetpack Compose. I
have attached reference images. Your job is to make Harbor look like they do,
without changing what it does.

## First, read the images out loud

Before touching any code, state back what you actually see, as a token set. Do
not skip this — a reskin that starts in the files becomes fifteen screens that
drifted apart.

- **Palette.** 6–9 colours with *roles*, not names: ground, raised surface,
  ink, muted ink, primary action, accent, and a non-alarming error colour.
  Give hex values. Say which single colour is the accent and where it is
  allowed to appear; an accent that appears everywhere is not an accent.
- **Type.** A display face and a text face, with the weights and the rough
  scale. Name real families.
- **Shape.** Corner radius, border vs. fill vs. shadow, how much air.
- **Density and mood.** Tight and technical, or roomy and quiet? Flat, or
  layered?
- **What you are deliberately not taking** from the images, and why.

Then say which parts of Harbor will change most, and show me the token set
before you write code. If the images disagree with each other, say so and
propose a resolution rather than averaging them into mush.

## Where the design actually lives

Four files are the design system. Change these and most of the app moves:

| File | What it holds |
| --- | --- |
| `ui/theme/Color.kt` | The palette, one constant per colour |
| `ui/theme/Type.kt` | The type scale and the faces |
| `ui/theme/Theme.kt` | The Material colour scheme and corner radii |
| `ui/theme/Harbor.kt` | The components: `Surface`, `SoftSurface`, `GoldSurface`, `PageIntro`, `SectionHeading`, `Eyebrow`, `SmallCopy`, `Notice`, `Avatar`, `PrimaryAction`, `QuietAction` |

Work at that layer first. Restyle a screen directly only when the token layer
genuinely cannot express what the images show, and say so when you do.

`docs/05-changing-the-ui.md` maps every screen to the file it lives in.

## The colours that are not in the theme — do not miss these

About fifty colour literals live outside `ui/theme/`. They are the most
distinctive things on screen, so a token-only reskin leaves the app looking
half-changed. Go through all of them:

| File | What its colours are |
| --- | --- |
| `ui/SkyWheel.kt` | Sky gradients per weather, the sun, clouds — for the plan garden |
| `ui/FieldSky.kt` | The same for the field: five weather tints, sun, clouds, rain, the dimming veil |
| `domain/Field.kt` | The terrain palette: five vegetation greens, two waters, bare ground |
| `ui/FieldCanvas.kt` | Rock, tilled rows, patch outlines, the patch name tags |
| `domain/Flowers.kt` | The flower library: petal, deep petal and heart for each of eight kinds |
| `cue/CueSurface.kt`, `cue/CallFlow.kt` | The full-screen gradients behind the cue and the reflection |
| `ui/WeatherBar.kt` | The slider's blue-to-gold fill |

The flower colours and the terrain greens are a palette of their own. Restyle
them as a set, against the new ground, rather than one at a time — and keep
the eight flowers distinguishable from each other, because the whole garden
reads as "which call was that" and identical flowers destroy it.

## The trap that will bite you

Harbor sets its own Material colour scheme. Material fills **any slot you
leave unset** from its baseline palette, which is purple.

This already happened once: `surfaceContainer` and `surfaceContainerHigh` were
set but `surfaceContainerHighest` was not — the slot a `Card` resolves to — and
every card rendered `#E6E0E9`, over half the pixels of one screen, in an app
whose palette contains no purple at all. Nothing in the palette was wrong; only
what was missing from it.

So: when you touch `Theme.kt`, set the **whole** container ladder and the
inverse slots, and if a component comes out an unexpected grey-lilac, you have
found an unset slot rather than a broken component.

## What must not change

These look like style and are not. Each is a decision recorded in
`docs/01-decisions.md`, and several are why the app is allowed to ask for the
permissions it asks for.

- **Nobody ever replies.** No screen may imply the parent sees anything, gets
  notified, or has an app. There is no other side (ADR-007).
- **The cue is call-shaped but never claims to be a call.** It shows her photo
  and plays her ringtone; the words never say "incoming call" (ADR-009).
- **Copy that describes what is sensed and where it stays is load-bearing.**
  Restyle it freely; do not rewrite what it claims.
- **Dismissing costs nothing.** No streaks, no scores, no progress bars toward
  a call.
- Harbor is light-only by decision. If the images imply a dark UI, stop and ask
  rather than inventing a dark theme.

## Things that are drawn, not styled

There are no image assets to swap. Flowers, the terrain, the sky, the nav and
cue icons and the week grid are all vector paths and generated geometry in
Kotlin. To restyle them you change colours and geometry in code. Do not add an
icon library or bitmap assets without asking — every mark currently restyles
with the palette for free, and that is worth keeping.

## Typefaces

`Type.kt` currently uses the device's system serif and sans; no font files
ship. If your images call for specific faces, add the `.ttf` files under
`app/src/main/res/font/`, declare a `FontFamily`, and point `Type.kt` at it —
and check the licence permits redistribution in an app binary.

## How to build and check your work

Gradle may not run in your environment. **CI is the reliable build, and for
Compose it is the only type-check** — Compose code cannot be checked locally
here, so pushing is how you find out whether it compiles.

1. Edit, commit, push. GitHub Actions attaches `harbor-debug-apk` to the run.
2. `adb install -r app-debug.apk` — this keeps existing data, because every
   build shares one committed debug keystore. Do not change that signing
   config.
3. **Screenshot every screen you touched and look at it.** A reskin is not
   done because it compiles. Home, the garden and field, the cue, the
   reflection, settings, the schedule grid, the line composer.
4. Pure-Kotlin logic can be tested locally without Gradle; `CLAUDE.md` has the
   recipe.

## What to report back

- The token set you derived, and anything in the images you chose not to take.
- Screenshots, before and after, of at least Home, the field and the cue.
- Any place the images and Harbor's constraints disagreed, and what you did.
- Anything you left un-restyled, and why.

Do not tell me it is finished until you have looked at the screenshots. If
something is wrong in them, say so plainly rather than describing the intent.
