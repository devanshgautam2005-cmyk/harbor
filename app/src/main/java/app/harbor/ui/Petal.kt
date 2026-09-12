package app.harbor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * The petal: Harbor's mark for sending something small.
 *
 * Leaving a line and sending a picture used to be two actions with two
 * borrowed icons — a speech bubble and a camera — which said *messaging* and
 * *photos*, two things this app is not. They are one act, and it is the
 * smallest act in the product: a petal off the flower, sent, nothing owed in
 * return. So one name, one mark, and the mark is the app's own.
 *
 * The silhouette is [petalPath], the same curve the onboarding's progress
 * flower is built from, so a petal here and a petal there are the same object.
 */

private val PetalWarm = Color(0xFFE9A15C)
private val PetalBlush = Color(0xFFD9604A)

/** One petal, tilted, filling whatever the caller sizes it to. */
@Composable
internal fun PetalMark(
    modifier: Modifier = Modifier,
    core: Color = PetalWarm,
    edge: Color = PetalBlush,
) {
    Canvas(modifier) {
        val length = size.minDimension * 0.82f
        translate(left = size.width / 2f, top = size.height * 0.86f) {
            rotate(degrees = -18f, pivot = Offset.Zero) {
                drawPath(
                    path = petalPath(length),
                    brush = Brush.radialGradient(
                        colors = listOf(core, edge),
                        center = Offset.Zero,
                        radius = length,
                    ),
                )
            }
        }
    }
}

/**
 * A petal leaving.
 *
 * What a send is worth looking at for. The line itself goes to whatever app
 * the user picked — Harbor never sends anything — so the moment the chooser
 * closes there is nothing on screen to say the thing happened. A row of text
 * saying "sent" is a receipt. This is the same information as a small event.
 *
 * It lifts, drifts, turns over once and fades. [onFinished] fires when there
 * is nothing left to see, so the caller can put its ordinary content back.
 *
 * Honours the reduced-motion setting by finishing immediately: somebody who
 * has asked for less movement should not be made to watch this.
 */
@Composable
internal fun PetalAway(
    label: String,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onFinished: () -> Unit,
) {
    val flight = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            // Still say it happened; just do not perform it.
            delay(700)
            onFinished()
            return@LaunchedEffect
        }
        flight.animateTo(1f, tween(durationMillis = 1150, easing = LinearOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        val t = flight.value
        if (!reducedMotion) {
            Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                val length = 34.dp.toPx()
                // Up and to the right, with a little sway across the rise —
                // the way something light actually leaves.
                val x = size.width * 0.5f + size.width * 0.26f * t +
                    sin(t * 7f) * length * 0.22f
                val y = size.height * 0.78f - size.height * 0.62f * t
                translate(left = x, top = y) {
                    rotate(degrees = -18f + 220f * t, pivot = Offset.Zero) {
                        drawPath(
                            path = petalPath(length * (1f - 0.25f * t)),
                            brush = Brush.radialGradient(
                                colors = listOf(PetalWarm, PetalBlush),
                                center = Offset.Zero,
                                radius = length,
                            ),
                            alpha = (1f - t * t).coerceIn(0f, 1f),
                        )
                    }
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/** The petal as a plain glyph inside an existing canvas. */
internal fun DrawScope.drawPetalGlyph(ink: Color) {
    val length = size.minDimension * 0.84f
    translate(left = size.width / 2f, top = size.height * 0.88f) {
        rotate(degrees = -18f, pivot = Offset.Zero) {
            drawPath(path = petalPath(length), color = ink)
        }
    }
}
