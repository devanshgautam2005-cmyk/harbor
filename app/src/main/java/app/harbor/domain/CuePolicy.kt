package app.harbor.domain

import java.time.Duration
import java.time.Instant

/**
 * Stages 2-4 of the pipeline: threshold check, suppression check, kairos
 * refinement. Given a sensed transition and what has already happened today,
 * decide whether to surface a cue.
 *
 * This is a pure function over its inputs. It does no IO, reads no clock of
 * its own, and knows nothing about Android — so it can be exercised properly
 * in unit tests, which matters more here than anywhere else in the app: a
 * cue that fires at the wrong moment is the single worst thing Harbor can
 * do to a user's trust.
 *
 * It also never blocks on the network. That is ADR-003, and it is why the
 * caller passes today's entries in rather than this code fetching them.
 */
object CuePolicy {

    /**
     * How long the user must have been still before we treat the transition
     * as genuinely complete. Stage 4.
     *
     * The point is to avoid firing at a traffic light. Long enough to mean
     * "stopped", short enough that the moment hasn't passed by the time we
     * ask.
     */
    val SETTLE: Duration = Duration.ofSeconds(90)

    /**
     * A transition the sensing layer has observed. [activeMinutes] is the
     * length of the bout that just ended — the walk, or the app session.
     */
    data class Signal(
        val source: TriggerSource,
        val activeMinutes: Int,
        /** When the user became still. Not when we noticed. */
        val stillSince: Instant,
    )

    /** What the pipeline decided, and why. The why is worth keeping. */
    sealed interface Decision {
        /** Surface the cue. Stage 5. */
        data object Fire : Decision

        /** Do nothing. [reason] is for logs and debugging, never for the user. */
        data class Hold(val reason: Reason) : Decision
    }

    enum class Reason {
        /** The bout was shorter than the user's own calibrated threshold. */
        BELOW_THRESHOLD,

        /** They already spoke to their person today. Nothing left to prompt. */
        ALREADY_CALLED_TODAY,

        /** The user's own daily ceiling. */
        DAILY_CAP_REACHED,

        /** Too soon after the last cue. */
        IN_COOLDOWN,

        /** They proposed a later time and it hasn't come round yet. */
        REMINDER_PENDING,

        /** Stopped, but not for long enough to be sure they've settled. */
        TRANSITION_UNSETTLED,
    }

    /**
     * @param signal what sensing observed.
     * @param thresholds the user's current calibration.
     * @param today every ledger entry already written for the current local
     *   day, in any order.
     * @param lastCueAt when the last cue fired, on any day. Separate from
     *   [today] because the cooldown has to survive midnight: a cue at 23:55
     *   must still suppress one at 00:05, and "today" is empty by then.
     * @param now the current instant, passed in so tests can control it.
     */
    fun decide(
        signal: Signal,
        thresholds: UserThresholds,
        today: List<LedgerEntry>,
        lastCueAt: Instant?,
        now: Instant,
    ): Decision {
        // --- stage 2: threshold ------------------------------------------
        val required = when (signal.source) {
            TriggerSource.WALKING_STOP -> thresholds.walkingMinutes
            TriggerSource.SESSION_END -> thresholds.sessionMinutes
            // The user asked for this one. Their asking is the threshold.
            TriggerSource.MANUAL -> 0
        }
        if (signal.activeMinutes < required) {
            return Decision.Hold(Reason.BELOW_THRESHOLD)
        }

        // --- stage 3: suppression ----------------------------------------
        // A manual request bypasses suppression: the user is standing there
        // asking for the prompt, and refusing them would be absurd.
        if (signal.source != TriggerSource.MANUAL) {
            if (today.any { it.resolution == Resolution.CALLED }) {
                return Decision.Hold(Reason.ALREADY_CALLED_TODAY)
            }
            if (today.size >= thresholds.dailyCap) {
                return Decision.Hold(Reason.DAILY_CAP_REACHED)
            }
            val pendingReminder = today.any { entry ->
                entry.resolution == Resolution.PROPOSED_LATER &&
                    entry.proposedTime != null &&
                    entry.proposedTime.isAfter(now)
            }
            if (pendingReminder) {
                return Decision.Hold(Reason.REMINDER_PENDING)
            }
            if (lastCueAt != null) {
                val elapsed = Duration.between(lastCueAt, now)
                if (elapsed < Duration.ofMinutes(thresholds.cooldownMinutes.toLong())) {
                    return Decision.Hold(Reason.IN_COOLDOWN)
                }
            }
        }

        // --- stage 4: kairos ---------------------------------------------
        // Fire on the completed stop, never mid-activity. Handoff, section 7.
        if (signal.source != TriggerSource.MANUAL) {
            if (Duration.between(signal.stillSince, now) < SETTLE) {
                return Decision.Hold(Reason.TRANSITION_UNSETTLED)
            }
        }

        return Decision.Fire
    }
}
