package app.harbor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Harbor's theme, ported from the prototype rather than generated.
 *
 * Two deliberate departures from what the Studio wizard produced:
 *
 *  - **No dynamic colour.** It repaints the app from the user's wallpaper,
 *    which is exactly wrong here: the palette is the product. Harbor is paper
 *    and garden on every phone, or it is not Harbor.
 *  - **No dark theme.** The prototype is light only, and a dark Harbor would
 *    be a different design rather than the same one after dusk. When someone
 *    designs it, it goes here.
 */
private val HarborColors = lightColorScheme(
    primary = Forest,
    onPrimary = Paper,
    primaryContainer = PaleGreen,
    onPrimaryContainer = DeepGreen,

    secondary = Sage,
    onSecondary = Cream,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = DeepGreen,

    tertiary = Gold,
    onTertiary = DeepGreen,
    tertiaryContainer = Sand,
    onTertiaryContainer = DeepGreen,

    background = Paper,
    onBackground = DeepGreen,
    surface = Cream,
    onSurface = DeepGreen,
    surfaceVariant = Sand,
    onSurfaceVariant = Sage,
    surfaceContainer = Cream,
    surfaceContainerHigh = Sand,

    outline = Sage,
    outlineVariant = PaleGreen,

    // Brown, not red. Nothing here is an alarm.
    error = Bark,
    onError = Paper,
    errorContainer = Sand,
    onErrorContainer = Bark,
)

/** `--radius: 1.35rem`. Everything in Harbor is softly rounded. */
private val HarborShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
