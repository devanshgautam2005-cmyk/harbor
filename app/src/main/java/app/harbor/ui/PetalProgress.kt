package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * The onboarding's progress: a flower that gains a petal per answer.
 *
 * This is the spine of the new onboarding. Rather than a bar or a row of dots,
 * the questions build one flower — "answer the questions to add petals" — so
 * the thing being grown is the same thing the rest of the app grows. A step is
 * not progress through a form; it is a petal.
 *
 * Three states, as the design has them: a petal already earned is filled, the
 * one being answered carries a dark outline, and the rest wait pale.
 *
 * ## Why this is drawn and not five imported SVGs
 *
 * The design exports each petal as its own SVG at a fixed rotation. Harbor
 * draws every flower it has from a shape function instead (see [FlowerMark]),
 * which is what lets a bloom scale, animate and take a colour from data. Five
 * fixed images could do none of that, and the petal here has to fill as you
 * answer. The silhouette below is taken from the exported path: a teardrop
 * 88 wide by 104.5 long, with its point at the flower's centre and a round
 * lobe outward.
 */

/** Bright at the throat, deepening to the tip. Sampled from the design. */
private val PetalCore = Color(0xFF0A9AA0)
private val PetalEdge = Color(0xFF184B6A)

/** A petal not yet earned. */
private val PetalWaiting = Color(0xFFD0DEE3)
private val PetalWaitingEdge = Color(0xFFB6C7CE)

/** The one being answered. */
private val PetalCurrentEdge = Color(0xFF1A1A1A)

@Composable
fun PetalProgress(
    step: Int,
    modifier: Modifier = Modifier,
    total: Int = 5,
) {
    Canvas(modifier) {
        val length = minOf(size.width, size.height) / 2f * 0.92f
        translate(left = size.width / 2f, top = size.height / 2f) {
            for (i in 0 until total) {
                // Petal 0 points up, the rest follow clockwise.
                rotate(degrees = i * 360f / total, pivot = Offset.Zero) {
                    drawPetal(
                        length = length,
                        earned = i < step,
                        current = i == step,
                    )
                }
            }
        }
    }
}

/** One teardrop, point at the origin, lobe reaching [length] outward. */
private fun DrawScope.drawPetal(length: Float, earned: Boolean, current: Boolean) {
    val path = petalPath(length)

    if (earned) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(PetalCore, PetalEdge),
                center = Offset.Zero,
                radius = length,
            ),
        )
    } else {
        drawPath(path = path, color = PetalWaiting)
        drawPath(
            path = path,
            color = if (current) PetalCurrentEdge else PetalWaitingEdge,
            style = Stroke(width = length * (if (current) 0.022f else 0.014f)),
        )
    }
}

/**
 * The exported silhouette, as a curve rather than an image.
 *
 * The control points put the widest part at roughly six tenths of the way out
 * and about four tenths of the length across, which is where the SVG's lobe
 * sits. Drawn up the negative Y axis so a petal at rotation zero points at the
 * top of the screen.
 */
private fun petalPath(length: Float): Path {
    val w = length * 0.50f
    return Path().apply {
        moveTo(0f, 0f)
        // The first control point is pulled well in, so the petal leaves the
        // centre as a point rather than a wedge. That narrow base is what
        // separates one petal from the next and opens the small star at the
        // throat; carrying the full width all the way down merged all five
        // into a single blob.
        cubicTo(-w * 0.26f, -length * 0.30f, -w, -length * 0.74f, 0f, -length)
        cubicTo(w, -length * 0.74f, w * 0.26f, -length * 0.30f, 0f, 0f)
        close()
    }
}
