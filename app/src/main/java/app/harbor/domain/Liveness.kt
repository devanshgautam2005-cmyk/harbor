package app.harbor.domain

import java.time.Duration
import java.time.Instant

/**
 * Whether sensing is actually alive.
 *
 * `Sensing.isActive` only says the switch is on and the permission is
 * granted. It cannot tell whether Google Play services ever registered the
 * transitions, whether the receiver is still running, or whether the OS put
 * the app to sleep days ago — all of which are ordinary on aggressive Android
 * skins, and all of which look identical to a person who simply did not walk.
 *
 * This reads the last transition the system delivered and says which of those
 * it is. The point is not to reassure anybody; it is that a week with no cues
 * has to be interpretable afterwards, and it is not unless the app recorded
 * its own pulse while the week was happening.
 */
object Liveness {

    /**
     * How long a silence has to run before it is worth mentioning.
     *
     * Twelve hours, because a night is legitimately quiet: becoming still at
     * bedtime is one transition, and then there is nothing to report until
     * morning. A gap longer than a night plus a slow start is the first point
     * at which silence stops being ordinary.
     */
    val QUIET: Duration = Duration.ofHours(12)

    enum class State {
        /** Never heard from the system at all. */
        NEVER,

        /** Heard from it recently enough to believe it is working. */
        HEALTHY,

        /** On, permitted, and silent for longer than a night. */
        QUIET_TOO_LONG,
    }

    fun state(last: Instant?, now: Instant): State = when {
        last == null -> State.NEVER
        Duration.between(last, now) >= QUIET -> State.QUIET_TOO_LONG
        else -> State.HEALTHY
    }

    /**
     * How long ago, in the app's own voice.
     *
     * Deliberately vague at the large end: "yesterday" is what somebody
     * actually needs to know, and "27 hours ago" is a number pretending to be
     * an answer.
     */
    fun phrase(last: Instant?, now: Instant): String {
        if (last == null) return "not yet"
        val gap = Duration.between(last, now)
        if (gap.isNegative) return "just now"
        val minutes = gap.toMinutes()
        return when {
            minutes < 2 -> "just now"
            minutes < 60 -> "$minutes minutes ago"
            minutes < 120 -> "an hour ago"
            gap.toHours() < 24 -> "${gap.toHours()} hours ago"
            gap.toDays() < 2 -> "yesterday"
            else -> "${gap.toDays()} days ago"
        }
    }
}
