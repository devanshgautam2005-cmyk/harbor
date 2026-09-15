package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * One prompt, every day.
 *
 * These used to pin the rotation against the prototype's `gameForDay`. There
 * is no rotation now — see [DailyQuestion] for why — so what is worth holding
 * is that the prompt is stable and that it is answerable in one word, which is
 * the box it is asked in.
 */
class DailyQuestionTest {

    @Test
    fun every_day_gets_the_same_prompt() {
        val days = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 9, 11),
            LocalDate.of(2026, 9, 12),
            LocalDate.of(2026, 12, 25),
        )
        days.forEach { assertEquals(DailyQuestion.PROMPT, DailyQuestion.forDay(it)) }
    }

    @Test
    fun the_question_does_not_change_during_the_day() {
        // It cannot shift under somebody who half-answered and came back.
        val day = LocalDate.of(2026, 9, 11)
        repeat(5) { assertEquals(DailyQuestion.forDay(day), DailyQuestion.forDay(day)) }
    }

    @Test
    fun every_day_of_a_year_lands_on_a_real_prompt() {
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val question = DailyQuestion.forDay(day)
            assertTrue("$day produced $question", question in DailyQuestion.QUESTIONS)
            day = day.plusDays(1)
        }
    }

    @Test
    fun the_prompt_asks_for_the_thing_the_field_accepts() {
        // The answer box is one line and its placeholder says "one word". A
        // prompt that invites a sentence is a prompt the screen will refuse.
        assertTrue(DailyQuestion.PROMPT.contains("one word"))
        assertTrue("a prompt is not a sentence", !DailyQuestion.PROMPT.contains("."))
    }
}
