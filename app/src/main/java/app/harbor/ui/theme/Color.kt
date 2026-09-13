package app.harbor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Harbor's palette, taken from "Harbor Reskin" — the dark glass language.
 *
 * The design is a warm dusk: a near-black ground, cards that are nothing but
 * white held at six percent, and a single amber that appears on the current
 * tab, the primary action and the selected chip. Nowhere else. Everything the
 * app *says* is white or a muted warm grey; everything it *asks for* is amber.
 *
 * ## What the dark pass changed, and what it kept
 *
 * The names in this file are the same names the light specimen used, because
 * every screen already refers to them and the reskin is meant to move the
 * colour without moving the code. Two of them now mean something subtler:
 *
 *  - **[Ink] is still dark.** It was the page's text colour *and* the fill of
 *    every action. On a dark ground those split: text is [Chalk], and [Ink]
 *    survives as what sits *on top of* a light thing — the initials on an
 *    avatar, the label on an amber button. Reading [Ink] as "the text colour"
 *    is the one mistake that paints a screen black on black.
 *  - **Cards are lighter than the ground again, but barely.** In the light
 *    specimen a card was near-white with a *lighter* rim. Here it is white at
 *    six percent over near-black, rimmed with white at nine. The relationship
 *    is identical; only the amounts collapsed.
 *
 * The fills below are composited rather than translucent, for the same reason
 * they were in the light specimen: a genuinely translucent card stops being
 * legible the moment it is laid over the garden. The *rims* stay translucent,
 * because a rim has to catch whatever it is lying on.
 *
 * The garden field does not read from here. Its terrain lives in
 * [app.harbor.domain.Field] and its weather in `FieldSky`, both hardcoded.
 */

// --- the ground and the glass -------------------------------------------

/** The ground everything sits on. The design's `Ground` token. */
val Paper = Color(0xFF0D0E11)

/**
 * A card: white at six percent over the ground, already composited.
 *
 * This is the design's `Raised glass` token, and the two agree to the byte —
 * which is a useful check that the ground and the card really are one
 * relationship rather than two hand-picked colours.
 */
val Cream = Color(0xFF1B1C21)

/** A surface that should recede rather than advance: white at four percent. */
val Sand = Color(0xFF17181B)

/**
 * Ink — what sits on top of a *light* thing.
 *
 * The label on an amber button, the initials on an avatar. Never the page's
 * text colour; that is [Chalk]. The design writes this as `#1a1206`, a brown
 * black rather than a neutral one, so it belongs to the amber it sits on.
 */
val Ink = Color(0xFF1A1206)

/** What the app says. The design's `Ink` token, which on a dark ground is white. */
val Chalk = Color(0xFFFFFFFF)

/** Captions and labels. The design's `Muted ink`. */
val Muted = Color(0xFF9C978F)

/**
 * The rim.
 *
 * White at nine percent, and deliberately translucent: a card's edge has to
 * catch the dusk gradient behind it on Home and the flat ground everywhere
 * else, and a composited rim can only do one of those. This is the whole of
 * the design's elevation — there is no shadow anywhere in it.
 */
val CardEdge = Color(0x17FFFFFF)

/** The drawn line: outline chips, dividers, anything that must read as a rule. */
val Hairline = Color(0x2EFFFFFF)

/**
 * The stripe down every other day column on the week grid.
 *
 * White at three percent. It has to be nearly nothing — seven narrow columns
 * on a near-black ground are already legible, and the banding only needs to
 * make them countable without reading the labels.
 */
val BandWarm = Color(0xFF141519)

// --- the one accent -----------------------------------------------------
//
// The design is strict about this: amber is the current tab, the primary
// action and the selected chip, and it appears nowhere else. Everything that
// wants to be noticed and is not one of those three gets brightness instead.

/** Amber. The design's `Accent`, and the brighter of the two. */
val Gold = Color(0xFFF0BD3E)

/** The primary action's fill. The design's `Primary action`. */
val Ember = Color(0xFFE08A3C)

/** The top of the primary action's gradient, which runs [EmberLight] to [Ember]. */
val EmberLight = Color(0xFFF5B85C)

/** The cool end of the dusk, behind the cards on Account and Schedule. */
val Dusk = Color(0xFF2F4A63)

/** Brown rather than red: this app has nothing angry to say. */
val Bark = Color(0xFF78513D)

/** The green a switch takes when it is on — see the note in `SettingsScreen`. */
val Leaf = Color(0xFF8FB25C)

// --- the greens of the garden -------------------------------------------
//
// Only ever illustration: a stem, a leaf, a bloom. Lifted from the light
// specimen's values, which were chosen to sit *under* a near-white page and
// vanish almost completely against a near-black one.

/** The deep green of a stem. */
val Stem = Color(0xFF5C7F4E)

/** A leaf in shadow. */
val Forest = Color(0xFF6F9A56)

/** A leaf in light. */
val LeafLight = Color(0xFF8FB25C)

// --- contact tones ------------------------------------------------------
//
// The design's four tones, and they are pale on purpose: a person's avatar is
// one of the few genuinely light objects in the app, so it reads as a lit
// thing on a dark page. All four carry [Ink] rather than [Chalk].

val SurfaceGreen = Color(0xFFCFE0C6)
val SurfaceGold = Color(0xFFF5C77A)
val SurfaceOrange = Color(0xFFE9C7A1)
val SurfaceSky = Color(0xFFC9D8E9)

// --- the marks ----------------------------------------------------------
//
// The same four tones for a mark too small to carry a tint. On a light page
// these had to be darkened to stay visible; here they need the opposite, so
// the mark and the surface are much closer than they used to be.

val MarkGreen = Color(0xFFCFE0C6)
val MarkGold = Gold
val MarkOrange = Color(0xFFF0A35F)
val MarkSky = Color(0xFFC9D8E9)
