package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import app.harbor.domain.FlowerKind
import app.harbor.domain.FlowerSpec
import app.harbor.domain.Flowers

/**
 * Draws a flower, the way the specimen sheet draws one.
 *
 * Petal count, colours and shape all come from [FlowerSpec], which is ported
 * from the prototype's library — so a marigold here is the same marigold the
 * web shows. Shared by the bloom that follows a call, by a person's specimen
 * on home, and by the picker.
 *
 * ## Three things make it look like the sheet
 *
 * The sheet's flowers are not flat shapes. Each petal is an **ellipse with a
 * vertical gradient**, light at the tip and deep at the throat; every petal is
 * drawn at **80% opacity** and composited with **multiply**; and the petals
 * are anchored at the flower's centre so they overlap near it. Where two
 * petals cross, multiply darkens the overlap, and that darkening is the whole
 * of the depth — there is no shading, no shadow and no outline anywhere.
 *
 * Drawn rather than shipped as art, because the shape is a function of the
 * spec: adding a flower to the library stays a data change, not an art
 * request. The field draws its own much cheaper version of this for the
 * top-down view, where there can be hundreds on screen at once.
 */
@Composable
fun FlowerMark(
    kind: FlowerKind,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val spec = Flowers.spec(kind)
    Canvas(modifier) {
        val radius = minOf(size.width, size.height) / 2f * 0.9f * scale
        translate(left = size.width / 2f, top = size.height / 2f) {
            drawFlower(spec, radius)
        }
    }
}

/** How far a petal reaches, how wide it is, and where it sits on the stem. */
private data class Petal(val rx: Float, val ry: Float, val cy: Float)

private fun petalOf(shape: FlowerSpec.Shape, radius: Float): Petal = when (shape) {
    // The sheet's default: a long lobe, reaching a full radius at the tip.
    FlowerSpec.Shape.ROUND -> Petal(radius * 0.38f, radius * 0.60f, -radius * 0.40f)
    // Wider and blunter, sitting lower, like a poppy or a tulip.
    FlowerSpec.Shape.CUP -> Petal(radius * 0.50f, radius * 0.52f, -radius * 0.34f)
    // Narrow and reaching, drawn as a spear rather than an ellipse.
    FlowerSpec.Shape.POINT -> Petal(radius * 0.24f, radius * 0.62f, -radius * 0.40f)
}

/**
 * Shared by the composable above, by the specimen arch, and by the bloom.
 *
 * Draws from the origin, so callers translate to wherever the flower belongs.
 */
internal fun DrawScope.drawFlower(spec: FlowerSpec, radius: Float) {
    val light = Color(spec.petal)
    val deep = Color(spec.petalDeep)
    val p = petalOf(spec.shape, radius)
    val top = p.cy - p.ry
    val foot = p.cy + p.ry

    for (i in 0 until spec.petals) {
        rotate(degrees = i * 360f / spec.petals, pivot = Offset.Zero) {
            // Light at the tip, deep at the throat. The gradient turns with
            // the petal because it is built inside the rotation.
            val brush = Brush.verticalGradient(
                colors = listOf(light, deep),
                startY = top,
                endY = foot,
            )
            if (spec.shape == FlowerSpec.Shape.POINT) {
                drawPath(
                    path = spearPath(p),
                    brush = brush,
                    alpha = PETAL_ALPHA,
                    blendMode = PETAL_BLEND,
                )
            } else {
                drawOval(
                    brush = brush,
                    topLeft = Offset(-p.rx, top),
                    size = Size(p.rx * 2f, p.ry * 2f),
                    alpha = PETAL_ALPHA,
                    blendMode = PETAL_BLEND,
                )
            }
        }
    }

    // The throat. Softer than the petals rather than a hard dot: in the sheet
    // the centre is where the flower is darkest, not where it is sharpest.
    drawCircle(
        color = Color(spec.heart),
        radius = radius * heartOf(spec.shape),
        center = Offset.Zero,
        alpha = 0.8f,
    )
}

/** Petals are laid at 80%, so where they cross they darken. */
private const val PETAL_ALPHA = 0.92f

/**
 * How one petal sits on the next.
 *
 * Multiply, which is what this was, is the right answer on paper: overlapping
 * translucent petals get *darker* where they cross, the way pigment does, and
 * on the light specimen's bone page that is exactly what the sheet shows.
 *
 * On a near-black ground it is catastrophic and quiet about it. Multiplying
 * anything by a near-black backdrop gives near-black, so every bloom in the
 * app -- the picker, a person's specimen, the patch, the field -- came out a
 * dim smudge with a faint rim, and nothing about it looked broken enough to
 * read as a bug rather than as a small flower.
 *
 * Screen is multiply's opposite: overlaps get lighter. The petals stop being
 * pigment and start being light, which is what a flower has to be when the
 * page behind it is the night.
 */
private val PETAL_BLEND = BlendMode.Screen

private fun heartOf(shape: FlowerSpec.Shape): Float = when (shape) {
    FlowerSpec.Shape.ROUND -> 0.22f
    FlowerSpec.Shape.CUP -> 0.17f
    FlowerSpec.Shape.POINT -> 0.19f
}

/** A spear petal: straight sides, a blunt shoulder, a point at the tip. */
private fun spearPath(p: Petal): Path = Path().apply {
    val tip = p.cy - p.ry
    val foot = p.cy + p.ry
    val shoulder = p.cy - p.ry * 0.25f
    moveTo(0f, foot)
    lineTo(-p.rx, shoulder)
    lineTo(0f, tip)
    lineTo(p.rx, shoulder)
    close()
}
