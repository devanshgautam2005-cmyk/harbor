package app.harbor.domain

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

    /** "40 min", "1 hr", "1 hr 20 min". */
    fun formatDuration(minutes: Int?): String {
        val n = minutes ?: return "a little while"
        if (n < 60) return "$n min"
        val hours = n / 60
        val rest = n % 60
        return if (rest == 0) "$hours hr" else "$hours hr $rest min"
    }
}
