package app.harbor.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The domain model for the Slack Tide pipeline.
 *
 * These types mirror `backend/supabase/migrations/0001_init.sql` deliberately.
 * If you change a name or a variant here, change it there in the same PR —
 * the sync layer maps between them by name.
 *
 * Nothing in this file touches Android. That is on purpose: the pipeline's
 * decisions are the risky part of the product, and pure code is the only kind
 * we can actually unit-test.
 */

/** What woke the pipeline up. Stage 1. */
enum class TriggerSource {
    /** A walking bout ended. The only source shipping in v0.1. */
    WALKING_STOP,

    /** A watched app session ended. v0.2 — see ADR-005. */
    SESSION_END,

    /** The user opened Harbor and asked for a prompt themselves. */
    MANUAL,
}

/** What the user chose at stage 6. No option is a default; none is a failure. */
enum class Resolution {
    CALLED,
    REACTED,
    PROPOSED_LATER,
    DISMISSED,
}

/** The one-tap pulse at stage 8. Null when the user skipped it. */
enum class FeedbackPulse {
    GOOD_TIME,
    BAD_TIME,
}

/** Which flavour of reward stage 7 showed. Varying, but always guaranteed. */
enum class RewardShown {
    READOUT,
    JAR_FILL,
    WRAPPED_CLIP,
}

/**
 * User-calibrated sensitivity. Ship [SUGGESTED] as a starting point and let
 * the user move it — never lock these as product defaults. Handoff, section 7.
 */
data class UserThresholds(
    val walkingMinutes: Int,
    val sessionMinutes: Int,
    /** Hard ceiling on cues per day. */
    val dailyCap: Int,
    /** Minimum gap between two cues. */
    val cooldownMinutes: Int,
) {
    init {
        require(walkingMinutes in 1..240) { "walkingMinutes out of range: $walkingMinutes" }
        require(sessionMinutes in 1..240) { "sessionMinutes out of range: $sessionMinutes" }
        require(dailyCap in 0..10) { "dailyCap out of range: $dailyCap" }
        require(cooldownMinutes in 0..1440) { "cooldownMinutes out of range: $cooldownMinutes" }
    }

    companion object {
        /**
         * The suggested starting point, matching the column defaults in
         * `0001_init.sql`. Study question 3 is how far people move away from
         * this — see docs/03-week-one-study.md.
         */
        val SUGGESTED = UserThresholds(
            walkingMinutes = 12,
            sessionMinutes = 20,
            dailyCap = 2,
            cooldownMinutes = 180,
        )
    }
}

/**
 * The person being called. v0.1 assumes an ordinary cellular number: the
 * parent installs nothing. See ADR-002.
 */
data class Contact(
    val label: String,
    /** E.164, e.g. +919876543210. */
    val phoneE164: String,
    /** Uri of the chosen cue sound, resolved on-device. Null = app default. */
    val cueSoundRef: String? = null,
)

/**
 * Written at stage 9. The device generates [clientId], so re-uploading after
 * a failed sync is an upsert rather than a duplicate row. See ADR-003.
 */
data class LedgerEntry(
    val clientId: UUID,
    val entryDate: LocalDate,
    val triggerSource: TriggerSource,
    /** The thresholds in force when this cue fired, not the current ones. */
    val thresholdSnapshot: UserThresholds,
    val resolution: Resolution,
    /** Set if and only if [resolution] is [Resolution.PROPOSED_LATER]. */
    val proposedTime: Instant?,
    val feedbackPulse: FeedbackPulse?,
    val rewardShown: RewardShown?,
    /** When the cue fired on the device — not when it synced. */
    val occurredAt: Instant,
) {
    init {
        require((resolution == Resolution.PROPOSED_LATER) == (proposedTime != null)) {
            "proposedTime must be set for PROPOSED_LATER and null otherwise"
        }
    }
}
