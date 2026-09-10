package app.harbor.cue

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.harbor.data.HarborRepository
import app.harbor.data.HarborStore
import app.harbor.domain.Contact
import app.harbor.domain.FeedbackPulse
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.RewardShown
import app.harbor.domain.TriggerSource
import app.harbor.ui.theme.HarborTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * The cue surface — pipeline stages 5 through 9.
 *
 * Call-shaped by design (ADR-009): full screen, over the lock screen, with the
 * contact's photo and their own ringtone playing. The association that sound
 * carries is the mechanism the whole trigger depends on.
 *
 * It never claims to be an incoming call. No "Mom is calling", no answer and
 * decline pair. Someone far from home who thinks their mother is unexpectedly
 * ringing will assume an emergency, and one such scare costs more trust than
 * the entire feature is worth.
 */
class CueActivity : ComponentActivity() {

    private val ringer by lazy { Ringer(this) }
    private lateinit var store: HarborRepository

    private var cueId: UUID? = null
    private var contactId: UUID? = null

    /**
     * The entry this cue produced, held so the feedback pulse can amend it
     * rather than write a second row.
     *
     * The id is generated once and reused, which makes [HarborRepository.append]
     * an idempotent replace — the same guarantee the server's upsert gives.
     */
    private var entryId: UUID? = null
    private var resolution: Resolution? = null
    private var proposedTime: Instant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Present like a call: wake the screen, show over the keyguard. The
        // quiet moment after a walk is often a moment with the phone in a
        // pocket and the screen dark.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        store = HarborStore(applicationContext)
        cueId = intent.getStringExtra(CueNotifier.EXTRA_CUE_ID)?.let(UUID::fromString)
        contactId = intent.getStringExtra(CueNotifier.EXTRA_CONTACT_ID)?.let(UUID::fromString)
        val contact = store.contacts.value.firstOrNull { it.id == contactId }

        // The notification has done its job; the surface takes over the sound.
        CueNotifier.cancel(this)
        ringer.start(contact?.cueSoundRef)

        setContent {
            HarborTheme {
                Surface(Modifier.fillMaxSize()) {
                    CueSurface(
                        contact = contact,
                        onRecord = ::record,
                        onDismiss = ::dismissAndFinish,
                    )
                }
            }
        }
    }

    /**
     * Stage 9, and stage 8 when the pulse arrives afterwards.
     *
     * Called twice for most cues: once when the user picks an option, once
     * more if they answer "was this a good moment?". The second call must
     * amend the first entry, not write another — so the id is generated once
     * and the resolution and any proposed time are remembered rather than
     * passed back in. Passing them back was how an earlier version of this
     * managed to lose every feedback pulse, and to crash on a proposed-later
     * cue whose second call had no time attached.
     *
     * The cue itself was recorded when it fired and already spent one of the
     * user's daily allowance. This only records what they chose.
     */
    private fun record(
        resolution: Resolution,
        proposedTime: Instant? = null,
        pulse: FeedbackPulse? = null,
    ) {
        // A real answer is never overwritten by a later dismissal — closing
        // the screen after choosing is not undoing the choice.
        if (this.resolution != null && resolution == Resolution.DISMISSED) return

        ringer.stop()

        this.resolution = resolution
        if (proposedTime != null) this.proposedTime = proposedTime
        val id = entryId ?: UUID.randomUUID().also { entryId = it }

        val now = Instant.now()
        val entry = LedgerEntry(
            id = id,
            entryDate = now.atZone(ZoneId.systemDefault()).toLocalDate(),
            cueId = cueId,
            contactId = contactId,
            triggerSource = TriggerSource.WALKING_STOP,
            thresholdSnapshot = store.settings.value.thresholds,
            resolution = resolution,
            proposedTime = this.proposedTime.takeIf { resolution == Resolution.PROPOSED_LATER },
            feedbackPulse = pulse,
            rewardShown = RewardShown.READOUT,
            occurredAt = now,
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.append(entry) }
        }
    }

    /** Dismissal costs nothing, but it is still an answer, so it is recorded. */
    private fun dismissAndFinish() {
        record(Resolution.DISMISSED)
        ringer.stop()
        finish()
    }

    override fun onDestroy() {
        ringer.stop()
        super.onDestroy()
    }
}

