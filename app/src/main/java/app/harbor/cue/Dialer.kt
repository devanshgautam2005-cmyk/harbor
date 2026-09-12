package app.harbor.cue

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri
import app.harbor.data.HarborRepository
import app.harbor.domain.Contact
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.TriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * The one way Harbor starts a call.
 *
 * Every screen that can dial goes through here — the cue, home, a person's
 * page, the little window on the schedule. Before this existed there were four
 * copies of `startActivity(ACTION_DIAL)` and only one of them, the cue's,
 * wrote anything down. So a call started from home or from the schedule left
 * no trace, and the whole reflection that follows a call — how it felt, which
 * flower it becomes — never ran, because [app.harbor.domain.CallStats
 * .pendingReflection] had no row to find.
 *
 * ## Two things it fixes that a bare intent got wrong
 *
 * **The lock screen.** The cue is deliberately shown over the keyguard
 * (ADR-009): the still moment after a walk is usually a moment with the phone
 * in a pocket and the screen dark. But Android will not bring the dialer over
 * a keyguard, so on a locked phone the intent was accepted, nothing visible
 * happened, and the user was returned to a reflection screen asking how a call
 * they never made had gone. Now the keyguard is dismissed first, and if the
 * user declines to unlock, nothing is recorded.
 *
 * **Recording a call before it started.** The row used to be written and then
 * the intent fired. If there was no dialer to fire at, the ledger kept a call
 * that never happened — and `called` is the single number the study exists to
 * measure. The order is the other way round now.
 *
 * It is still [Intent.ACTION_DIAL] and never `CALL_PHONE` (ADR-002). Harbor
 * fills in the number; the person presses the green button. That is not a
 * limitation to be worked around — it is the reason this app needs no phone
 * permission at all.
 */
object Dialer {

    /** What became of an attempt to hand off. */
    sealed interface Outcome {
        /** The dialer is open. */
        data object Dialing : Outcome

        /** There is nobody to dial: the contact has no number. */
        data object NoNumber : Outcome

        /** The user was asked to unlock and chose not to. Nothing recorded. */
        data object Locked : Outcome

        /** No app on this device answers a `tel:` intent. */
        data object NoDialer : Outcome
    }

    /**
     * Put the dialer in front of the user, unlocking the phone first if it has
     * to.
     *
     * The intent half on its own, for the cue — which keeps its own ledger row
     * so that the reflection can amend it rather than write a second one.
     * Everywhere else wants [handOff].
     *
     * [onOutcome] is called on the main thread, possibly after a trip through
     * the lock screen, so callers must not assume it has run by the time this
     * returns. Nothing should be recorded before it says [Outcome.Dialing].
     */
    fun open(context: Context, number: String?, onOutcome: (Outcome) -> Unit) {
        if (number.isNullOrBlank()) {
            onOutcome(Outcome.NoNumber)
            return
        }

        val intent = Intent(Intent.ACTION_DIAL, "tel:$number".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun go() {
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // A tablet with no dialer, or a locked-down device. Say so
                // rather than recording a call and asking how it went.
                onOutcome(Outcome.NoDialer)
                return
            }
            onOutcome(Outcome.Dialing)
        }

        val activity = context.activity()
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        if (activity != null && keyguard != null && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(
                activity,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = go()

                    // Could not ask — better to try the dialer and let the
                    // system decide than to swallow the user's tap.
                    override fun onDismissError() = go()

                    override fun onDismissCancelled() = onOutcome(Outcome.Locked)
                },
            )
        } else {
            go()
        }
    }

    /**
     * Hand [contact]'s number to the dialer, and record that a call started
     * once it is actually open.
     *
     * This is what makes the reflection appear afterwards, wherever the call
     * was started from: the row it writes is the row
     * `CallStats.pendingReflection` looks for on the next resume.
     */
    fun handOff(
        context: Context,
        store: HarborRepository,
        scope: CoroutineScope,
        contact: Contact,
        source: TriggerSource = TriggerSource.MANUAL,
        topic: String? = null,
        onOutcome: (Outcome) -> Unit = {},
    ) = open(context, contact.phoneE164) { outcome ->
        if (outcome is Outcome.Dialing) record(store, scope, contact, source, topic)
        onOutcome(outcome)
    }

    /**
     * The row a call leaves the moment it starts.
     *
     * Written before anyone knows how it went, because a call that happened
     * has to be recorded even if the user never comes back to say anything
     * about it. The reflection amends this same id rather than adding a second
     * row, and "we did not get to talk" is what turns it into
     * [Resolution.NOT_REACHED].
     */
    private fun record(
        store: HarborRepository,
        scope: CoroutineScope,
        contact: Contact,
        source: TriggerSource,
        topic: String?,
    ): UUID {
        val id = UUID.randomUUID()
        val now = Instant.now()
        scope.launch {
            store.append(
                LedgerEntry(
                    id = id,
                    entryDate = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                    cueId = null,
                    contactId = contact.id,
                    triggerSource = source,
                    thresholdSnapshot = store.settings.value.thresholds,
                    resolution = Resolution.CALLED,
                    proposedTime = null,
                    feedbackPulse = null,
                    callMinutes = null,
                    feeling = null,
                    flower = null,
                    topic = topic,
                    occurredAt = now,
                ),
            )
        }
        return id
    }

    /** The Activity behind a Compose `LocalContext`, or null if there is none. */
    private fun Context.activity(): Activity? {
        var context: Context? = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}
