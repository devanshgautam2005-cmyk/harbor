package app.harbor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Harbor's theme, mapped onto the specimen palette in [Color].
 *
 * Three deliberate departures from what the Studio wizard produced:
 *
 *  - **No dynamic colour.** It repaints the app from the user's wallpaper,
 *    which is exactly wrong here: the palette is the product. Harbor is a
 *    specimen sheet on every phone, or it is not Harbor.
 *  - **No dark theme.** The specimen is light only. There is a dark screen in
 *    it — the cue at night — but that is one screen inverting itself, not a
 *    second palette, and a dark Harbor would be a different design rather
 *    than the same one after dusk.
 *  - **Printed, not inflated.** The specimen's cards and buttons sit at 8-10px.
 *    The old ladder topped out at 28dp and made every card read as a bubble.
 *
 * `primary` is ink because every filled action in the specimen is ink. That
 * looks odd in a colour scheme and is correct: the accent in this design is
 * the absence of colour everywhere else.
 */
private val HarborColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Sand,
    onPrimaryContainer = Ink,

    secondary = Muted,
    onSecondary = Paper,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,

    tertiary = Gold,
    onTertiary = Ink,
    tertiaryContainer = SurfaceGold,
    onTertiaryContainer = Ink,

    background = Paper,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Muted,
    // The whole container ladder, not part of it.
    //
    // lightColorScheme() fills anything left unset from the Material baseline
    // palette, which is purple. Setting only some of these is worse than
    // setting none: Card resolves to surfaceContainerHighest, so leaving that
    // one out painted every card #E6E0E9 lavender on a cream page -- half the
    // pixels of the cues screen, on a palette with no purple in it at all.
    surfaceContainerLowest = Cream,
    surfaceContainerLow = Cream,
    surfaceContainer = Cream,
    surfaceContainerHigh = Sand,
    surfaceContainerHighest = Sand,
    surfaceBright = Cream,
    surfaceDim = Sand,

    // Used when something dark sits on the page -- snackbars, mostly. Baseline
    // leaves these a neutral charcoal that belongs to a different app.
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = SurfaceGreen,

    outline = Muted,
    outlineVariant = Hairline,

    // Brown, not red. Nothing here is an alarm.
    error = Bark,
    onError = Paper,
    errorContainer = Sand,
    onErrorContainer = Bark,
)

/** The specimen's radii: 8px for a button, 10px for a card, and little else. */
private val HarborShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun HarborTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HarborColors,
        typography = Typography,
        shapes = HarborShapes,
        content = content,
    )
}
