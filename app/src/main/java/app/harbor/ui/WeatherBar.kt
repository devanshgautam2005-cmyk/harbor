package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.Weather
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Gold
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * How life feels, set the way you would read a sky.
 *
 * Hand-translated from `weather-bar.tsx` and its `.weather-*` rules: a rail,
 * a gradient fill from sky blue to gold, a stop for each weather, and a thumb
 * you can tap or slide.
 *
 * A slider rather than five buttons on purpose — this is a scale, not a set of
 * options, and "a bit worse than yesterday" is the thing someone actually
 * wants to say.
 */
@Composable
fun WeatherBar(store: HarborRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()
    val steps = Weather.entries
    val last = steps.size - 1
    val index = steps.indexOf(settings.weather).coerceAtLeast(0)

    var trackWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val inset = with(density) { 22.dp.toPx() }

    fun choose(next: Int) {
        val clamped = next.coerceIn(0, last)
        if (steps[clamped] != settings.weather) {
            scope.launch { store.setSettings(settings.copy(weather = steps[clamped])) }
        }
    }

    fun chooseFromX(x: Float) {
        val usable = trackWidth - inset * 2
        if (usable <= 0) return
        choose((((x - inset) / usable) * last).roundToInt())
    }

    Surface(modifier) {
        Eyebrow("How is life right now")
        Text(
            settings.weather.label,
            style = MaterialTheme.typography.titleLarge,
        )
        SmallCopy(settings.weather.caption)

        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .onSizeChanged { trackWidth = it.width }
                .pointerInput(last, trackWidth) {
                    detectTapGestures { chooseFromX(it.x) }
                }
                .pointerInput(last, trackWidth) {
                    detectHorizontalDragGestures { change, _ -> chooseFromX(change.position.x) }
                },
        ) {
            // the rail
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )

            // how far along the scale we are, sky through to gold
            val fraction = if (last == 0) 0f else index.toFloat() / last
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp)
                    .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF9DC6E8), Gold))),
            )

            // the thumb
            val thumbX = with(density) {
                (inset + (trackWidth - inset * 2) * fraction).toDp() - 21.dp
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbX)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { step ->
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

internal val Weather.label: String
    get() = when (this) {
        Weather.CLEAR -> "Clear"
        Weather.BRIGHT -> "Bright"
        Weather.CLOUDY -> "Cloudy"
        Weather.RAIN -> "Rain"
        Weather.STORM -> "Storm"
    }

internal val Weather.caption: String
    get() = when (this) {
        Weather.CLEAR -> "Room to breathe. Nothing pressing."
        Weather.BRIGHT -> "Good and busy. The kind you chose."
        Weather.CLOUDY -> "A little grey around the edges."
        Weather.RAIN -> "Heavy going. Steady, but heavy."
        Weather.STORM -> "Too much at once. This passes."
    }
