package app.harbor.ui

import android.content.Intent
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.harbor.domain.Thresholds
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * A line to send, when a call is more than you have in you.
 *
 * Reshaped rather than ported, per ADR-007. The prototype's Notes strip is
 * two-sided: notes you leave and notes your people leave back. There is no
 * parent side in this build, so the incoming half cannot exist without being
 * a fiction.
 *
 * What is left is still worth having, and is arguably more honest: Harbor
 * helps you write the line and then hands it to whatever you actually use to
 * send things. It does not pretend to deliver it, and it does not pretend
 * anyone replied.
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("A line, then.", style = MaterialTheme.typography.headlineMedium)
        Text(
            "One line is plenty. No call, no explanation, and nobody owes you " +
                "a reply for it.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = line,
            onValueChange = { line = it.take(120) },
            label = { Text(who?.let { "To ${it.label}" } ?: "Your line") },
            placeholder = { Text("thinking of you, that's all") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val text = line.trim()
                    record()
                    // Harbor does not send anything itself. It hands the line
                    // to whatever the user already uses, which is where their
                    // person actually is.
                    if (text.isNotEmpty()) {
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
                },
                enabled = line.isNotBlank() && who != null,
            ) { Text("Send it") }

            OutlinedButton(
                onClick = ::record,
                enabled = line.isNotBlank() && who != null,
            ) { Text("Just keep it") }
        }

        if (who == null) {
            Text(
                "Add someone first — a line needs somebody to be for.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        val sent = entries.filter { it.resolution == Resolution.MESSAGE }
            .sortedByDescending { it.occurredAt }
        if (sent.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            Text("Lines you've left", style = MaterialTheme.typography.titleMedium)
            sent.take(10).forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            entry.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate()
                                .toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        // The text itself is deliberately not stored. A note is
                        // a thing you said to someone, not a record Harbor
                        // keeps — the ledger holds that it happened and when.
                        Text(
                            "You left a line.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onDone) { Text("Back") }
    }
}
