package app.harbor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif for the things Harbor says, sans for the things it labels.
 *
 * The dark design pairs **Instrument Serif** at 400 with **Manrope**, keeping
 * the split the light specimen had while reversing where the weight lives.
 * Serif is now reserved for Harbor's own voice — a person's name, a headline,
 * "It opened." — at regular weight, where the size is the emphasis. Everything
 * the interface says *about itself* is sans at 500–700: a button label, a tab,
 * a chip, a caption.
 *
 * That reversal is deliberate, and it is the thing to preserve if these styles
 * are ever retuned. The light specimen set button labels in serif because it
 * was imitating a printed page. On a dark ground a serif button label reads as
 * a pull-quote — like something being said, rather than something to press.
 *
 * The tracking on the label styles is the other half. The design sits around
 * 1.8px at 11px, far wider than a UI label normally wants, which is the point.
 *
 * **Still outstanding:** neither face is bundled, so this uses the platform
 * serif and sans — the right *pairing*, with the exact faces still to come.
 * The gap matters more here than it did: this design leans hard on type, so a
 * phone with an odd system serif drifts further from the drawing than it would
 * have on the light skin. Shipping Instrument Serif and Manrope is a change to
 * this file alone.
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
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 19.sp),

    // Caps, spaced the way the specimen spaces them.
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 2.0.sp),
)
