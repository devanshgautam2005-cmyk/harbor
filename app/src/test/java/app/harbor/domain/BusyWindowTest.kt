package app.harbor.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BusyWindowTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private val lecture = BusyWindow(
        day = DayOfWeek.TUESDAY,
        start = LocalTime.of(9, 0),
        end = LocalTime.of(10, 30),
        label = "Marketing 101",
    )

    private fun at(day: DayOfWeek, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 9, 8, hour, minute, 0, 0, zone)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(day))

    @Test
    fun covers_a_time_inside_the_window() {
        assertTrue(lecture.covers(at(DayOfWeek.TUESDAY, 9, 30)))
    }

    @Test
    fun the_start_is_inside_and_the_end_is_not() {
        // Half-open, so back-to-back classes cannot both claim the same
        // minute — and a cue at exactly 10:30 belongs to whatever comes next.
        assertTrue(lecture.covers(at(DayOfWeek.TUESDAY, 9, 0)))
        assertFalse(lecture.covers(at(DayOfWeek.TUESDAY, 10, 30)))
    }

    @Test
    fun does_not_cover_the_same_time_on_another_day() {
        assertFalse(lecture.covers(at(DayOfWeek.WEDNESDAY, 9, 30)))
    }

    @Test
    fun does_not_cover_a_time_outside_the_window() {
        assertFalse(lecture.covers(at(DayOfWeek.TUESDAY, 8, 59)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun a_window_that_ends_before_it_starts_is_rejected() {
        BusyWindow(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(10, 0))
    }
}
