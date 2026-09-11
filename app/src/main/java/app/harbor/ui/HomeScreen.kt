package app.harbor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import app.harbor.domain.CallStats
import app.harbor.domain.DailyQuestion
import kotlinx.coroutines.launch
import java.time.LocalDate
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.Weather
import app.harbor.sensing.Sensing
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId

/**
 * Where the app opens.
 *
 * Deliberately quiet. Harbor's whole argument is that it should not be
 * something you check — the cue comes to you. A home screen that demanded
 * attention would be arguing against the product.
 *
 * So it answers three questions and stops: is Harbor listening, when did you
 * last reach someone, and what has grown.
 */
@Composable
fun HomeScreen(
    store: HarborRepository,
    onOpenGarden: () -> Unit,
    onOpenCues: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onReflect: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by store.settings.collectAsState()
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val who = contacts.firstOrNull()
    val listening = Sensing.isActive(context, store)

    val lastConnection = entries
        .filter { it.resolution.isConnection }
        .maxByOrNull { it.occurredAt }

    val flowersGrown = entries.count { it.flower != null }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("harbor", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.size(4.dp))
        Text(settings.weather.greeting, style = MaterialTheme.typography.headlineMedium)

        // A call Harbor watched you start but never heard about. Offered here
        // because nobody reopens an app the moment a call ends.
        CallStats.pendingReflection(entries, Instant.now())?.let { waiting ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How did that go?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "You called ${who?.label ?: "someone"} earlier. It only " +
                            "takes a moment, and it is what grows the flower.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = { onReflect(waiting) }) { Text("Tell me") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (listening) "Harbor is listening for a quiet moment."
                    else "Cues are off.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when {
                        !listening ->
                            "Nothing is being sensed. Turn cues on whenever you feel ready."
                        who == null ->
                            "There is nobody to call yet, so a cue has nothing to offer."
                        else ->
                            "After a walk of ${settings.thresholds.walkingMinutes} minutes or " +
                                "more, at most ${settings.thresholds.dailyCap} times a day."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenCues) {
                    Text(if (listening) "Cues and privacy" else "Turn on gentle cues")
                }
            }
        }

        if (who != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(who.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        lastConnection?.let { last ->
                            val days = java.time.Duration
                                .between(
                                    last.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate()
                                        .atStartOfDay(ZoneId.systemDefault()),
                                    Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
                                        .atStartOfDay(ZoneId.systemDefault()),
                                ).toDays()
                            when (days) {
                                0L -> "You reached them today."
                                1L -> "Yesterday."
                                else -> "$days days ago."
                            }
                        } ?: "You have not reached them through Harbor yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    CallStats.usualMinutes(entries, who.id)?.let { usual ->
                        Text(
                            "Your calls usually run about ${CallStats.formatDuration(usual)}.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        OutlinedButton(onClick = onOpenNotes, modifier = Modifier.fillMaxWidth()) {
            Text("Leave a line")
        }

        DailyQuestionCard(store)

        Button(onClick = onOpenGarden, modifier = Modifier.fillMaxWidth()) {
            Text(
                when (flowersGrown) {
                    0 -> "See your garden"
                    1 -> "See your garden · 1 flower"
                    else -> "See your garden · $flowersGrown flowers"
                },
            )
        }

        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Your pace")
        }
    }
}

/**
 * One small question a day.
 *
 * Reshaped, not ported. The prototype shows your family's answers beside
 * yours; there is no family side in this build (ADR-007), so inventing their
 * replies would be the one thing the prototype is careful never to do.
 *
 * What survives is a daily prompt worth sitting with, and something small to
 * bring to a call. Honest, and noticeably less than the prototype's version —
 * this is the screen ADR-007 costs the most.
 */
@Composable
private fun DailyQuestionCard(store: HarborRepository) {
    val scope = rememberCoroutineScope()
    val answers by store.dailyAnswers.collectAsState()
    val today = remember { LocalDate.now() }
    val question = remember(today) { DailyQuestion.forDay(today) }
    val answered = answers[today]
    var draft by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Today's little question", style = MaterialTheme.typography.labelMedium)
            Text(question, style = MaterialTheme.typography.titleMedium)

            if (answered != null) {
                Text(answered, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "No streak to keep. Answer today, skip tomorrow — either is fine.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(200) },
                    placeholder = { Text("Whatever comes to mind…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) scope.launch { store.setDailyAnswer(today, text) }
                    },
                    enabled = draft.isNotBlank(),
                ) { Text("Keep my answer") }
            }
        }
    }
}

/**
 * The weather sets the greeting rather than the time of day.
 *
 * How someone's week is going is a better predictor of whether a call is
 * welcome than whether it happens to be morning.
 */
private val Weather.greeting: String
    get() = when (this) {
        Weather.CLEAR -> "A clear stretch."
        Weather.BRIGHT -> "Busy, in the good way."
        Weather.CLOUDY -> "A little grey."
        Weather.RAIN -> "Heavy going lately."
        Weather.STORM -> "A lot at once."
    }
