package app.harbor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif for the things Harbor says, sans for the things it labels.
 *
 * The specimen pairs **Newsreader** at weight 400 with **DM Sans** at 400/500,
 * and it never goes bolder than that. Every name, headline and button label is
 * regular-weight serif; every caption is small, uppercase and tracked wide.
 * That contrast — an unhurried name over a quiet, spaced-out label — is most of
 * what makes the design read as a catalogue rather than an app, so nothing in
 * this file is bold and emphasis comes from size and air alone.
 *
 * The tracking on the label styles is the other half. The specimen sits around
 * 0.18em at these sizes, far wider than a UI label normally wants, which is
 * exactly the point.
 *
 * **Still outstanding:** neither face is bundled, so this uses the platform
 * serif and sans — the right *pairing*, with the exact faces still to come.
 * That gap matters more now than it did: the old design leaned on colour, this
 * one leans on type, so a phone with an odd system serif will drift further
 * from the specimen than it used to. Shipping Newsreader and DM Sans is a
 * change to this file alone.
 */
private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

val Typography = Typography(
    // Harbor's own voice: "Looks like you're free.", "It opened."
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 39.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 26.sp),

    // Everything the interface says about itself.
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 19.sp),

    // Caps, spaced the way the specimen spaces them.
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 2.0.sp),
)
