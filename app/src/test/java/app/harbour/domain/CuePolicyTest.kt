package app.harbour.domain

import app.harbour.domain.CuePolicy.Decision
import app.harbour.domain.CuePolicy.Reason
import app.harbour.domain.CuePolicy.Signal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class CuePolicyTest {

    private val now: Instant = Instant.parse("2026-09-10T17:30:00Z")
    private val today: LocalDate = now.atZone(ZoneOffset.UTC).toLocalDate()
    private val thresholds = UserThresholds.SUGGESTED

    /** A walk that just ended, long enough and settled enough to fire. */
    private fun walkSignal(
        activeMinutes: Int = 20,
        stillFor: Duration = CuePolicy.SETTLE.plusSeconds(30),
    ) = Signal(
        source = TriggerSource.WALKING_STOP,
        activeMinutes = activeMinutes,
        stillSince = now.minus(stillFor),
    )

    private fun entry(
        resolution: Resolution = Resolution.DISMISSED,
        occurredAt: Instant = now.minus(Duration.ofHours(6)),
        proposedTime: Instant? = null,
    ) = LedgerEntry(
        clientId = UUID.randomUUID(),
        entryDate = today,
        triggerSource = TriggerSource.WALKING_STOP,
        thresholdSnapshot = thresholds,
        resolution = resolution,
        proposedTime = proposedTime,
        feedbackPulse = null,
        rewardShown = RewardShown.READOUT,
        occurredAt = occurredAt,
    )

    private fun decide(
        signal: Signal = walkSignal(),
        thresholds: UserThresholds = this.thresholds,
        today: List<LedgerEntry> = emptyList(),
        lastCueAt: Instant? = today.maxOfOrNull { it.occurredAt },
    ) = CuePolicy.decide(signal, thresholds, today, lastCueAt, now)

    // --- the happy path ---------------------------------------------------

    @Test
    fun `fires on a settled walk past the threshold with a clean day`() {
        assertEquals(Decision.Fire, decide())
    }

    // --- stage 2: threshold ----------------------------------------------

    @Test
    fun `holds when the walk was shorter than the user's threshold`() {
        assertEquals(
            Decision.Hold(Reason.BELOW_THRESHOLD),
            decide(walkSignal(activeMinutes = thresholds.walkingMinutes - 1)),
        )
    }

    @Test
    fun `fires when the walk exactly meets the threshold`() {
        assertEquals(
            Decision.Fire,
            decide(walkSignal(activeMinutes = thresholds.walkingMinutes)),
        )
    }

    @Test
    fun `a session signal is measured against the session threshold`() {
        val signal = Signal(
            source = TriggerSource.SESSION_END,
            // Past the walking threshold, short of the session one. If the
            // policy reached for the wrong field this would fire.
            activeMinutes = thresholds.sessionMinutes - 1,
            stillSince = now.minus(CuePolicy.SETTLE.plusSeconds(30)),
        )
        assertEquals(Decision.Hold(Reason.BELOW_THRESHOLD), decide(signal))
    }

    // --- stage 3: suppression --------------------------------------------

    @Test
    fun `holds once the user has already called today`() {
        assertEquals(
            Decision.Hold(Reason.ALREADY_CALLED_TODAY),
            decide(today = listOf(entry(resolution = Resolution.CALLED))),
        )
    }

    @Test
    fun `holds at the daily cap`() {
        val old = now.minus(Duration.ofHours(9))
        val entries = List(thresholds.dailyCap) { entry(occurredAt = old) }
        assertEquals(Decision.Hold(Reason.DAILY_CAP_REACHED), decide(today = entries))
    }

    @Test
    fun `a daily cap of zero suppresses everything`() {
        assertEquals(
            Decision.Hold(Reason.DAILY_CAP_REACHED),
            decide(thresholds = thresholds.copy(dailyCap = 0)),
        )
    }

    @Test
    fun `holds inside the cooldown window`() {
        val recent = now.minus(Duration.ofMinutes(thresholds.cooldownMinutes - 1L))
        assertEquals(
            Decision.Hold(Reason.IN_COOLDOWN),
            decide(today = listOf(entry(occurredAt = recent))),
        )
    }

    @Test
    fun `fires once the cooldown has cleared`() {
        val cleared = now.minus(Duration.ofMinutes(thresholds.cooldownMinutes + 1L))
        assertEquals(Decision.Fire, decide(today = listOf(entry(occurredAt = cleared))))
    }

    @Test
    fun `cooldown survives midnight, when today is empty but a cue just fired`() {
        // The 23:55 / 00:05 case. Before lastCueAt was passed separately this
        // fired, because the cooldown was derived from today's entries and
        // today had just rolled over.
        assertEquals(
            Decision.Hold(Reason.IN_COOLDOWN),
            decide(today = emptyList(), lastCueAt = now.minus(Duration.ofMinutes(10))),
        )
    }

    @Test
    fun `no previous cue anywhere means no cooldown`() {
        assertEquals(Decision.Fire, decide(today = emptyList(), lastCueAt = null))
    }

    @Test
    fun `cooldown is measured from the most recent cue, not the first`() {
        val entries = listOf(
            entry(occurredAt = now.minus(Duration.ofHours(9))),
            entry(occurredAt = now.minus(Duration.ofMinutes(5))),
        )
        assertEquals(
            Decision.Hold(Reason.IN_COOLDOWN),
            decide(thresholds = thresholds.copy(dailyCap = 5), today = entries),
        )
    }

    @Test
    fun `holds while a proposed time is still ahead of us`() {
        val pending = entry(
            resolution = Resolution.PROPOSED_LATER,
            occurredAt = now.minus(Duration.ofHours(8)),
            proposedTime = now.plus(Duration.ofHours(2)),
        )
        assertEquals(
            Decision.Hold(Reason.REMINDER_PENDING),
            decide(thresholds = thresholds.copy(dailyCap = 5), today = listOf(pending)),
        )
    }

    @Test
    fun `a proposed time that has passed no longer suppresses`() {
        val lapsed = entry(
            resolution = Resolution.PROPOSED_LATER,
            occurredAt = now.minus(Duration.ofHours(8)),
            proposedTime = now.minus(Duration.ofHours(1)),
        )
        assertEquals(
            Decision.Fire,
            decide(thresholds = thresholds.copy(dailyCap = 5), today = listOf(lapsed)),
        )
    }

    // --- stage 4: kairos --------------------------------------------------

    @Test
    fun `holds while the user has only just stopped`() {
        assertEquals(
            Decision.Hold(Reason.TRANSITION_UNSETTLED),
            decide(walkSignal(stillFor = CuePolicy.SETTLE.minusSeconds(1))),
        )
    }

    @Test
    fun `fires the moment the settle window is met`() {
        assertEquals(Decision.Fire, decide(walkSignal(stillFor = CuePolicy.SETTLE)))
    }

    // --- manual ------------------------------------------------------------

    @Test
    fun `a manual request ignores threshold, suppression and settling`() {
        val signal = Signal(
            source = TriggerSource.MANUAL,
            activeMinutes = 0,
            stillSince = now,
        )
        val busyDay = listOf(
            entry(resolution = Resolution.CALLED, occurredAt = now.minus(Duration.ofMinutes(2))),
        )
        assertEquals(Decision.Fire, decide(signal, today = busyDay))
    }

    // --- ordering ----------------------------------------------------------

    @Test
    fun `threshold is reported before suppression when both would hold`() {
        // The handoff orders the stages 2 then 3, and the hold reason is the
        // main thing we will have to debug from, so the order is load-bearing.
        assertEquals(
            Decision.Hold(Reason.BELOW_THRESHOLD),
            decide(
                walkSignal(activeMinutes = 1),
                today = listOf(entry(resolution = Resolution.CALLED)),
            ),
        )
    }
}
