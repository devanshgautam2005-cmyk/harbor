package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.Notice
import app.harbor.ui.theme.PageIntro
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import app.harbor.domain.BusyWindow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * When the user is not reachable.
 *
 * The data source the in-class suppression rule was built without (ADR-011).
 * Self-entered: no campus API to depend on, no calendar permission, and
 * nobody asked for their college password.
 *
 * Not a port of the prototype's Schedule, which is built around sharing
 * availability with a parent and a mutual-consent handshake. There is no
 * parent side here (ADR-007), so there is nobody to share with.
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

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.padding(horizontal = 28.dp)) {
            PageIntro(
                eyebrow = "Room for real life",
                title = "When you are busy.",
                subtitle = "Harbor stays quiet during these.",
            )
        }

        Flow(Modifier.pageContent()) {
            Notice(
                "Only the times. No subjects, no locations, and nothing leaves " +
                    "this phone.",
            )

            Surface {
                SectionHeading("Add a block")
                DayOfWeek.entries.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { candidate ->
                            Pill(
                                text = candidate.getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.getDefault(),
                                ),
                                selected = candidate == day,
                                modifier = Modifier.weight(1f),
                            ) { day = candidate }
                        }
                    }
                }

                Stepper(
                    label = "From",
                    value = hourLabel(startHour),
                    onDown = {
                        startHour = (startHour - 1).coerceAtLeast(0)
                        if (endHour <= startHour) endHour = (startHour + 1).coerceAtMost(23)
                    },
                    onUp = {
                        startHour = (startHour + 1).coerceAtMost(22)
                        if (endHour <= startHour) endHour = (startHour + 1).coerceAtMost(23)
                    },
                )
                Stepper(
                    label = "Until",
                    value = hourLabel(endHour),
                    onDown = { endHour = (endHour - 1).coerceAtLeast(startHour + 1) },
                    onUp = { endHour = (endHour + 1).coerceAtMost(23) },
                )

                TextLink("Add this block") {
                    if (endHour > startHour) {
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
                }
            }

            SectionHeading("Your week")
            if (windows.isEmpty()) {
                SmallCopy("Nothing yet. Without any blocks, a cue can land during a class.")
            }
            windows.groupBy { it.day }.toSortedMap().forEach { (weekday, blocks) ->
                Surface {
                    Eyebrow(weekday.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    blocks.forEach { block ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                hourLabel(block.start.hour) + " - " + hourLabel(block.end.hour),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Remove",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        scope.launch { store.setBusyWindows(windows - block) }
                                    }
                                    .padding(8.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }

            TextLink("Back", onDone)
        }
    }
}

private fun hourLabel(hour: Int): String {
    val display = if (hour % 12 == 0) 12 else hour % 12
    return display.toString() + if (hour < 12) "am" else "pm"
}
