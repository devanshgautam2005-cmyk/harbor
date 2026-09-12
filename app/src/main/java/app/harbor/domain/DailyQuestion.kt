package app.harbor.domain

import java.time.LocalDate

/**
 * The one small thing Harbor asks you each day.
 *
 * **Describe your day in one word.** The same prompt every day, and that is
 * the point: it is not a quiz, it is a place to put a word down. Answering
 * today and skipping tomorrow are equally fine — no streak, nothing to keep
 * up, which is the same principle as everything else here.
 *
 * ## Why the rotation went
 *
 * This used to pick one of twelve questions from a hash of the date, ported
 * from the prototype's `gameForDay`. Half of them could not be answered in the
 * box they were asked in: "If today had a theme song, what would it be?" over
 * a single-word field, with the placeholder *one word*, is a question the
 * screen will not take an answer to. And a prompt that changes daily makes a
 * week of answers unreadable — twelve different questions with one word each
 * is a pile of words rather than a record of anything.
 *
 * One prompt, asked the same way every day, gives a week that can be read down
 * the page. That is worth more than variety here.
 */
object DailyQuestion {

    const val PROMPT = "Describe your day in one word"

    /** Still a list, so a caller can check that an answer belongs to a prompt. */
    val QUESTIONS: List<String> = listOf(PROMPT)

    /**
     * @param day accepted so callers can key their state on the date, and so
     *   this can go back to varying without touching every call site.
     */
    @Suppress("UNUSED_PARAMETER")
    fun forDay(day: LocalDate): String = PROMPT
}
