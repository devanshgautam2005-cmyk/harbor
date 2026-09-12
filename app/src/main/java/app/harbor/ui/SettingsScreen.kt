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
import androidx.compose.material3.SwitchDefaults
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
import app.harbor.ui.theme.Hairline
import app.harbor.ui.theme.Leaf
import app.harbor.ui.theme.Notice
import app.harbor.ui.theme.PageIntro
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import app.harbor.domain.CueSound
import app.harbor.domain.Thresholds
import app.harbor.domain.UserSettings
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.launch

/**
 * Everything the user is allowed to change, which is deliberately everything
 * that decides when Harbor speaks.
 *
 * The numbers here ship as a suggestion and are never locked. That is a
 * guardrail from the design audit, not a preference — and they are what the
 * study measures drift against.
 */
@Composable
fun SettingsScreen(
    store: HarborRepository,
    onEditSchedule: () -> Unit,
    onOpenCues: () -> Unit,
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.padding(horizontal = 28.dp)) {
            PageIntro(
                eyebrow = "Always on your terms",
                title = "Your pace.",
                subtitle = "Suggestions, not rules. Move them until Harbor fits your week.",
            )
        }

        Flow(Modifier.pageContent()) {
            Surface {
                SectionHeading("What you call yourself")
                OutlinedTextField(
                    value = settings.name,
                    onValueChange = { save(settings.copy(name = it.take(40))) },
                    placeholder = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SmallCopy("Only used to say hello. It never leaves this phone.")
            }

            Surface {
                SectionHeading("When a cue can come")
                Stepper(
                    label = "Walk before a cue",
                    value = settings.thresholds.walkingMinutes.toString() + " min",
                    onDown = {
                        thresholds(
                            settings.thresholds.copy(
                                walkingMinutes =
                                    (settings.thresholds.walkingMinutes - 1).coerceAtLeast(1),
                            ),
                        )
                    },
                    onUp = {
                        thresholds(
                            settings.thresholds.copy(
                                walkingMinutes =
                                    (settings.thresholds.walkingMinutes + 1).coerceAtMost(120),
                            ),
                        )
                    },
                )
                Stepper(
                    label = "Most cues a day",
                    value = settings.thresholds.dailyCap.toString(),
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
                    value = settings.thresholds.cooldownMinutes.toString() + " min",
                    onDown = {
                        thresholds(
                            settings.thresholds.copy(
                                cooldownMinutes =
                                    (settings.thresholds.cooldownMinutes - 30).coerceAtLeast(1),
                            ),
                        )
                    },
                    onUp = {
                        thresholds(
                            settings.thresholds.copy(
                                cooldownMinutes =
                                    (settings.thresholds.cooldownMinutes + 30)
                                        .coerceAtMost(1440),
                            ),
                        )
                    },
                )
                SmallCopy("Suggested values, always editable.")
            }

            Surface {
                SectionHeading("Your gentle sound")
                SmallCopy("Someone you have chosen a ringtone for overrides this.")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CueSound.entries.forEach { option ->
                        Pill(
                            text = option.label,
                            selected = option == settings.sound,
                            modifier = Modifier.weight(1f),
                        ) { save(settings.copy(sound = option)) }
                    }
                }
            }

            Surface {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        SectionHeading("A little less movement")
                        SmallCopy("Reduce animation.")
                    }
                    // Green is the one colour the specimen lets the interface
                    // itself use, and this is the only place it uses it: a
                    // switch that is on. Left to Material it would come out
                    // ink, because ink is `primary` in this palette.
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = { save(settings.copy(reducedMotion = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = Leaf,
                            checkedBorderColor = Leaf,
                            uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                            uncheckedTrackColor = Hairline,
                            uncheckedBorderColor = Hairline,
                        ),
                    )
                }
            }

            // Both of these used to sit at the bottom of home, under the
            // garden, where they competed with the things you open Harbor to
            // do. They are settings; they live with the settings.
            StudyExportCard(store)

            TextLink("When you are busy", onEditSchedule)
            TextLink("Find a quiet moment", onOpenCues)
            TextLink("Back", onDone)
        }
    }
}

/** `.duration-row` — a label, and a round stepper either side of the value. */
@Composable
internal fun Stepper(label: String, value: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("-", onDown)
            Text(
                value,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp).size(width = 76.dp, height = 20.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            )
            StepButton("+", onUp)
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) = Box(
    Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    Text(
        glyph,
        style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/** A chip that fills in when chosen, as `.cue-topic` does. */
@Composable
internal fun Pill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = Box(
    modifier
        .clip(RoundedCornerShape(99.dp))
        .background(
            if (selected) MaterialTheme.colorScheme.primary
            else androidx.compose.ui.graphics.Color.Transparent,
        )
        .border(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(99.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        ),
    )
}

private val CueSound.label: String
    get() = when (this) {
        CueSound.CHIME -> "Little chime"
        CueSound.SOFT -> "Soft note"
        CueSound.SILENT -> "Silence"
    }
