package app.harbor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Harbor's theme, mapped onto the dark glass palette in [Color].
 *
 * Three deliberate departures from what the Studio wizard produced:
 *
 *  - **No dynamic colour.** It repaints the app from the user's wallpaper,
 *    which is exactly wrong here: the palette is the product. Harbor is one
 *    dusk on every phone, or it is not Harbor.
 *  - **No light theme.** This is the inverse of what this file used to say.
 *    Harbor was light-only by decision, and the decision was reversed when the
 *    dark reskin was adopted; there is no `lightColorScheme` here and the app
 *    does not follow the system setting, because a design that flips is two
 *    designs and only one of them has been drawn.
 *  - **Printed, not inflated — but no longer flat.** The light specimen sat at
 *    8–10dp because a card was a printed plinth. A glass card is a physical
 *    object, and the design draws it at 24dp with every action a full pill.
 *
 * `primary` is amber because every filled action in the design is amber with a
 * brown-black label. The rule the design states is stricter than a colour
 * scheme can express: amber is the current tab, the primary action and the
 * selected chip, and nothing else. Anything reaching for `tertiary` to
 * decorate itself is breaking that rule, not following it.
 */
private val HarborColors = darkColorScheme(
    primary = Ember,
    onPrimary = Ink,
    primaryContainer = Sand,
    onPrimaryContainer = Chalk,

    secondary = Muted,
    onSecondary = Ink,
    secondaryContainer = Sand,
    onSecondaryContainer = Chalk,

    tertiary = Gold,
    onTertiary = Ink,
    tertiaryContainer = SurfaceGold,
    onTertiaryContainer = Ink,

    background = Paper,
    onBackground = Chalk,
    surface = Cream,
    onSurface = Chalk,
    surfaceVariant = Sand,
    onSurfaceVariant = Muted,
    // The whole container ladder, not part of it.
    //
    // darkColorScheme() fills anything left unset from the Material baseline
    // palette, which is purple. Setting only some of these is worse than
    // setting none: Card resolves to surfaceContainerHighest, so leaving that
    // one out painted every card lavender on a page with no purple in it.
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Sand,
    surfaceContainer = Cream,
    surfaceContainerHigh = Cream,
    surfaceContainerHighest = Cream,
    surfaceBright = Cream,
    surfaceDim = Paper,

    // Used when something light sits on the page -- snackbars, mostly. On a
    // dark ground the inverse pair is the light one, which is the opposite of
    // what it held before.
    inverseSurface = Chalk,
    inverseOnSurface = Ink,
    inversePrimary = Ember,

    outline = Muted,
    outlineVariant = Hairline,

    // Brown, not red. Nothing here is an alarm.
    error = Bark,
    onError = Chalk,
    errorContainer = Sand,
    onErrorContainer = Bark,
)

/**
 * The design's radii: 24dp for a card, and a pill for everything you press.
 *
 * This is the largest single change of the dark pass. The light specimen
 * collapsed every radius to 8–10dp so a card read as a printed plinth; the
 * dark design does the reverse, because a piece of glass with a tight corner
 * reads as a dialog. `medium` is the one to watch — it is what Material hands
 * to anything that does not ask for a shape by name.
 */
private val HarborShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(26.dp),
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
