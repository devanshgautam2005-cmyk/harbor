package app.harbor.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * What the ledger knows about how someone's calls tend to go.
 *
 * Used by the cue, which says "calls with her usually run ~12 min" before
 * offering to place one. That line is doing real work: the most common reason
 * not to call is not knowing what you are agreeing to, and an ask with a known
 * size is a much smaller ask.
 */
object CallStats {

    /**
     * The average length of completed calls with this person, or null if there
     * have not been any yet.
     *
     * The cue falls back to a stated default rather than hiding the line — a
     * first-time user is exactly who benefits most from knowing roughly how
     * long this takes.
     */
    fun usualMinutes(entries: List<LedgerEntry>, contactId: UUID?): Int? {
        val lengths = entries
            .filter { it.resolution == Resolution.CALLED && it.contactId == contactId }
            .mapNotNull { it.callMinutes }

        if (lengths.isEmpty()) return null
        return max(1, (lengths.sum().toDouble() / lengths.size).roundToInt())
    }

    /** Which flower this person's patch is mostly made of. */
    fun dominantFlower(entries: List<LedgerEntry>, contactId: UUID?): FlowerKind? =
        entries
            .filter { it.resolution == Resolution.CALLED && it.contactId == contactId }
            .mapNotNull { it.flower }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    /**
     * A call that happened but was never reflected on.
     *
     * Harbor hands off to the phone's dialer, and people do not come back to
     * an app the moment a call ends — they put the phone in a pocket. So the
     * entry is written when the dialer opens and the reflection is offered
     * later, which is the only version of this that survives contact with how
     * calls actually end.
     *
     * Bounded to [WINDOW] because being asked about a call from last Tuesday
     * is worse than not being asked at all.
     */
    fun pendingReflection(entries: List<LedgerEntry>, now: Instant): LedgerEntry? =
        entries
            .filter {
                it.resolution == Resolution.CALLED &&
                    it.feeling == null &&
                    it.flower == null &&
                    Duration.between(it.occurredAt, now) < WINDOW &&
                    !Duration.between(it.occurredAt, now).isNegative
            }
            .maxByOrNull { it.occurredAt }

    /**
     * How long the user was away, in minutes, from the moment Harbor dialled.
     *
     * The row is written the instant the call is placed, so its `occurredAt`
     * *is* the start of the call and coming back is near enough the end of it.
     * Harbor never asks the system how long the call ran — that would need
     * READ_PHONE_STATE, and it is not worth a permission to turn "about eleven
     * minutes" into "eleven minutes and four seconds".
     *
     * Floored at one, because a call that rounds to nothing still happened,
     * and capped with the same ceiling the stepper uses.
     */
    fun minutesAway(dialedAt: Instant, now: Instant): Int =
        Duration.between(dialedAt, now).toMinutes().toInt().coerceIn(1, 180)

    /** How long a call stays worth asking about. */
    val WINDOW: Duration = Duration.ofHours(12)

    /** "40 min", "1 hr", "1 hr 20 min". */
    fun formatDuration(minutes: Int?): String {
        val n = minutes ?: return "a little while"
        if (n < 60) return "$n min"
        val hours = n / 60
        val rest = n % 60
        return if (rest == 0) "$hours hr" else "$hours hr $rest min"
    }
}