@Composable
private fun CueSurface(
    contact: Contact?,
    onRecord: (Resolution, Instant?, FeedbackPulse?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var chosen by remember { mutableStateOf<Resolution?>(null) }
    var planning by remember { mutableStateOf(false) }
    var pulsed by remember { mutableStateOf(false) }

    // Back is a dismissal, in one gesture, with no cost. Handoff, section 7.
    BackHandler(enabled = chosen == null) { onDismiss() }

    val who = contact?.label ?: "someone at home"

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val photo by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, contact?.photoRef) {
            val ref = contact?.photoRef
            value = if (ref == null) null else withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(ref)).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }

        photo?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.size(24.dp))
        }

        when {
            chosen != null -> Resolved(
                who = who,
                pulsed = pulsed,
                onPulse = { pulse ->
                    pulsed = true
                    onRecord(chosen!!, null, pulse)
                },
                onDone = onDismiss,
            )

            planning -> PlanLater(
                onPick = { at ->
                    chosen = Resolution.PROPOSED_LATER
                    onRecord(Resolution.PROPOSED_LATER, at, null)
                },
                onBack = { planning = false },
            )

            else -> Choices(
                who = who,
                canCall = contact?.phoneE164 != null,
                onCall = {
                    contact?.phoneE164?.let { number ->
                        // ACTION_DIAL, not ACTION_CALL: the dialer opens with
                        // the number filled in and the user presses call
                        // themselves. Costs no permission. ADR-002.
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")),
                        )
                    }
                    chosen = Resolution.CALLED
                    onRecord(Resolution.CALLED, null, null)
                },
                onReact = {
                    chosen = Resolution.REACTED
                    onRecord(Resolution.REACTED, null, null)
                },
                onPlan = { planning = true },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun Choices(
    who: String,
    canCall: Boolean,
    onCall: () -> Unit,
    onReact: () -> Unit,
    onPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Harbor asking, in its own voice. Never "$who is calling".
    Text(
        "A quiet moment.",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(8.dp))
    Text(
        "Would now be a good time to call $who?",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(32.dp))

    // Three options, equal weight, no default. Handoff, stage 6.
    Button(onClick = onCall, enabled = canCall, modifier = Modifier.fillMaxWidth()) {
        Text("Call now")
    }
    Spacer(Modifier.size(12.dp))
    OutlinedButton(onClick = onReact, modifier = Modifier.fillMaxWidth()) {
        Text("Send a little love")
    }
    Spacer(Modifier.size(12.dp))
    OutlinedButton(onClick = onPlan, modifier = Modifier.fillMaxWidth()) {
        Text("Plan a better time")
    }

    Spacer(Modifier.size(24.dp))
    TextButton(onClick = onDismiss) { Text("Not now, and that's okay") }
}

@Composable
private fun PlanLater(onPick: (Instant) -> Unit, onBack: () -> Unit) {
    Text("When would suit you?", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.size(8.dp))
    Text(
        "A reminder inside Harbor, not a promise to anyone.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(24.dp))

    // Presets rather than a time picker. At the end of a walk, with a phone
    // half out of a pocket, three taps of a clock face is friction the moment
    // will not survive. The prototype uses a datetime field on the web, where
    // that is cheap; this is the native equivalent, not a divergence in
    // intent.
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    listOf(
        "In an hour" to now.plus(Duration.ofHours(1)),
        "This evening" to now.atZone(zone).with(LocalTime.of(20, 0))
            .let { if (it.toInstant().isAfter(now)) it.toInstant() else it.plusDays(1).toInstant() },
        "Tomorrow" to now.atZone(zone).plusDays(1).with(LocalTime.of(18, 0)).toInstant(),
    ).forEach { (label, at) ->
        OutlinedButton(onClick = { onPick(at) }, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
        Spacer(Modifier.size(12.dp))
    }

    TextButton(onClick = onBack) { Text("Back") }
}

@Composable
private fun Resolved(
    who: String,
    pulsed: Boolean,
    onPulse: (FeedbackPulse) -> Unit,
    onDone: () -> Unit,
) {
    // Stage 7: the reward. Varying but always guaranteed — never random.
    Text("Caught it.", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.size(8.dp))
    Text(
        "A little closer to $who than you were a minute ago.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(32.dp))

    if (!pulsed) {
        // Stage 8: one tap, optional, shown once.
        Text("Was this a good moment?", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onPulse(FeedbackPulse.GOOD_TIME) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Good time") }
            Spacer(Modifier.size(12.dp))
            OutlinedButton(
                onClick = { onPulse(FeedbackPulse.BAD_TIME) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Not this time") }
        }
    } else {
        Text("Thank you — that helps.", style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(Modifier.weight(1f, fill = false))
    Spacer(Modifier.size(24.dp))
    TextButton(onClick = onDone) { Text("Back to my day") }
}
