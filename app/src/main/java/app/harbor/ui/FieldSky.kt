package app.harbor.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import app.harbor.domain.Weather

/**
 * The sky the field sits under.
 *
 * Ported from the prototype's `.field-frame[data-weather]` rules: the ground
 * gradient, a sun, three clouds on slow loops, rain, and an overall dimming
 * that gets heavier as the weather does.
 *
 * This is the one place in Harbor where the weather the user set is more than
 * a label — a storm actually makes the garden darker. That is the whole point
 * of asking: the app takes their word for how life is and reflects it back,
 * rather than filing it away for a chart nobody sees.
 */
@Composable
fun FieldSky(weather: Weather, modifier: Modifier = Modifier) {
    val clock = rememberInfiniteTransition(label = "sky")
    val slow by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "drift",
    )
    val fast by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760, easing = LinearEasing)),
        label = "rain",
    )

    Canvas(modifier.fillMaxSize()) {
        val sky = fieldTintOf(weather)
        drawRect(
            brush = Brush.verticalGradient(listOf(sky.top, sky.bottom)),
            size = size,
        )

        if (sky.sun > 0f) drawFieldSun(sky.sun)

        // Three clouds at different widths and speeds, so the loop never
        // reads as a loop.
        if (sky.cloud > 0f) {
            drawFieldCloud(88.dp.toPx(), 16.dp.toPx(), loopPhase(slow, 60_000f, 36_000f, 0f), sky)
            if (weather != Weather.BRIGHT) {
                drawFieldCloud(66.dp.toPx(), 44.dp.toPx(), loopPhase(slow, 60_000f, 48_000f, 0.19f), sky)
                drawFieldCloud(112.dp.toPx(), 28.dp.toPx(), loopPhase(slow, 60_000f, 60_000f, 0.40f), sky)
            }
        }

        if (sky.rain > 0f) drawFieldRain(fast, sky.rain)

        if (sky.dim > 0f) {
            drawRect(color = Color(0xFF1E2C3C).copy(alpha = sky.dim), size = size)
        }
    }
}

/** Where a looping thing is, given a shared clock and its own period. */
private fun loopPhase(t: Float, clockMs: Float, periodMs: Float, offset: Float): Float {
    val turns = t * clockMs / periodMs + offset
    return turns - turns.toInt()
}

private fun DrawScope.drawFieldSun(alpha: Float) {
    val r = 75.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD678).copy(alpha = 0.95f * alpha),
                Color(0xFFFFD678).copy(alpha = 0f),
            ),
            center = Offset(size.width / 2, -44.dp.toPx() + r),
            radius = r,
        ),
        radius = r,
        center = Offset(size.width / 2, -44.dp.toPx() + r),
    )
}

private fun DrawScope.drawFieldCloud(width: Float, top: Float, phase: Float, sky: SkyTint) {
    val travel = size.width + 260.dp.toPx()
    val x = -130.dp.toPx() + travel * phase
    val h = 22.dp.toPx()
    val colour = sky.cloudColour.copy(alpha = sky.cloud)

    drawRoundRect(
        color = colour,
        topLeft = Offset(x, top),
        size = Size(width, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2),
    )
    drawCircle(colour, radius = 14.dp.toPx(), center = Offset(x + width * 0.16f + 14.dp.toPx(), top + h - 12.dp.toPx()))
    drawCircle(colour, radius = 10.dp.toPx(), center = Offset(x + width * 0.78f, top + h - 8.dp.toPx()))
}

private fun DrawScope.drawFieldRain(phase: Float, alpha: Float) {
    val drop = 16.dp.toPx()
    val fall = size.height + drop * 2
    for (i in 0 until 26) {
        // Deterministic scatter: same drops every frame, no per-frame random.
        val lane = ((i * 3.9f + (i % 5) * 1.7f) % 100f) / 100f
        val stagger = ((i % 9) * 0.13f + (i % 4) * 0.07f)
        val p = (phase + stagger).let { it - it.toInt() }
        val y = -drop + fall * p
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7896AF).copy(alpha = 0f),
                    Color(0xFF6888A4).copy(alpha = 0.8f * alpha),
                ),
                startY = y,
                endY = y + drop,
            ),
            start = Offset(size.width * lane, y),
            end = Offset(size.width * lane, y + drop),
            strokeWidth = 1.6.dp.toPx(),
        )
    }
}

private class SkyTint(
    val top: Color,
    val bottom: Color,
    val sun: Float,
    val cloud: Float,
    val cloudColour: Color,
    val rain: Float,
    val dim: Float,
)

private fun fieldTintOf(weather: Weather): SkyTint = when (weather) {
    // Dusk, in five weathers.
    //
    // The design draws the field as one gradient -- cool blue overhead falling
    // through ember to a near-black floor -- and these are that gradient bent
    // five ways. Weather still has to be legible at a glance, which is the
    // whole point of it, so what separates them is the same thing it always
    // was: how warm the bottom is and how much of the sky the cloud takes. A
    // bright evening keeps its ember; a storm loses it and goes blue-grey.
    //
    // Nothing here is allowed to get light. The field sits directly on the
    // app's ground with no card around it, so a pale sky would read as a hole
    // cut in the page rather than as weather.
    Weather.CLEAR -> SkyTint(
        Color(0xFF2B4F6B), Color(0xFF3A2018),
        sun = 0.55f, cloud = 0f, cloudColour = Color(0xFFE8D6A8), rain = 0f, dim = 0f,
    )
    Weather.BRIGHT -> SkyTint(
        Color(0xFF2F5A7D), Color(0xFF7B4226),
        sun = 1f, cloud = 0.5f, cloudColour = Color(0xFFF0C894), rain = 0f, dim = 0f,
    )
    Weather.CLOUDY -> SkyTint(
        Color(0xFF2A3F52), Color(0xFF33262A),
        sun = 0f, cloud = 0.92f, cloudColour = Color(0xFF8FA0AC), rain = 0f, dim = 0.07f,
    )
    Weather.RAIN -> SkyTint(
        Color(0xFF24374A), Color(0xFF262126),
        sun = 0f, cloud = 0.9f, cloudColour = Color(0xFF7E8A94), rain = 0.7f, dim = 0.13f,
    )
    Weather.STORM -> SkyTint(
        Color(0xFF1D2C3C), Color(0xFF1E1B20),
        sun = 0f, cloud = 0.92f, cloudColour = Color(0xFF6B747D), rain = 1f, dim = 0.24f,
    )
}

