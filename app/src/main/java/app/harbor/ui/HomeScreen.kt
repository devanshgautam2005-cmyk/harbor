package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.CallStats
import app.harbor.domain.Contact
import app.harbor.domain.DailyQuestion
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.ui.theme.Avatar
import app.harbor.ui.theme.AvatarSize
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * Home, hand-translated from `components/harbor/home.tsx`.
 *
 * The order is the prototype's and it is the argument: greeting, then the
 * garden, then how life feels, then your people. The garden sits above the
 * fold because it is the point of the app rather than a page you navigate to
 * — opening Harbor should show you what calling people has grown, not a
 * console for an app.
 */
@Composable
fun HomeScreen(
    store: HarborRepository,
    onOpenGarden: () -> Unit,
    onOpenCues: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenPerson: (java.util.UUID) -> Unit,
    onReflect: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by store.settings.collectAsState()
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val grown = entries.count { it.resolution == Resolution.CALLED && it.flower != null }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // .home-greeting
        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 14.dp)) {
            Eyebrow("A little closer, every day")
            Spacer(Modifier.size(4.dp))
            Text(
                if (settings.name.isBlank()) "Hey there." else "Hey, ${settings.name}.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
            )
            Spacer(Modifier.size(6.dp))
            SmallCopy(
                if (grown > 0) "$grown calls have grown here."
                else "Your first call plants the first flower.",
            )
        }

        // .garden-holder — 404px of garden, on the home screen
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(404.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenGarden),
        ) {
            GardenCanvas(store, Modifier.fillMaxSize())
        }

        Flow(Modifier.pageContent()) {
            WeatherBar(store)

            // A call Harbor watched you start and never heard about.
            CallStats.pendingReflection(entries, Instant.now())?.let { waiting ->
                Surface {
                    SectionHeading("How did that go?")
                    SmallCopy(
                        "You called " +
                            (contacts.firstOrNull { it.id == waiting.contactId }?.label
                                ?: "someone") +
                            " earlier. It only takes a moment, and it is what grows " +
                            "the flower.",
                    )
                    TextLink("Tell me") { onReflect(waiting) }
                }
            }

            SectionHeading("Your people")
            if (contacts.isEmpty()) {
                SmallCopy("Nobody yet. Add someone, and their patch appears above.")
                TextLink("Choose someone", onOpenCues)
            }
            contacts.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { contact ->
                        PersonTile(
                            contact = contact,
                            calls = entries.count {
                                it.resolution == Resolution.CALLED &&
                                    it.contactId == contact.id &&
                                    it.flower != null
                            },
                            usual = CallStats.usualMinutes(entries, contact.id),
                            onClick = { onOpenPerson(contact.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            DailyQuestionCard(store)

            TextLink("Leave a line", onOpenNotes)
            TextLink("Find a quiet moment", onOpenCues)
            TextLink("Your pace", onOpenSettings)
        }
    }
}

/** `.chat-tile` — a person, their patch, and how it has been going. */
@Composable
private fun PersonTile(
    contact: Contact,
    calls: Int,
    usual: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Avatar(contact.label, contact.tone, size = AvatarSize.MD)
        Spacer(Modifier.size(8.dp))
        Text(
            contact.label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.size(5.dp))
        SmallCopy(
            usual?.let { "Calls run about ${CallStats.formatDuration(it)}." }
                ?: "Say hello whenever.",
            size = 13,
        )
        Spacer(Modifier.size(10.dp))
        SmallCopy(
            when (calls) {
                0 -> "No calls yet"
                1 -> "1 flower in their patch"
                else -> "$calls flowers in their patch"
            },
            size = 12,
        )
    }
}

/** `.text-link` — a quiet way onward, never a button competing for attention. */
@Composable
internal fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * One small question a day.
 *
 * Reshaped, not ported: the prototype shows your family's answers beside
 * yours, and there is no family side in this build (ADR-007). Inventing their
 * replies is the one thing the prototype itself never does.
 */
@Composable
private fun DailyQuestionCard(store: HarborRepository) {
    val scope = rememberCoroutineScope()
    val answers by store.dailyAnswers.collectAsState()
    val today = remember { LocalDate.now() }
    val question = remember(today) { DailyQuestion.forDay(today) }
    val answered = answers[today]
    var draft by remember { mutableStateOf("") }

    Surface {
        Eyebrow("Today's little question")
        Text(question, style = MaterialTheme.typography.titleLarge)

        if (answered != null) {
            SmallCopy(answered)
            SmallCopy(
                "No streak to keep. Answer today, skip tomorrow — either is fine.",
                size = 13,
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
