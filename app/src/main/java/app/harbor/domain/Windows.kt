package app.harbor.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

/**
 * The gaps between the things you marked busy.
 *
 * The specimen sheet's schedule leads with a card it calls "a little window,
 * together" — a stretch of evening set in large serif, as the one thing on
 * that screen worth looking at. This computes the honest version of it.
 *
 * ## Why it says "you" and never "together"
 *
 * The sheet's card implies both people's calendars. Harbor cannot know the
 * second one and must never look as though it does: the parent installs
 * nothing and is never contacted (ADR-007), and no location or calendar of
 * theirs exists anywhere in this app. So this is *your* window — a time you
 * are free — and the copy that renders it has to stay on that side of the
 * line. A card that quietly implied Mum's evening was being read would be the
 * single most damaging thing this design could do.
 *
 * Everything here is derived from [BusyWindow]s the user typed in themselves.
 * Nothing is sensed, stored or sent.
 */
object Windows {

    /** Before this, nobody wants a call. */
    val DAY_START: LocalTime = LocalTime.of(8, 0)

    /** After this, neither does anybody else. */
    val DAY_END: LocalTime = LocalTime.of(22, 0)

    /** Shorter than this is a gap between classes, not room for a call. */
    val LEAST: Duration = Duration.ofMinutes(20)

    data class Window(val start: LocalTime, val end: LocalTime) {
        init {
            require(start < end) { "a window must end after it starts" }
        }

        val minutes: Int get() = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
    }

    /**
     * Every stretch of [day] long enough to matter that is not marked busy.
     *
     * Overlapping blocks are merged first, so two classes that run into each
     * other do not produce a phantom window between them.
     */
    fun free(
        blocks: List<BusyWindow>,
        day: DayOfWeek,
        from: LocalTime = DAY_START,
        to: LocalTime = DAY_END,
    ): List<Window> {
        if (from >= to) return emptyList()

        val clamped = blocks
            .filter { it.day == day }
            .map { maxOf(it.start, from) to minOf(it.end, to) }
            .filter { it.first < it.second }
            .sortedBy { it.first }

        val merged = mutableListOf<Pair<LocalTime, LocalTime>>()
        for ((start, end) in clamped) {
            val last = merged.lastOrNull()
            if (last != null && start <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, end)
            } else {
                merged.add(start to end)
            }
        }

        val out = mutableListOf<Window>()
        var cursor = from
        for ((start, end) in merged) {
            if (cursor < start) out.add(Window(cursor, start))
            cursor = maxOf(cursor, end)
        }
        if (cursor < to) out.add(Window(cursor, to))

        return out.filter { it.minutes >= LEAST.toMinutes() }
    }

    /**
     * The roomiest window still ahead of [now], or null if the day is spent.
     *
     * Longest rather than soonest: the card is an invitation, and ten minutes
     * before a lecture is not one.
     */
    fun next(
        blocks: List<BusyWindow>,
        day: DayOfWeek,
        now: LocalTime,
        from: LocalTime = DAY_START,
        to: LocalTime = DAY_END,
    ): Window? = free(blocks, day, maxOf(now, from), to).maxByOrNull { it.minutes }
}
