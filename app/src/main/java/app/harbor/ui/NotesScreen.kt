package app.harbor.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.TriggerSource
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.Notice
import app.harbor.ui.theme.PageIntro
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * A line to send, when a call is more than you have in you.
 *
 * Reshaped rather than ported (ADR-007). The prototype's strip is two-sided —
 * lines you leave and lines your people leave back — and the incoming half
 * cannot exist here without being a fiction.
 *
 * What is left is arguably more honest: Harbor helps you write the line and
 * hands it to whatever you actually use to send things. It does not pretend
 * to deliver it, and it does not pretend anyone replied.
 */
@Composable
fun NotesScreen(
    store: HarborRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }
    var line by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val who = contacts.firstOrNull()

    // A picture, handed straight to whatever sends it.
    //
    // Harbor deliberately does not keep the image, for the same reason it does
    // not keep the words: a snapshot is something you sent someone, not a
    // record this app is owed. The ledger holds that it happened.
    val pickPicture = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { picked ->
        if (picked != null && who != null) {
            recordSnapshot(store, scope, who.id) { entries = it }
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, picked)
                        if (line.isNotBlank()) putExtra(Intent.EXTRA_TEXT, line.trim())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Send your picture",
                ),
            )
            line = ""
        }
    }

    fun record() {
        val text = line.trim()
        if (text.isEmpty() || who == null) return
        val now = Instant.now()
        scope.launch {
            store.append(
                LedgerEntry(
                    id = UUID.randomUUID(),
                    entryDate = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                    cueId = null,
                    contactId = who.id,
                    triggerSource = TriggerSource.NOTE,
                    thresholdSnapshot = store.settings.value.thresholds,
                    resolution = Resolution.MESSAGE,
                    proposedTime = null,
                    feedbackPulse = null,
                    callMinutes = null,
                    feeling = null,
                    flower = null,
                    topic = null,
                    note = text,
                    occurredAt = now,
                ),
            )
            entries = store.recentEntries()
            line = ""
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.padding(horizontal = 28.dp)) {
            PageIntro(
                eyebrow = "Small enough that nobody owes a reply",
                title = "A line, then.",
                subtitle = "One line is plenty. No call, no explanation.",
            )
        }

        Flow(Modifier.pageContent()) {
            Surface {
                OutlinedTextField(
                    value = line,
                    onValueChange = { line = it.take(120) },
                    label = { Text(who?.let { "To " + it.label } ?: "Your line") },
                    placeholder = { Text("thinking of you, that is all") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(text = "Send it", selected = true) {
                        val text = line.trim()
                        if (text.isNotEmpty() && who != null) {
                            record()
                            // Harbor sends nothing itself. It hands the line to
                            // whatever the user already uses, which is where
                            // their person actually is.
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    },
                                    "Send your line",
                                ),
                            )
                        }
                    }
                    Pill(text = "Just keep it", selected = false) { record() }
                }
                Pill(text = "Send a picture instead", selected = false) {
                    if (who != null) pickPicture.launch("image/*")
                }
                if (who == null) {
                    SmallCopy("Add someone first — a line needs somebody to be for.")
                }
            }

            Notice(
                "Harbor keeps your line so you can look back at it, along with " +
                    "when you left it. It never sends anything itself — that is " +
                    "still you, in whatever app you chose.",
            )

            val sent = entries
                .filter { it.resolution == Resolution.MESSAGE }
                .sortedByDescending { it.occurredAt }

            if (sent.isNotEmpty()) {
                SectionHeading("Lines you have left")
                sent.take(10).forEach { entry ->
                    Surface {
                        Eyebrow(
                            entry.occurredAt.atZone(ZoneId.systemDefault())
                                .toLocalDate().toString(),
                        )
                        // A line left before Harbor kept the words, or a
                        // picture, has nothing to show but the fact of it.
                        entry.note?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                "\u201c$it\u201d",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        } ?: SmallCopy("You sent something.")
                    }
                }
            }

            TextLink("Back", onDone)
        }
    }
}

/**
 * Records that a picture went out, without keeping the picture.
 *
 * Same shape of entry as a written line — from Harbor's point of view they are
 * the same act, and the study counts them the same way.
 */
private fun recordSnapshot(
    store: HarborRepository,
    scope: CoroutineScope,
    contactId: UUID,
    onSaved: (List<LedgerEntry>) -> Unit,
) {
    val now = Instant.now()
    scope.launch {
        store.append(
            LedgerEntry(
                id = UUID.randomUUID(),
                entryDate = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                cueId = null,
                contactId = contactId,
                triggerSource = TriggerSource.NOTE,
                thresholdSnapshot = store.settings.value.thresholds,
                resolution = Resolution.MESSAGE,
                proposedTime = null,
                feedbackPulse = null,
                callMinutes = null,
                feeling = null,
                flower = null,
                topic = null,
                occurredAt = now,
            ),
        )
        onSaved(store.recentEntries())
    }
}
