package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * What counts as room for a call.
 *
 * The card this feeds is the most inviting thing on the schedule screen, so
 * the arithmetic behind it has to be right: a window that is not really there
 * is an invitation to interrupt yourself.
 */
class WindowsTest {

    private val mon = DayOfWeek.MONDAY

    private fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)

    private fun busy(from: LocalTime, to: LocalTime, day: DayOfWeek = mon) =
        BusyWindow(day, from, to, "SECRET_LABEL")

    @Test
    fun `an empty week is one long window`() {
        val free = Windows.free(emptyList(), mon)
        assertEquals(1, free.size)
        assertEquals(Windows.DAY_START, free[0].start)
        assertEquals(Windows.DAY_END, free[0].end)
    }

    @Test
    fun `a block in the middle leaves a window on each side`() {
        val free = Windows.free(listOf(busy(at(10), at(12))), mon)
        assertEquals(2, free.size)
        assertEquals(Windows.DAY_START to at(10), free[0].start to free[0].end)
        assertEquals(at(12) to Windows.DAY_END, free[1].start to free[1].end)
    }

    @Test
    fun `two classes that overlap do not invent a window between them`() {
        val free = Windows.free(listOf(busy(at(10), at(12)), busy(at(11), at(13))), mon)
        assertEquals(2, free.size)
        assertEquals(at(13), free[1].start)
    }

    @Test
    fun `nor do two that run straight into each other`() {
        val free = Windows.free(listOf(busy(at(10), at(12)), busy(at(12), at(14))), mon)
        assertEquals(2, free.size)
        assertEquals(at(14), free[1].start)
    }

    @Test
    fun `a block wholly inside another is swallowed`() {
        val free = Windows.free(listOf(busy(at(9), at(15)), busy(at(11), at(12))), mon)
        assertEquals(2, free.size)
        assertEquals(at(9), free[0].end)
        assertEquals(at(15), free[1].start)
    }

    @Test
    fun `ten minutes between classes is not a window`() {
        val free = Windows.free(listOf(busy(at(10), at(12)), busy(at(12, 10), at(14))), mon)
        assertTrue(free.none { it.start == at(12) })
    }

    @Test
    fun `another day's classes do not shorten this one`() {
        val free = Windows.free(listOf(busy(at(10), at(12), DayOfWeek.TUESDAY)), mon)
        assertEquals(1, free.size)
    }

    @Test
    fun `a day booked end to end has no window at all`() {
        val free = Windows.free(listOf(busy(Windows.DAY_START, Windows.DAY_END)), mon)
        assertTrue(free.isEmpty())
    }

    @Test
    fun `blocks outside the callable day are clipped away`() {
        // A 6am gym slot does not create a window before breakfast.
        val free = Windows.free(listOf(busy(at(6), at(7))), mon)
        assertEquals(1, free.size)
        assertEquals(Windows.DAY_START, free[0].start)
    }

    // --- the card's own question --------------------------------------------

    @Test
    fun `the next window is the roomiest one still ahead`() {
        // 08:00-10:00 is behind us; 12:00-14:00 beats 14:30-15:00.
        val blocks = listOf(busy(at(10), at(12)), busy(at(14), at(14, 30)), busy(at(15), at(22)))
        val next = Windows.next(blocks, mon, now = at(11))
        assertEquals(at(12) to at(14), next?.start to next?.end)
    }

    @Test
    fun `a window already under way starts from now, not from its beginning`() {
        val next = Windows.next(listOf(busy(at(9), at(10))), mon, now = at(13))
        assertEquals(at(13), next?.start)
    }

    @Test
    fun `a spent day offers nothing`() {
        assertNull(Windows.next(emptyList(), mon, now = at(23)))
        assertNull(Windows.next(listOf(busy(at(8), at(22))), mon, now = at(9)))
    }

    @Test
    fun `minutes are what the card prints`() {
        assertEquals(50, Windows.Window(at(20, 40), at(21, 30)).minutes)
    }

    // --- how the card says it -----------------------------------------------

    @Test
    fun `under an hour is counted in minutes`() {
        assertEquals("50 unhurried minutes", Windows.phrase(Windows.Window(at(20, 40), at(21, 30))))
    }

    @Test
    fun `a whole free day is not three hundred and seventy three minutes`() {
        // The bug this exists for: with nothing blocked, the honest arithmetic
        // answer is absurd as copy.
        val all = Windows.Window(at(15, 46), at(22))
        assertEquals("6 unhurried hours", Windows.phrase(all))
    }

    @Test
    fun `one hour reads as one hour`() {
        assertEquals("an unhurried hour", Windows.phrase(Windows.Window(at(9), at(10))))
    }

    @Test
    fun `an hour and fifty minutes does not present itself as one`() {
        assertEquals("2 unhurried hours", Windows.phrase(Windows.Window(at(9), at(10, 50))))
    }

    @Test
    fun `the boundary between minutes and hours is exactly an hour`() {
        assertEquals("59 unhurried minutes", Windows.phrase(Windows.Window(at(9), at(9, 59))))
        assertEquals("an unhurried hour", Windows.phrase(Windows.Window(at(9), at(10))))
    }
}
