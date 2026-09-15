package app.harbor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.harbor.domain.FlowerKind
import app.harbor.ui.theme.Leaf
import kotlinx.coroutines.delay

/**
 * The flower going into the ground.
 *
 * Planting used to end with a screen closing. You chose a flower, pressed
 * "Back to your day", and the picker vanished — and whether anything had
 * actually been added to the garden was something you had to go and check.
 * The reward was real and the app kept it a secret.
 *
 * So the bloom leaves the picker at the size it was, travels to where the
 * field sits on home, and shrinks into it, with a ring opening where it lands.
 * Nothing about the data changed; the plant was always recorded. This is the
 * app admitting to it.
 *
 * Drawn over home rather than in place of it, so the field is underneath the
 * whole way down and the flower is visibly going *into* something.
 *
 * ## Where it aims
 *
 * [FIELD_CENTRE] is a fraction of the screen rather than a measured position.
 * Handing the real coordinate down from home would mean home publishing its
 * layout upward through two screens purely so an animation could read it, and
 * the target only has to be close: what sells this is the shrink, not the
 * landing being pixel-accurate on the right blade of grass.
 */
private const val BLOOM_CENTRE = 0.46f
private const val FIELD_CENTRE = 0.30f

@Composable
fun FlowerLanding(
    kind: FlowerKind,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onLanded: () -> Unit,
) {
    val fall = remember { Animatable(0f) }

    LaunchedEffect(kind) {
        if (reducedMotion) {
            onLanded()
            return@LaunchedEffect
        }
        fall.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        // A beat with the ring still open, so the landing is seen rather than
        // merely completed.
        delay(260)
        onLanded()
    }

    if (reducedMotion) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        val t = fall.value
        val height = maxHeight
        val travel = height * (BLOOM_CENTRE - FIELD_CENTRE)

        // 200dp is the bloom's size on the picker, so it leaves at exactly the
        // size it arrived.
        val span = 200.dp - 156.dp * t

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = height * BLOOM_CENTRE - travel * t - span / 2f),
            contentAlignment = Alignment.Center,
        ) {
            // The ring opens as the flower lands, not before.
            if (t > 0.55f) {
                val ring = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                Canvas(Modifier.size(span * 1.6f).alpha((1f - ring) * 0.55f)) {
                    drawCircle(
                        color = Leaf,
                        radius = size.minDimension * 0.22f * (0.5f + ring),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 2.5f),
                    )
                }
            }
            FlowerMark(kind = kind, modifier = Modifier.size(span))
        }
    }
}
