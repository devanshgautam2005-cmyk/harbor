package app.harbor.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.harbor.cue.CueActivity
import app.harbor.cue.CueNotifier
import app.harbor.data.HarborRepository
import app.harbor.domain.Cue
import app.harbor.domain.TriggerSource
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import app.harbor.sensing.ActivityTransitions
import app.harbor.sensing.Sensing
import kotlinx.coroutines.launch

/**
 * The permission and privacy explainer, and the switch that turns cues on.
 *
 * This screen carries more risk than anything else in the app. Activity
 * recognition reads as invasive, and if someone declines here nothing
 * downstream matters — no trigger, no cue, no study data. So it explains
 * before it asks, and the system dialog only ever appears after the user has
 * chosen to see it.
 *
 * Every claim below is one the code actually keeps. If any of it stops being
 * true, this copy is the first thing that has to change — see ADR-004.
 */
@Composable
fun CuesSetupScreen(
    store: HarborRepository,
    onEditContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()
    val contact by store.contacts.collectAsState()

    var hasPermission by remember { mutableStateOf(ActivityTransitions.hasPermission(context)) }
    var refused by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val request = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        // Activity recognition is the one that decides whether sensing can run
        // at all. Notifications are asked for in the same breath because a cue
        // nobody can see is not a cue, but refusing them does not stop sensing.
        val granted = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: hasPermission
        hasPermission = granted
        refused = !granted
        if (granted) {
            scope.launch { failed = !Sensing.enable(context, store) }
        }
    }

    fun turnOn() {
        failed = false
        refused = false

        val wanted = buildList {
            if (!hasPermission) add(Manifest.permission.ACTIVITY_RECOGNITION)
            // API 33+ only. Without it the cue is posted and silently dropped,
            // which looks exactly like a trigger that never fired.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (wanted.isEmpty()) {
            scope.launch { failed = !Sensing.enable(context, store) }
        } else {
            request.launch(wanted.toTypedArray())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("A cue, never a demand.", style = MaterialTheme.typography.headlineMedium)

        Text(
            "Harbor can notice the quiet moment just after a walk ends, and " +
                "offer you the chance to call home. That is the whole of it.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("What Harbor reads", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Whether your phone thinks you are walking or still. Not " +
                        "where you are, not what you are doing, not which apps " +
                        "you use.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text("Where it stays", style = MaterialTheme.typography.titleMedium)
                Text(
                    "On this phone. Your movement is never sent to us and never " +
                        "shared with your family — not as a summary, not ever. " +
                        "The only things that leave are the ones you chose: that " +
                        "a cue appeared, and what you decided to do about it.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text("What you keep control of", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Every cue can be dismissed, and dismissing costs nothing — " +
                        "there is no streak to break. At most " +
                        "${settings.thresholds.dailyCap} a day, with at least " +
                        "${settings.thresholds.cooldownMinutes} minutes between " +
                        "them. You choose those numbers, and you can turn this " +
                        "off whenever you like.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Without someone to call, a cue can only say "someone at home" and
        // cannot dial. Worth surfacing before the switch, not after.
        val who = contact.firstOrNull()
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Who you would call", style = MaterialTheme.typography.titleMedium)
                Text(
                    who?.let { "${it.label} — ${it.phoneE164}" }
                        ?: "Nobody yet. A cue needs someone to be about.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onEditContact) {
                    Text(if (who == null) "Choose someone" else "Change")
                }

                if (who != null) {
                    // The prototype's Slack Tide screen has the same thing: a
                    // way to see a cue without waiting for a walk. It is not
                    // debug scaffolding — TriggerSource.MANUAL is in the model
                    // and CuePolicy already lets a manual request past every
                    // gate, on the grounds that someone standing there asking
                    // for the prompt should get it.
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val now = Instant.now()
                                val cue = Cue(
                                    id = UUID.randomUUID(),
                                    firedDate = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                                    triggerSource = TriggerSource.MANUAL,
                                    firedAt = now,
                                )
                                store.recordCue(cue)
                                context.startActivity(
                                    Intent(context, CueActivity::class.java).apply {
                                        putExtra(CueNotifier.EXTRA_CUE_ID, cue.id.toString())
                                        putExtra(CueNotifier.EXTRA_CONTACT_ID, who.id.toString())
                                    },
                                )
                            }
                        },
                    ) {
                        Text("Show me a cue now")
                    }
                    Text(
                        "Hear their sound and see the moment, without waiting " +
                            "for a walk. This does not use up today's allowance.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        when {
            Sensing.isActive(context, store) -> {
                Text(
                    "Cues are on. Harbor will wait for a walk of at least " +
                        "${settings.thresholds.walkingMinutes} minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = { scope.launch { Sensing.disable(context, store) } }) {
                    Text("Turn cues off")
                }
            }

            // The setting says on, but the permission has since been revoked
            // from system settings. Saying "cues are on" here would be a lie
            // the user has no way to catch.
            settings.cuesEnabled && !hasPermission -> {
                Text(
                    "Cues are paused. Harbor no longer has permission to notice " +
                        "when you stop walking.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = ::turnOn) { Text("Give permission again") }
            }

            else -> {
                Button(onClick = ::turnOn) { Text("Turn on gentle cues") }
                Text(
                    "You can do this later. Harbor works without it — you can " +
                        "always start a moment yourself.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (refused) {
            Text(
                "That is completely fine. Cues stay off, and nothing else " +
                    "changes. If you change your mind, Android may not ask " +
                    "again — you can grant it from system settings.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column {
                TextButton(onClick = { openAppSettings(context) }) {
                    Text("Open system settings")
                }
                TextButton(
                    onClick = { hasPermission = ActivityTransitions.hasPermission(context) },
                ) {
                    // Rechecking on resume would need a lifecycle observer whose
                    // API has moved around between Compose versions. A button the
                    // user presses is duller and cannot break.
                    Text("I have granted it — check again")
                }
            }
        }

        if (failed) {
            Text(
                "Harbor could not start listening. Google Play services may be " +
                    "unavailable on this phone. Cues stay off rather than " +
                    "pretending to work.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
