package app.harbor.domain

import java.time.LocalDate

/**
 * One small question a day.
 *
 * The same question for the whole day, chosen from the date rather than at
 * random, so it does not change under someone who half-answered it and came
 * back. No streak, nothing to keep up — answering today and skipping tomorrow
 * are equally fine, which is the same principle as everything else here.
 *
 * Ported from `gameForDay` in the prototype, hash and all, so the question of
 * the day matches between the two.
 */
object DailyQuestion {

    val QUESTIONS: List<String> = listOf(
        "Tea, coffee, or neither today?",
        "Window seat or aisle?",
        "One good thing about today, however small?",
        "Sweet or savory breakfast?",
        "Beach or mountains?",
        "If today had a theme song, what would it be?",
        "Early bird or night owl, honestly?",
        "What smell reminds you of home right now?",
        "Rain or sunshine today?",
        "One word for how today felt?",
        "Cat person, dog person, or neither?",
        "One small thing you are looking forward to?",
    )

    /**
     * @param day the local date, as the prototype formats it: `YYYY-MM-DD`.
     */
    fun forDay(day: LocalDate): String {
        val text = day.toString()
        var h = 0u
        for (c in text) {
            // Matches the prototype's `(h * 31 + code) >>> 0`, including the
            // wrap at 32 bits.
            h = (h.toInt() * 31 + c.code).toUInt()
        }
        return QUESTIONS[(h % QUESTIONS.size.toUInt()).toInt()]
    }
}
