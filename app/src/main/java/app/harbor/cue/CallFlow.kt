package app.harbor.cue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.harbor.domain.CallStats
import app.harbor.domain.Feeling
import app.harbor.domain.FlowerKind
import app.harbor.domain.Flowers
import app.harbor.ui.FlowerMark

/**
 * What a call leaves behind.
 *
 * Three steps after the dialer hands control back: how it felt, which flower
 * it becomes, and the bloom itself. Ported from `components/harbor/call.tsx`.
 *
 * The reflection is the reward. It is asked once, it is optional in substance
 * — every answer grows something — and nothing here can be failed. That is the
 * point: the garden records that calls happened, it does not score them.
 */
@Composable
fun CallFlow(
    who: String,
    /** Measured from handing off to the dialer until the user came back. */
    measuredMinutes: Int,
    initialTopic: String?,
    onPlant: (minutes: Int, feeling: Feeling, flower: FlowerKind, topic: String?) -> Unit,
    onDone: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.Reflect) }
    var minutes by remember { mutableStateOf(measuredMinutes.coerceIn(1, 180)) }
    var feeling by remember { mutableStateOf(Feeling.STEADY) }
    var flower by remember { mutableStateOf(Feeling.STEADY.flower) }
    var about by remember { mutableStateOf(initialTopic.orEmpty()) }
    var wholeLibrary by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            Step.Reflect -> {
                Text("Just for you", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(8.dp))
                Text("How did that feel?", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(20.dp))

                Feeling.entries.forEach { option ->
                    val selected = option == feeling
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                feeling = option
                                // Changing how it felt re-picks the flower, so
                                // the suggestion always matches the answer.
                                flower = option.flower
                            }
                            .padding(14.dp),
                    ) {
                        Text(option.label, style = MaterialTheme.typography.titleMedium)
                        Text(option.caption, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.size(10.dp))
                }

                Spacer(Modifier.size(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("About how long?", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Coarser steps once a call is long enough that a
                        // single minute stops being a meaningful difference.
                        OutlinedButton(onClick = {
                            minutes = (minutes - if (minutes > 15) 5 else 1).coerceAtLeast(1)
                        }) { Text("−") }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            CallStats.formatDuration(minutes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.size(12.dp))
                        OutlinedButton(onClick = {
                            minutes = (minutes + if (minutes >= 15) 5 else 1).coerceAtMost(180)
                        }) { Text("+") }
                    }
                }

                Spacer(Modifier.size(16.dp))
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it.take(90) },
                    label = { Text("What was it about? (optional)") },
                    placeholder = { Text("the tomatoes, mostly") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.size(20.dp))
                Button(onClick = { step = Step.Flower }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a flower")
                }
            }

            Step.Flower -> {
                Text("$who's patch", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(8.dp))
                Text("Which flower was it?", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (wholeLibrary) "The whole library. Pick whatever fits."
                    else "Picked for a ${feeling.label.lowercase()} call — or open the library.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(20.dp))

                val offered =
                    if (wholeLibrary) FlowerKind.entries else Flowers.suggestions(feeling)

                offered.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { kind ->
                            val selected = kind == flower
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .clickable { flower = kind }
                                    .padding(8.dp),
                            ) {
                                FlowerMark(kind, Modifier.size(56.dp))
                                Text(
                                    Flowers.spec(kind).name,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                }

                Spacer(Modifier.size(4.dp))
                Text(
                    Flowers.spec(flower).note,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.size(12.dp))
                TextButton(onClick = { wholeLibrary = !wholeLibrary }) {
                    Text(
                        if (wholeLibrary) "Back to the suggestions"
                        else "Open the flower library",
                    )
                }

                Button(
                    onClick = {
                        onPlant(minutes, feeling, flower, about.trim().ifEmpty { null })
                        step = Step.Bloom
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Plant ${Flowers.spec(flower).name.lowercase()}")
                }
            }

            Step.Bloom -> {
                Text(
                    "${CallStats.formatDuration(minutes)} together",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.size(24.dp))

                // A longer call opens a fuller bloom, bounded at both ends so
                // a short call is still a whole flower.
                FlowerMark(
                    kind = flower,
                    modifier = Modifier.size(200.dp),
                    scale = Flowers.bloomScale(minutes).toFloat(),
                )

                Spacer(Modifier.size(24.dp))
                Text("It opened.", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(8.dp))
                Text(
                    "A longer call opens a fuller bloom. This one is planted in " +
                        "$who's patch, and it stays there.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.size(28.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to your day")
                }
            }
        }
    }
}

private enum class Step { Reflect, Flower, Bloom }

private val Feeling.label: String
    get() = when (this) {
        Feeling.LIGHT -> "Lighter"
        Feeling.WARM -> "Warm"
        Feeling.STEADY -> "Steady"
        Feeling.TENDER -> "Tender"
    }

private val Feeling.caption: String
    get() = when (this) {
        Feeling.LIGHT -> "Something lifted."
        Feeling.WARM -> "Glad you picked up."
        Feeling.STEADY -> "Ordinary, in a good way."
        Feeling.TENDER -> "A lot, but worth it."
    }
