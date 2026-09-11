package app.harbor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif for the things Harbor says, sans for the things it labels.
 *
 * The prototype pairs Lora with DM Sans. Those are Google Fonts and are not
 * bundled here yet, so this uses the platform serif and sans — the right
 * *pairing*, with the exact faces still to come. Swapping them in later is a
 * change to this file alone.
 */
private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

val Typography = Typography(
    // Harbor's own voice: "A quiet moment.", "It opened."
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 21.sp, lineHeight = 28.sp),

    // Everything the interface says about itself.
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.6.sp),
)
