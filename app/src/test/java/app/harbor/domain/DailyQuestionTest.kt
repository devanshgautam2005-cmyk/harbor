package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Expected values taken from running the prototype's own `gameForDay` under
 * node, so the question of the day is the same on both.
 */
class DailyQuestionTest {

    @Test
    fun the_question_of_the_day_matches_the_prototype() {
        assertEquals("One word for how today felt?", DailyQuestion.forDay(LocalDate.of(2026, 9, 11)))
        assertEquals(
            "Cat person, dog person, or neither?",
            DailyQuestion.forDay(LocalDate.of(2026, 9, 12)),
        )
        assertEquals(
            "Early bird or night owl, honestly?",
            DailyQuestion.forDay(LocalDate.of(2026, 1, 1)),
        )
        assertEquals("Rain or sunshine today?", DailyQuestion.forDay(LocalDate.of(2026, 12, 25)))
    }

    @Test
    fun the_question_does_not_change_during_the_day() {
        // It is chosen from the date, not at random, so it cannot shift under
        // someone who half-answered and came back.
        val day = LocalDate.of(2026, 9, 11)
        repeat(5) { assertEquals(DailyQuestion.forDay(day), DailyQuestion.forDay(day)) }
    }

    @Test
    fun every_day_of_a_year_lands_on_a_real_question() {
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val question = DailyQuestion.forDay(day)
            assert(question in DailyQuestion.QUESTIONS) { "$day produced $question" }
            day = day.plusDays(1)
        }
    }
}
