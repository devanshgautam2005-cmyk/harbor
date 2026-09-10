package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import app.harbor.domain.FlowerKind
import app.harbor.domain.FlowerSpec
import app.harbor.domain.Flowers

/**
 * Draws a flower.
 *
 * Petal count, colours and shape all come from [FlowerSpec], which is ported
 * from the prototype's library — so a marigold here is the same marigold the
 * web shows. Shared by the bloom that follows a call and, later, by the
 * garden.
 *
 * Deliberately hand-drawn on a Canvas rather than shipped as vector assets.
 * The shape is a function of the spec, so adding a flower to the library is a
 * data change rather than an art request.
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

/** Shared by the composable above and by the garden's bulk drawing. */
internal fun DrawScope.drawFlower(spec: FlowerSpec, radius: Float) {
    val petalLength = radius * 0.92f
    val petalWidth = when (spec.shape) {
        FlowerSpec.Shape.ROUND -> radius * 0.46f
        FlowerSpec.Shape.CUP -> radius * 0.62f
        FlowerSpec.Shape.POINT -> radius * 0.30f
    }

    val petal = Color(spec.petal)
    val deep = Color(spec.petalDeep)

    for (i in 0 until spec.petals) {
        val angle = i * 360f / spec.petals
        rotate(degrees = angle, pivot = Offset.Zero) {
            // Alternate the two petal tones so a flower reads as having depth
            // without needing a gradient per petal.
            drawPath(
                path = petalPath(spec.shape, petalLength, petalWidth),
                color = if (i % 2 == 0) petal else deep,
            )
        }
    }

    drawCircle(
        color = Color(spec.heart),
        radius = radius * when (spec.shape) {
            FlowerSpec.Shape.ROUND -> 0.26f
            FlowerSpec.Shape.CUP -> 0.20f
            FlowerSpec.Shape.POINT -> 0.22f
        },
        center = Offset.Zero,
    )
}

private fun petalPath(shape: FlowerSpec.Shape, length: Float, width: Float): Path = Path().apply {
    when (shape) {
        // A rounded lobe: two symmetric curves meeting at the tip.
        FlowerSpec.Shape.ROUND -> {
            moveTo(0f, 0f)
            cubicTo(width, -length * 0.25f, width * 0.7f, -length, 0f, -length)
            cubicTo(-width * 0.7f, -length, -width, -length * 0.25f, 0f, 0f)
        }
        // Wider and blunter, like a poppy or tulip.
        FlowerSpec.Shape.CUP -> {
            moveTo(0f, 0f)
            cubicTo(width * 1.1f, -length * 0.15f, width * 0.9f, -length * 0.85f, 0f, -length)
            cubicTo(-width * 0.9f, -length * 0.85f, -width * 1.1f, -length * 0.15f, 0f, 0f)
        }
        // Narrow, straight-sided, ending in a point.
        FlowerSpec.Shape.POINT -> {
            moveTo(0f, 0f)
            lineTo(width, -length * 0.45f)
            lineTo(0f, -length)
            lineTo(-width, -length * 0.45f)
            close()
        }
    }
}
