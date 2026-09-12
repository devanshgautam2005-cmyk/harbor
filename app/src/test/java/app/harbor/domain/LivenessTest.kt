package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Whether the app can tell a quiet week from a dead one.
 *
 * This exists because of a hole in the study rather than a bug in the code: a
 * participant with no cues is uninterpretable unless the app recorded its own
 * pulse while the week was happening. The thresholds below are a judgement,
 * which is exactly why they are pinned here.
 */
class LivenessTest {

    private val now: Instant = Instant.parse("2026-09-12T18:00:00Z")

    private fun ago(d: Duration) = now.minus(d)

    @Test
    fun `never having heard from the phone is its own state`() {
        assertEquals(Liveness.State.NEVER, Liveness.state(null, now))
        assertEquals("not yet", Liveness.phrase(null, now))
    }

    @Test
    fun `a phone that spoke recently is healthy`() {
        assertEquals(
            Liveness.State.HEALTHY,
            Liveness.state(ago(Duration.ofMinutes(20)), now),
        )
    }

    @Test
    fun `a night of silence is not an alarm`() {
        // Becoming still at bedtime is one transition and then there is
        // nothing to report until morning. Eight hours must stay healthy or
        // every participant sees a warning over breakfast.
        assertEquals(
            Liveness.State.HEALTHY,
            Liveness.state(ago(Duration.ofHours(8)), now),
        )
    }

    @Test
    fun `longer than a night is worth saying out loud`() {
        assertEquals(
            Liveness.State.QUIET_TOO_LONG,
            Liveness.state(ago(Duration.ofHours(13)), now),
        )
        assertEquals(
            Liveness.State.QUIET_TOO_LONG,
            Liveness.state(ago(Duration.ofDays(3)), now),
        )
    }

    @Test
    fun `the boundary is exactly where it says it is`() {
        assertEquals(Liveness.State.QUIET_TOO_LONG, Liveness.state(ago(Liveness.QUIET), now))
        assertEquals(
            Liveness.State.HEALTHY,
            Liveness.state(ago(Liveness.QUIET.minusMinutes(1)), now),
        )
    }

    @Test
    fun `the phrasing gets vaguer as it gets older`() {
        assertEquals("just now", Liveness.phrase(ago(Duration.ofSeconds(30)), now))
        assertEquals("20 minutes ago", Liveness.phrase(ago(Duration.ofMinutes(20)), now))
        assertEquals("an hour ago", Liveness.phrase(ago(Duration.ofMinutes(75)), now))
        assertEquals("5 hours ago", Liveness.phrase(ago(Duration.ofHours(5)), now))
        assertEquals("yesterday", Liveness.phrase(ago(Duration.ofHours(30)), now))
        assertEquals("4 days ago", Liveness.phrase(ago(Duration.ofDays(4)), now))
    }

    @Test
    fun `a clock that went backwards does not produce nonsense`() {
        // Timezone changes and NTP corrections both do this.
        assertEquals("just now", Liveness.phrase(now.plusSeconds(600), now))
        assertEquals(Liveness.State.HEALTHY, Liveness.state(now.plusSeconds(600), now))
    }
}
