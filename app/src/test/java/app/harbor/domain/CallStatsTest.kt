package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CallStatsTest {

    private val mom: UUID = UUID.randomUUID()
    private val dad: UUID = UUID.randomUUID()
    private val now: Instant = Instant.parse("2026-09-11T10:00:00Z")

    private fun call(
        contactId: UUID = mom,
        minutes: Int? = 10,
        flower: FlowerKind? = FlowerKind.DAISY,
        resolution: Resolution = Resolution.CALLED,
    ) = LedgerEntry(
        id = UUID.randomUUID(),
        entryDate = LocalDate.of(2026, 9, 11),
        cueId = null,
        contactId = contactId,
        triggerSource = TriggerSource.WALKING_STOP,
        thresholdSnapshot = Thresholds.SUGGESTED,
        resolution = resolution,
        proposedTime = null,
        feedbackPulse = null,
        callMinutes = minutes,
        feeling = null,
        flower = flower,
        topic = null,
        occurredAt = now,
    )

    // --- usual length -----------------------------------------------------

    @Test
    fun no_calls_yet_means_no_usual_length() {
        assertNull(CallStats.usualMinutes(emptyList(), mom))
    }

    @Test
    fun averages_the_calls_with_that_person() {
        val entries = listOf(call(minutes = 10), call(minutes = 20), call(minutes = 15))
        assertEquals(15, CallStats.usualMinutes(entries, mom))
    }

    @Test
    fun ignores_calls_with_someone_else() {
        val entries = listOf(call(contactId = mom, minutes = 10), call(contactId = dad, minutes = 60))
        assertEquals(10, CallStats.usualMinutes(entries, mom))
    }

    @Test
    fun ignores_resolutions_that_were_not_calls() {
        // A reaction has no length, and counting it as a zero would drag the
        // stated average below what any real call has ever been.
        val entries = listOf(
            call(minutes = 20),
            call(minutes = null, resolution = Resolution.REACTED),
            call(minutes = null, resolution = Resolution.DISMISSED),
        )
        assertEquals(20, CallStats.usualMinutes(entries, mom))
    }

    @Test
    fun never_reports_a_usual_length_below_one_minute() {
        assertEquals(1, CallStats.usualMinutes(listOf(call(minutes = 0)), mom))
    }

    // --- dominant flower --------------------------------------------------

    @Test
    fun finds_the_flower_a_patch_is_mostly_made_of() {
        val entries = listOf(
            call(flower = FlowerKind.MARIGOLD),
            call(flower = FlowerKind.DAISY),
            call(flower = FlowerKind.MARIGOLD),
        )
        assertEquals(FlowerKind.MARIGOLD, CallStats.dominantFlower(entries, mom))
    }

    @Test
    fun an_empty_patch_has_no_dominant_flower() {
        assertNull(CallStats.dominantFlower(emptyList(), mom))
    }

    // --- formatting -------------------------------------------------------

    @Test
    fun formats_durations_the_way_a_person_would_say_them() {
        assertEquals("40 min", CallStats.formatDuration(40))
        assertEquals("1 hr", CallStats.formatDuration(60))
        assertEquals("1 hr 20 min", CallStats.formatDuration(80))
        assertEquals("a little while", CallStats.formatDuration(null))
    }

    // --- blooms -----------------------------------------------------------

    @Test
    fun a_short_call_is_still_a_whole_flower() {
        // The floor is what stops the garden becoming a ranking of calls by
        // length, which is the scoring this design refuses to do. A
        // one-minute call already sits well clear of it.
        assertEquals(0.74, Flowers.bloomScale(1), 0.001)
        assertEquals(0.68, Flowers.bloomScale(0), 0.001)
        assert(Flowers.bloomScale(1) > 0.68) { "a real call outgrows the floor" }
    }

    @Test
    fun a_longer_call_opens_a_fuller_bloom_up_to_a_ceiling() {
        val ten = Flowers.bloomScale(10)
        val forty = Flowers.bloomScale(40)
        assert(forty > ten) { "40 minutes should open wider than 10" }
        assertEquals(1.45, Flowers.bloomScale(600), 0.001)
    }

    @Test
    fun an_unknown_length_blooms_like_a_typical_call() {
        assertEquals(Flowers.bloomScale(8), Flowers.bloomScale(null), 0.001)
    }

    // --- suggestions ------------------------------------------------------

    @Test
    fun the_feelings_own_flower_is_offered_first() {
        val suggestions = Flowers.suggestions(Feeling.WARM)
        assertEquals(FlowerKind.MARIGOLD, suggestions.first())
        assertEquals(4, suggestions.size)
    }

    @Test
    fun suggestions_never_repeat_the_primary() {
        for (feeling in Feeling.entries) {
            val suggestions = Flowers.suggestions(feeling)
            assertEquals(suggestions.size, suggestions.toSet().size)
        }
    }

    @Test
    fun every_flower_kind_has_a_spec() {
        for (kind in FlowerKind.entries) {
            assertEquals(kind, Flowers.spec(kind).kind)
        }
    }
}
