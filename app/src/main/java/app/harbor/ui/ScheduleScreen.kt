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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.BusyWindow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * When the user is not reachable — classes, labs, shifts.
 *
 * This is the data source the in-class suppression rule was built without
 * (ADR-011). Self-entered on purpose: DigiCampus publishes no API anyone could
 * find, reading the device calendar would cost a permission, and scraping a
 * campus portal would mean asking a study participant for their college
 * password. A student typing their week once costs none of that.
 *
 * Deliberately **not** a port of the prototype's Schedule screen. That one is
 * built around sharing availability with a parent and a mutual-consent
 * handshake; there is no parent-side anything in this build (ADR-007), so
 * there is nobody to share with and no consent to collect.
 */
@Composable
fun ScheduleScreen(
    store: HarborRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val windows by store.busyWindows.collectAsState()

    var day by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var startHour by remember { mutableStateOf(9) }
    var endHour by remember { mutableStateOf(10) }

    fun add() {
        if (endHour <= startHour) return
        scope.launch {
            store.setBusyWindows(
                windows + BusyWindow(
                    day = day,
                    start = LocalTime.of(startHour, 0),
                    end = LocalTime.of(endHour, 0),
                ),
            )
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("When you're busy", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Harbor stays quiet during these. Walking between buildings and " +
                "stopping outside a lecture hall is exactly the moment it would " +
                "otherwise catch — and exactly the wrong one.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Only the times. No subjects, no locations, and nothing leaves this " +
                "phone.",
            style = MaterialTheme.typography.bodySmall,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add a block", style = MaterialTheme.typography.titleMedium)

                // A week is seven buttons. A day picker dialog would be more
                // taps for less clarity.
                DayOfWeek.entries.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { candidate ->
                            OutlinedButton(
                                onClick = { day = candidate },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                            ) {
                                Text(
                                    if (candidate == day) "· ${candidate.short}" else candidate.short,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }

                HourRow("From", startHour) { hour ->
                    startHour = hour
                    if (endHour <= hour) endHour = (hour + 1).coerceAtMost(23)
                }
                HourRow("Until", endHour) { hour ->
                    endHour = hour.coerceAtLeast(startHour + 1)
                }

                Button(onClick = ::add, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Add ${day.getDisplayName(TextStyle.FULL, Locale.getDefault())} " +
                            "${hourLabel(startHour)}–${hourLabel(endHour)}",
                    )
                }
            }
        }

        if (windows.isEmpty()) {
            Text(
                "Nothing yet. Without any blocks, cues can land during a class.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text("Your week", style = MaterialTheme.typography.titleMedium)
            windows.groupBy { it.day }.toSortedMap().forEach { (weekday, blocks) ->
                Text(
                    weekday.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                )
                blocks.forEach { block ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${hourLabel(block.start.hour)} – ${hourLabel(block.end.hour)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        TextButton(onClick = {
                            scope.launch { store.setBusyWindows(windows - block) }
                        }) { Text("Remove") }
                    }
                }
            }
        }

        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onDone) { Text("Back") }
    }
}

@Composable
private fun HourRow(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange((hour - 1).coerceAtLeast(0)) }) { Text("−") }
            Spacer(Modifier.size(12.dp))
            Text(hourLabel(hour), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(12.dp))
            OutlinedButton(onClick = { onChange((hour + 1).coerceAtMost(23)) }) { Text("+") }
        }
    }
}

private fun hourLabel(hour: Int): String {
    val display = if (hour % 12 == 0) 12 else hour % 12
    return "$display${if (hour < 12) "am" else "pm"}"
}

private val DayOfWeek.short: String
    get() = getDisplayName(TextStyle.SHORT, Locale.getDefault())
