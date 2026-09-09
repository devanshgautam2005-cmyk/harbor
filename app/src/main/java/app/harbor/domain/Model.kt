package app.harbor.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The domain model.
 *
 * These types mirror the Postgres schema in `backend/supabase/migrations/`
 * deliberately, and they follow the UI prototype (harvest-pulse) wherever the
 * two disagreed — see docs/02-ui-reconciliation.md for the walkthrough. If you
 * change a name or a variant here, change it in the schema in the same PR: the
 * sync layer maps between them by name.
 *
 * Nothing in this file touches Android. That is on purpose.
 */

/** What woke the pipeline up. Stage 1. */
enum class TriggerSource {
    /** A walking bout ended. The only sensed source in v0.1. */
    WALKING_STOP,

    /** A watched app session ended. Not sensed yet — see ADR-005. */
    SESSION_END,

    /** Raised by the Dispatch content pipeline rather than by sensing. */
    DISPATCH,

    /** The user opened Harbor and asked for a prompt themselves. */
    MANUAL,
}

/**
 * What the user chose at stage 6. No option is a default, and none is a
 * failure — dismissing is a legitimate answer, not a missed one.
 */
enum class Resolution {
    CALLED,
    REACTED,

    /** Sent a note. Distinct from a reaction, and counts toward the Jar. */
    MESSAGE,

    PROPOSED_LATER,
    DISMISSED;

    /**
     * Whether this counts as having reached the other person today.
     *
     * Includes reactions and notes, not just calls. That is a product
     * judgement carried over from the prototype: a heart sent on purpose is
     * connection, and treating it as one is what keeps the app from nagging
     * someone who already did the thing.
     */
    val isConnection: Boolean
        get() = this == CALLED || this == REACTED || this == MESSAGE
}

/** The one-tap pulse at stage 8. Null when the user skipped it. */
enum class FeedbackPulse { GOOD_TIME, BAD_TIME }

/** Which flavour of reward stage 7 showed. Varying, but always guaranteed. */
enum class RewardShown { READOUT, JAR_FILL, WRAPPED_CLIP }

/** A contact that cannot be dialled reads differently in the UI. */
enum class ContactKind { PERSON, GROUP }

/** The global default cue sound. A contact may override it. */
enum class CueSound { CHIME, SOFT, SILENT }

/** What the user counts as a connected day, for the Jar. */
enum class Minimum {
    /** A call, a note, a reaction or a plan all count. */
    ANY,

    /** Only a completed call counts, however short. */
    CALL,
}

/**
 * The four numbers that decide whether a cue fires. Snapshotted onto every
 * ledger entry, so a later recalibration cannot rewrite the past.
 */
data class Thresholds(
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
         * `0001_init.sql`. Ship it as a suggestion and let the user move it —
         * never lock it as a product default. Handoff, section 7.
         *
         * Study question 3 is how far people move away from this.
         */
        val SUGGESTED = Thresholds(
            walkingMinutes = 12,
            sessionMinutes = 20,
            dailyCap = 2,
            cooldownMinutes = 180,
        )
    }
}

/** Everything the user can change about how Harbor behaves. */
data class UserSettings(
    val thresholds: Thresholds = Thresholds.SUGGESTED,

    /**
     * The real opt-out. Off until the user turns it on, behind a privacy
     * explainer. Also the answer to the handoff's open "degraded mode"
     * question: if activity permission is refused, cues are off and say so —
     * nothing degrades silently.
     */
    val cuesEnabled: Boolean = false,

    /** Null is a real state: the user has not decided yet, and the UI says so. */
    val minimum: Minimum? = null,

    val sound: CueSound = CueSound.CHIME,

    val reducedMotion: Boolean = false,
)

/**
 * Someone worth calling. v0.1 assumes an ordinary cellular number: the parent
 * installs nothing (ADR-002, ADR-007).
 */
data class Contact(
    val id: UUID,
    val label: String,
    /** E.164, e.g. +919876543210. Null for a group, which cannot be dialled. */
    val phoneE164: String?,
    val kind: ContactKind = ContactKind.PERSON,
    /** Overrides [UserSettings.sound] for this person. Null = use the default. */
    val cueSoundRef: String? = null,
) {
    init {
        require(kind == ContactKind.GROUP || phoneE164 != null) {
            "a person needs a number to call"
        }
    }
}

/**
 * A cue that fired, whether or not the user answered it.
 *
 * Separate from [LedgerEntry] because the daily cap counts cues, and because a
 * cue nobody engaged with is exactly the signal the study wants — no
 * resolution-based count can see it.
 */
data class Cue(
    val clientId: UUID,
    /** The device's local day. */
    val firedDate: LocalDate,
    val triggerSource: TriggerSource,
    val firedAt: Instant,
)

/**
 * Written at stage 9. The device generates [clientId], so re-uploading after a
 * failed sync is an upsert rather than a duplicate row. See ADR-003.
 */
data class LedgerEntry(
    val clientId: UUID,
    val entryDate: LocalDate,
    /** The cue this resolved. Null when the user started the moment themselves. */
    val cueId: UUID?,
    /** Who it was with. Null if the contact has since been deleted. */
    val contactId: UUID?,
    val triggerSource: TriggerSource,
    /** The thresholds in force when this cue fired, not the current ones. */
    val thresholdSnapshot: Thresholds,
    val resolution: Resolution,
    /** Set if and only if [resolution] is [Resolution.PROPOSED_LATER]. */
    val proposedTime: Instant?,
    /**
     * Whether a proposed-later reminder has been dealt with. Stored rather
     * than inferred from [proposedTime] having passed, which guesses wrong
     * whenever the user acts early or late.
     */
    val reminderDone: Boolean = false,
    val feedbackPulse: FeedbackPulse?,
    val rewardShown: RewardShown?,
    /** When the moment happened on the device — not when it synced. */
    val occurredAt: Instant,
) {
    init {
        require((resolution == Resolution.PROPOSED_LATER) == (proposedTime != null)) {
            "proposedTime must be set for PROPOSED_LATER and null otherwise"
        }
        require(!reminderDone || resolution == Resolution.PROPOSED_LATER) {
            "reminderDone only means anything for PROPOSED_LATER"
        }
    }
}
