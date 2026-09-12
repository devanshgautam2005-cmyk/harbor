package app.harbor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Harbor's palette, taken from "Harbor Specimen - All Screens".
 *
 * The design is a botanical catalogue: a flat warm-grey ground, frosted white
 * plinth cards, near-black Newsreader for anything the app says, and letter-
 * spaced captions for everything secondary. Colour is held right back, so the
 * only saturated things on a page are a flower and the occasional gold mark.
 *
 * Two things about this palette are easy to get wrong, and both were wrong in
 * the first pass of this reskin:
 *
 *  - **The action colour is ink, not green.** Every filled button in the
 *    specimen is `#22211F` with ground-coloured text. Green appears only
 *    inside flower artwork and on a switch that is on. A green button looks
 *    plausible and is not what the design does.
 *  - **Cards are lighter than the ground, not darker.** They are white at
 *    around 70% with a *white* rim, so a card reads as frosted glass laid on
 *    the page. The dark hairline belongs to outline chips, not to cards.
 *
 * The garden field does not read from here. Its terrain lives in
 * [app.harbor.domain.Field] (`VEG`, `WATER`, `BARE`) and its weather in
 * `FieldSky`, both hardcoded, so nothing in this file can repaint the field.
 */

/** The ground everything sits on. */
val Paper = Color(0xFFEDECEA)

/**
 * A card.
 *
 * The specimen paints `rgba(255,255,255,.68)` over the ground; this is that,
 * already composited. It is kept opaque on purpose — a card that is genuinely
 * translucent stops being legible the moment it is laid over the garden, and
 * over the ground the two are indistinguishable.
 */
val Cream = Color(0xFFFAFAF9)

/** A surface that should recede rather than advance. */
val Sand = Color(0xFFF3F2F0)

/** Ink, and the fill of every action the app is actually asking for. */
val Ink = Color(0xFF22211F)

/** Captions and labels — ink at about half strength, over the ground. */
val Muted = Color(0xFF82817F)

/**
 * The frosted rim.
 *
 * White at 90%, sitting between a near-white card and the grey ground. It is
 * *lighter* than both the card edge and the page, which is what makes a card
 * read as glass rather than as a box. Cards use this; chips use [Hairline].
 */
val CardEdge = Color(0xE6FFFFFF)

/** The drawn line: outline chips, dividers, anything that must read as a rule. */
val Hairline = Color(0xFFD5D4D2)

/**
 * The warm stripe down every other day column on the week grid.
 *
 * Its own colour rather than `surfaceVariant`, which is a cool grey that all
 * but disappears against the bone ground — and the banding is the only thing
 * that makes seven narrow columns countable without reading the labels.
 */
val BandWarm = Color(0xFFF6EFE4)

/** Gold. The accent, and deliberately rare. */
val Gold = Color(0xFFE7B23F)

/** The one green the interface uses: a switch that is on. */
val Leaf = Color(0xFF6E9443)

// --- the greens of the garden -------------------------------------------
//
// Only ever illustration: a stem, a leaf, a bloom. The interface itself uses
// green in exactly one place, the switch above, and nowhere else.

/** The deep green of a stem. */
val Stem = Color(0xFF2E4B2A)

/** A leaf in shadow. */
val Forest = Color(0xFF3E6B33)

/** A leaf in light. */
val LeafLight = Color(0xFF4A7A3C)

/** Brown rather than red: this app has nothing angry to say. */
val Bark = Color(0xFF8A6A4F)

// --- contact tones ------------------------------------------------------
//
// In the specimen a person's tone is their hue at 35-45% over the card, which
// keeps every one of them light enough to sit under [Ink]. These are those,
// composited, so there is one ink and never a second rule about contrast.

val SurfaceGreen = Color(0xFFC9D6B9)
val SurfaceGold = Color(0xFFF1D9A5)
val SurfaceOrange = Color(0xFFE7C4B6)
val SurfaceSky = Color(0xFFBECCF8)

// --- the marks ----------------------------------------------------------
//
// The same four tones at full strength, for the little petal mark on a
// specimen's plinth. The Surface* fills above are hues at 35-45% and are
// meant for an area -- an avatar, a card. At 11dp they simply disappear, so
// the mark gets the hue itself.

val MarkGreen = Leaf
val MarkGold = Gold
val MarkOrange = Color(0xFFD9604A)
val MarkSky = Color(0xFF507AF5)
