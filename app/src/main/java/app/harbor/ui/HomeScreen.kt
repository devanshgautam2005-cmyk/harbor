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
import app.harbor.domain.CallStats
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
