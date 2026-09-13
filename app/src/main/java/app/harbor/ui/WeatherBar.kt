package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import app.harbor.domain.DailyQuestion
import app.harbor.domain.Moment
import app.harbor.domain.Weather
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Gold
import app.harbor.ui.theme.Hairline
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import kotlinx.coroutines.launch
import java.time.LocalDate
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

    val answers by store.dailyAnswers.collectAsState()
    val today = remember { LocalDate.now() }
    val question = remember(today) { DailyQuestion.forDay(today) }
    val answered = answers[today]
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    // The one-word question is not on the page until the mood has been set.
    //
    // It used to hold its own row whether or not anyone was going to answer
    // it, and this card sits above the thing people open Harbor to do. Naming
    // how the day feels is the follow-on thought to setting the weather, so it
    // arrives when that thought does - as an extension of this card, never a
    // popup. Already answered today counts as having asked.
    var moodSet by remember { mutableStateOf(false) }

    var trackWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val inset = with(density) { 22.dp.toPx() }

    fun choose(next: Int) {
        val clamped = next.coerceIn(0, last)
        moodSet = true
        if (steps[clamped] != settings.weather) {
            scope.launch {
                store.setSettings(settings.copy(weather = steps[clamped]))
                store.note(Moment.WEATHER_SET, steps[clamped].name.lowercase())
            }
        }
    }

    fun chooseFromX(x: Float) {
        val usable = trackWidth - inset * 2
        if (usable <= 0) return
        choose((((x - inset) / usable) * last).roundToInt())
    }

    Surface(modifier) {
        // Label and answer on one line rather than three stacked.
        //
        // This card sits between the field and the call button now, so every
        // row it takes is a row of somebody's people pushed off the screen.
        // The caption under the weather word was the first to go: it said
        // "Room to breathe. Nothing pressing." under the word "Clear", which
        // is the same thought twice.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("How is life right now")
            Text(
                settings.weather.label,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
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

            // A notch per weather.
            //
            // The five words used to run underneath, which cost a whole row.
            // Without them the rail came out as a grey bar with a dot resting
            // at one end of it — on a clear day, indistinguishable from a
            // control that was broken or switched off. These say the same
            // thing (there are five of these, you are at this one) and cost
            // no height at all.
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                steps.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= index) MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.3f),
                            ),
                    )
                }
            }

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
                (inset + (trackWidth - inset * 2) * fraction).toDp() - 17.dp
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbX)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    // The card is nearly the same white as the thumb, so
                    // without a rim the thumb reads as a bare dot floating on
                    // the rail rather than as something you can take hold of.
                    .border(1.dp, Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        // The five step labels used to run under the rail, repeating the word
        // already set in large type directly above it. The rail's shape says
        // where you are on the scale; the labels only cost a row.

        // One word about today, folded into the sky rather than asked again.
        //
        // It used to be its own card directly below this one, which meant the
        // page asked how life was and then asked how today felt -- the same
        // question twice, a thumb-scroll apart. Setting the weather and
        // naming the day are one thought, so they are one card.
        if (moodSet || answered != null) Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .clickable { if (answered == null) expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            when {
                answered != null -> Column {
                    Eyebrow(question)
                    Text(
                        answered,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                expanded -> Column {
                    Eyebrow(question)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(40) },
                        placeholder = { Text("one word") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Pill(text = "Keep it", selected = draft.isNotBlank()) {
                        val word = draft.trim()
                        if (word.isNotEmpty()) {
                            scope.launch {
                                store.setDailyAnswer(today, word)
                                // That they answered, never the word.
                                store.note(Moment.ANSWER_KEPT)
                            }
                            expanded = false
                        }
                    }
                }

                else -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallCopy(question, size = 13)
                    Text(
                        "+",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
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
