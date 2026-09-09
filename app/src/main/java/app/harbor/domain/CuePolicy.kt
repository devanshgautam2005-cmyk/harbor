package app.harbor.domain

import java.time.Duration
import java.time.Instant

/**
 * Stages 2-4 of the pipeline: threshold check, suppression check, kairos
 * refinement. Given a sensed transition and what has already happened, decide
 * whether to surface a cue.
 *
 * This is a pure function over its inputs. It does no IO, reads no clock of
 * its own, and knows nothing about Android — so it can be exercised properly
 * in unit tests, which matters more here than anywhere else in the app: a cue
 * that fires at the wrong moment is the single worst thing Harbor can do to a
 * user's trust.
 *
 * It also never blocks on the network. That is ADR-003, and it is why the
 * caller assembles [DayState] and passes it in rather than this code fetching
 * anything.
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

    /**
     * Everything about the user's recent history that bears on the decision.
     *
     * The three counters are separate on purpose. [cuesToday] is not
     * `entriesToday.size`: the cap counts cues that *fired*, and a cue the
     * user swiped away without answering still spent one of their two.
     * [lastCueAt] is not derived from today either, because the cooldown has
     * to survive midnight — a cue at 23:55 must still suppress one at 00:05.
     * And [hasPendingReminder] spans every day, since a plan made on Tuesday
     * for Friday is still a plan.
     */
    data class DayState(
        val entriesToday: List<LedgerEntry>,
        val cuesToday: Int,
        val lastCueAt: Instant?,
        val hasPendingReminder: Boolean,
    )

    /** What the pipeline decided, and why. The why is worth keeping. */
    sealed interface Decision {
        /** Surface the cue. Stage 5. */
        data object Fire : Decision

        /** Do nothing. [reason] is for logs and debugging, never for the user. */
        data class Hold(val reason: Reason) : Decision
    }

    enum class Reason {
        /** The user has cues switched off. Their choice, and it is absolute. */
        CUES_DISABLED,

        /** The bout was shorter than the user's own calibrated threshold. */
        BELOW_THRESHOLD,

        /** They already reached their person today. Nothing left to prompt. */
        ALREADY_CONNECTED_TODAY,

        /** The user's own daily ceiling. */
        DAILY_CAP_REACHED,

        /** Too soon after the last cue. */
        IN_COOLDOWN,

        /** They planned a later time and it hasn't been dealt with yet. */
        REMINDER_PENDING,

        /** Stopped, but not for long enough to be sure they've settled. */
        TRANSITION_UNSETTLED,
    }

    /**
     * @param signal what sensing observed.
     * @param settings the user's current preferences and calibration.
     * @param day what has already happened, assembled by the caller.
     * @param now the current instant, passed in so tests can control it.
     */
    fun decide(
        signal: Signal,
        settings: UserSettings,
        day: DayState,
        now: Instant,
    ): Decision {
        // A manual request is the user standing there asking for the prompt.
        // Refusing them because of a cooldown, a cap, or their own off switch
        // would be absurd, so the whole gauntlet is skipped.
        if (signal.source == TriggerSource.MANUAL) return Decision.Fire

        if (!settings.cuesEnabled) {
            return Decision.Hold(Reason.CUES_DISABLED)
        }

        // --- stage 2: threshold ------------------------------------------
        val thresholds = settings.thresholds
        val required = when (signal.source) {
            TriggerSource.WALKING_STOP -> thresholds.walkingMinutes
            TriggerSource.SESSION_END -> thresholds.sessionMinutes
            // Dispatch decides its own timing upstream; there is no bout to
            // measure, so there is nothing to compare a threshold against.
            TriggerSource.DISPATCH -> 0
            TriggerSource.MANUAL -> 0
        }
        if (signal.activeMinutes < required) {
            return Decision.Hold(Reason.BELOW_THRESHOLD)
        }

        // --- stage 3: suppression ----------------------------------------
        if (day.entriesToday.any { it.resolution.isConnection }) {
            return Decision.Hold(Reason.ALREADY_CONNECTED_TODAY)
        }
        if (day.cuesToday >= thresholds.dailyCap) {
            return Decision.Hold(Reason.DAILY_CAP_REACHED)
        }
        if (day.hasPendingReminder) {
            return Decision.Hold(Reason.REMINDER_PENDING)
        }
        if (day.lastCueAt != null) {
            val elapsed = Duration.between(day.lastCueAt, now)
            if (elapsed < Duration.ofMinutes(thresholds.cooldownMinutes.toLong())) {
                return Decision.Hold(Reason.IN_COOLDOWN)
            }
        }

        // --- stage 4: kairos ---------------------------------------------
        // Fire on the completed stop, never mid-activity. Handoff, section 7.
        // Dispatch has no transition to settle.
        if (signal.source != TriggerSource.DISPATCH) {
            if (Duration.between(signal.stillSince, now) < SETTLE) {
                return Decision.Hold(Reason.TRANSITION_UNSETTLED)
            }
        }

        return Decision.Fire
    }
}
