package app.harbor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.harbor.R

/**
 * One typeface, not a pairing.
 *
 * This used to be Instrument Serif for the things Harbor says paired with
 * Manrope for the things the interface labels itself with — the right idea,
 * with neither face actually bundled, so every phone fell back to its own
 * system serif and sans and drifted from the drawing by exactly how odd that
 * phone's fonts were. Usability testing settled on Manjari for both instead:
 * one humanist face, bundled for real this time, so a name, a headline and a
 * button label are all the same typeface at different weights rather than two
 * different guesses at what the system provides.
 *
 * Manjari ships three weights only — Thin, Regular, Bold — with nothing
 * between Regular and Bold. The five styles below that used to ask for
 * [FontWeight.SemiBold] (every label, tab and button) collapse onto
 * [FontWeight.Bold] rather than [FontWeight.Normal]: those are the styles
 * that want to read as the interface *asking* for something, and dropping
 * them to Regular flattened that against the body copy around them.
 *
 * Manjari's Latin is a humanist, low-contrast face, closer to a sans with
 * calligraphic flicks than to the high-contrast serif Instrument Serif was.
 * The 32sp greeting on Home is the line most worth looking at after this
 * change — it is the one place the app leaned hardest on a serif's contrast
 * to carry a headline, and Manjari carries that weight differently.
 */
private val Manjari = FontFamily(
    Font(R.font.manjari_thin, FontWeight.Thin),
    Font(R.font.manjari_regular, FontWeight.Normal),
    Font(R.font.manjari_bold, FontWeight.Bold),
)

val Typography = Typography(
    // Harbor's own voice: "Looks like you're free.", "It opened."
    displayLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 39.sp),
    headlineLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 26.sp),

    // Everything the interface says about itself. SemiBold collapsed to Bold
    // — see the file note above.
    titleMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 19.sp),

    // Caps, spaced the way the specimen spaces them.
    labelLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp),
    labelSmall = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.0.sp),
)
