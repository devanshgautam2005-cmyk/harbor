package app.harbor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.CueSound
import app.harbor.domain.Thresholds
import app.harbor.domain.UserSettings
import app.harbor.domain.Weather
import kotlinx.coroutines.launch

/**
 * Everything the user is allowed to change, which is deliberately everything
 * that decides when Harbor speaks.
 *
 * The numbers here are the ones the study measures drift against. They ship as
 * a suggestion and are never locked — that is a guardrail from the design
 * audit, not a preference.
 */
@Composable
fun SettingsScreen(
    store: HarborRepository,
    onEditSchedule: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()

    fun save(next: UserSettings) = scope.launch { store.setSettings(next) }
    fun thresholds(next: Thresholds) = save(settings.copy(thresholds = next))

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Your pace", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Suggestions, not rules. Move them until Harbor fits your week.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Stepper(
                    label = "Walk before a cue",
                    value = "${settings.thresholds.walkingMinutes} min",
                    onDown = {
                        thresholds(
                            settings.thresholds.copy(
                                walkingMinutes = (settings.thresholds.walkingMinutes - 1)
                                    .coerceAtLeast(1),
                            ),
                        )
                    },
                    onUp = {
                        thresholds(
                            settings.thresholds.copy(
                                walkingMinutes = (settings.thresholds.walkingMinutes + 1)
                                    .coerceAtMost(120),
                            ),
                        )
                    },
                )
                Stepper(
                    label = "Most cues a day",
                    value = "${settings.thresholds.dailyCap}",
                    onDown = {
                        thresholds(
                            settings.thresholds.copy(
                                dailyCap = (settings.thresholds.dailyCap - 1).coerceAtLeast(1),
                            ),
                        )
                    },
                    onUp = {
                        thresholds(
                            settings.thresholds.copy(
                                dailyCap = (settings.thresholds.dailyCap + 1).coerceAtMost(10),
                            ),
                        )
                    },
                )
                Stepper(
                    label = "Quiet between cues",
                    value = "${settings.thresholds.cooldownMinutes} min",
                    onDown = {
                        thresholds(
                            settings.thresholds.copy(
                                cooldownMinutes = (settings.thresholds.cooldownMinutes - 30)
                                    .coerceAtLeast(1),
                            ),
                        )
                    },
                    onUp = {
                        thresholds(
                            settings.thresholds.copy(
                                cooldownMinutes = (settings.thresholds.cooldownMinutes + 30)
                                    .coerceAtMost(1440),
                            ),
                        )
                    },
                )
            }
        }

        OutlinedButton(onClick = onEditSchedule, modifier = Modifier.fillMaxWidth()) {
            Text("When you're busy")
        }

        // --- weather ------------------------------------------------------
        Text("How is life right now?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Weather, not a rating. It happens to you, and it passes.",
            style = MaterialTheme.typography.bodySmall,
        )
        Weather.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    OutlinedButton(
                        onClick = { save(settings.copy(weather = option)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (option == settings.weather) "· ${option.label}" else option.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        Text(settings.weather.caption, style = MaterialTheme.typography.bodySmall)

        // --- sound --------------------------------------------------------
        Text("Default sound", style = MaterialTheme.typography.titleMedium)
        Text(
            "A person you have chosen a ringtone for overrides this.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CueSound.entries.forEach { option ->
                OutlinedButton(
                    onClick = { save(settings.copy(sound = option)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (option == settings.sound) "· ${option.label}" else option.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("A little less movement", style = MaterialTheme.typography.titleSmall)
                Text("Reduce animation.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = settings.reducedMotion,
                onCheckedChange = { save(settings.copy(reducedMotion = it)) },
            )
        }

        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onDone) { Text("Back") }
    }
}

@Composable
private fun Stepper(label: String, value: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDown) { Text("−") }
            Spacer(Modifier.size(10.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(10.dp))
            OutlinedButton(onClick = onUp) { Text("+") }
        }
    }
}

private val Weather.label: String
    get() = when (this) {
        Weather.CLEAR -> "Clear"
        Weather.BRIGHT -> "Bright"
        Weather.CLOUDY -> "Cloudy"
        Weather.RAIN -> "Rain"
        Weather.STORM -> "Storm"
    }

private val Weather.caption: String
    get() = when (this) {
        Weather.CLEAR -> "Room to breathe. Nothing pressing."
        Weather.BRIGHT -> "Good and busy. The kind you chose."
        Weather.CLOUDY -> "A little grey around the edges."
        Weather.RAIN -> "Heavy going. Steady, but heavy."
        Weather.STORM -> "Too much at once. This passes."
    }

private val CueSound.label: String
    get() = when (this) {
        CueSound.CHIME -> "Chime"
        CueSound.SOFT -> "Soft"
        CueSound.SILENT -> "Silent"
    }
