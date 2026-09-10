package app.harbor.sensing

import app.harbor.domain.CuePolicy
import app.harbor.domain.TriggerSource
import java.time.Duration
import java.time.Instant

/**
 * Stage 1 of the pipeline, as a pure state machine.
 *
 * Google Play services hands us a stream of activity transitions — entered
 * walking, exited walking, entered still — and this turns that stream into
 * the one thing the rest of the pipeline cares about: a walk that ended in
 * stillness, and how long it was.
 *
 * Pure and testable for the same reason [CuePolicy] is. Transition streams in
 * the wild are messy — events arrive late, out of order, duplicated, or not at
 * all — and every one of those cases is trivial to write down here and
 * miserable to reproduce on a phone.
 */
object BoutTracker {

    /**
     * A bout longer than this is not a walk, it is a missed EXIT event.
     *
     * The transition API drops events — the process is killed, the OEM
     * suspends Play services, the phone is off. Without a ceiling, a stale
     * `walkingSince` from Tuesday produces a "2,400 minute walk" on Thursday
     * that sails past any threshold the user could set. Discarding is right:
     * a bout we cannot vouch for should not become a cue.
     */
    val MAX_BOUT: Duration = Duration.ofHours(4)

    /** The activities we care about. Everything else is [Activity.OTHER]. */
    enum class Activity { WALKING, STILL, OTHER }

    enum class Kind { ENTER, EXIT }

    data class Event(val activity: Activity, val kind: Kind, val at: Instant)

    /**
     * Everything the tracker remembers between events.
     *
     * It has to survive process death — the receiver is woken, runs for
     * milliseconds, and dies — so it is a small serialisable value rather than
     * anything held in memory. See [SensingStore].
     */
    data class State(val walkingSince: Instant? = null)

    data class Step(val state: State, val signal: CuePolicy.Signal?)

    /**
     * Advance the machine by one transition.
     *
     * @return the new state, and a signal if a walk just ended in stillness.
     */
    fun advance(state: State, event: Event): Step = when {
        // A walk begins. If one was already open we keep the earlier start:
        // a duplicate ENTER is far more likely than the user genuinely
        // starting a second walk without stopping the first.
        event.activity == Activity.WALKING && event.kind == Kind.ENTER ->
            Step(State(walkingSince = state.walkingSince ?: event.at), null)

        // Stillness is what ends a bout — not EXIT walking, which also fires
        // when someone starts running or gets into a car. The handoff is
        // specific that the cue belongs to the *stop*, not to the end of
        // movement.
        event.activity == Activity.STILL && event.kind == Kind.ENTER ->
            closeBout(state, stillSince = event.at)

        // Got into a vehicle, started cycling, started running: the walk ended,
        // but not in the stillness we are looking for. Drop the bout rather
        // than let it hang open and later close against unrelated stillness.
        event.activity == Activity.OTHER && event.kind == Kind.ENTER ->
            Step(State(), null)

        // EXIT events and anything else move nothing.
        else -> Step(state, null)
    }

    private fun closeBout(state: State, stillSince: Instant): Step {
        val startedAt = state.walkingSince
            // Still, but no walk was open. The common case by far — most
            // stillness follows more stillness.
            ?: return Step(State(), null)

        val bout = Duration.between(startedAt, stillSince)

        // Negative means the clock moved backwards (timezone change, NTP
        // correction, a stale event). Not measurable, so not a cue.
        if (bout.isNegative || bout > MAX_BOUT) {
            return Step(State(), null)
        }

        return Step(
            State(),
            CuePolicy.Signal(
                source = TriggerSource.WALKING_STOP,
                // Truncating, not rounding: a 9-minute-59-second walk should
                // not clear a 10-minute threshold the user set deliberately.
                activeMinutes = bout.toMinutes().toInt(),
                stillSince = stillSince,
            ),
        )
    }
}
