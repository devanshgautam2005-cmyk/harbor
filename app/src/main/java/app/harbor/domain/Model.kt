package app.harbor.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
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

    /** Left from a note or snapshot the user sent. */
    NOTE,

    /** Left from the daily question. */
    GAME,

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

    /** Played the day's family game. Counts for the Jar, but see below. */
    PLAYED,

    PROPOSED_LATER,
    DISMISSED,

    /**
     * They went to call and no conversation happened.
     *
     * Changed their mind at the dialer, or nobody picked up. The row is
     * written as [CALLED] the moment the dialer opens — before anything is
     * known — because a call that happened must be recorded even if the user
     * never comes back to say how it went. That trade means the only way
     * `called` stays honest is if there is a way to say no afterwards, and
     * this is it.
     *
     * Kept separate from [DISMISSED] on purpose: dismissing is declining the
     * cue, this is accepting it and coming away with nothing. For the study
     * those are different answers to "what happened to a cue", and collapsing
     * them would hide the more interesting one.
     */
    NOT_REACHED;

    /**
     * Whether this counts as having reached the other person today.
     *
     * Includes reactions and notes, not just calls. That is a product
     * judgement carried over from the prototype: a heart sent on purpose is
     * connection, and treating it as one is what keeps the app from nagging
     * someone who already did the thing.
     *
     * [PLAYED] is deliberately excluded, matching the prototype: playing the
     * daily game is a nice thing to have done, but nobody on the other end
     * heard from you.
     */
    val isConnection: Boolean
        get() = this == CALLED || this == REACTED || this == MESSAGE
}

/** The one-tap pulse at stage 8. Null when the user skipped it. */
enum class FeedbackPulse { GOOD_TIME, BAD_TIME }

/**
 * How a call left the user feeling, asked once afterwards.
 *
 * This is the reward, and it is also the input to it: each feeling grows a
 * particular flower in the garden, so answering honestly is what makes the
 * garden a record of the calls rather than a scoreboard of them.
 */
enum class Feeling(val flower: FlowerKind) {
    LIGHT(FlowerKind.COSMOS),
    WARM(FlowerKind.MARIGOLD),
    STEADY(FlowerKind.DAISY),
    TENDER(FlowerKind.POPPY),
}

/**
 * What a call becomes.
 *
 * Eight kinds, chosen by [Feeling] and by how long the call ran. The garden is
 * the reward surface — there is no score, no streak, and nothing that can be
 * lost; a flower that grew stays grown.
 */
enum class FlowerKind {
    DAISY, MARIGOLD, COSMOS, POPPY, TULIP, BLUEBELL, ASTER, SUNFLOWER,
}

/**
 * How life feels at the moment, on a scale the user sets themselves.
 *
 * Replaces the earlier "season". Deliberately weather rather than a rating:
 * weather is something that happens to you and passes, which is a kinder
 * frame for a hard week than a number would be.
 */
enum class Weather { CLEAR, BRIGHT, CLOUDY, RAIN, STORM }

/** A contact that cannot be dialled reads differently in the UI. */
enum class ContactKind { PERSON, GROUP }

/** The colour a person is drawn in, across the garden and their avatar. */
enum class Tone { GREEN, GOLD, ORANGE, SKY }

/** The global default cue sound. A contact may override it. */
enum class CueSound { CHIME, SOFT, SILENT }

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
        // Ranges match the prototype's settings form and the CHECK
        // constraints in 0001_init.sql, so a value one layer accepts can
        // never be rejected by another.
        require(walkingMinutes in 1..120) { "walkingMinutes out of range: $walkingMinutes" }
        require(sessionMinutes in 1..180) { "sessionMinutes out of range: $sessionMinutes" }
        require(dailyCap in 1..10) { "dailyCap out of range: $dailyCap" }
        require(cooldownMinutes in 1..1440) { "cooldownMinutes out of range: $cooldownMinutes" }
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
            walkingMinutes = 10,
            sessionMinutes = 20,
            dailyCap = 2,
            cooldownMinutes = 120,
        )
    }
}

/** Everything the user can change about how Harbor behaves. */
data class UserSettings(
    /** What Harbor calls the user. Greeted by it on the home screen. */
    val name: String = "",

    val thresholds: Thresholds = Thresholds.SUGGESTED,

    /**
     * The real opt-out. Off until the user turns it on, behind a privacy
     * explainer. Also the answer to the handoff's open "degraded mode"
     * question: if activity permission is refused, cues are off and say so —
     * nothing degrades silently.
     */
    val cuesEnabled: Boolean = false,

    val sound: CueSound = CueSound.CHIME,

    /**
     * How life feels at the moment. The user sets it; nothing infers it.
     *
     * Weather rather than a rating, because weather happens to you and
     * passes — a kinder frame for a hard week than a number would be.
     */
    val weather: Weather = Weather.CLEAR,

    val reducedMotion: Boolean = false,
)

/**
 * A recurring block when the user is not reachable — a class, a lab, a shift.
 *
 * Weekly rather than dated, because that is the shape a timetable actually
 * has. A one-off engagement is not worth modelling: the cue is capped and
 * dismissible, and being asked once during an unusual afternoon costs almost
 * nothing.
 *
 * Deliberately independent of where the times came from. They might be typed
 * in, read from the device calendar, or one day pulled from a campus system —
 * the policy does not care, and keeping it that way is what stops a data
 * source from becoming an architectural commitment.
 */
data class BusyWindow(
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    /** "Marketing 101", or null. Never leaves the device. */
    val label: String? = null,
) {
    init {
        require(start < end) { "a busy window must end after it starts" }
    }

    fun covers(at: ZonedDateTime): Boolean =
        at.dayOfWeek == day && at.toLocalTime() >= start && at.toLocalTime() < end
}

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
    val tone: Tone = Tone.GREEN,

    /** Overrides [UserSettings.sound] for this person. Null = use the default. */
    val cueSoundRef: String? = null,

    /**
     * A picked photo shown on the cue surface, as a device-local URI.
     *
     * Their face is half of what makes the cue land as *them* rather than as
     * an app (ADR-009). Picked with the system photo picker rather than read
     * from their contact entry, so it costs no permission.
     */
    val photoRef: String? = null,
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
    val id: UUID,
    /** The device's local day. */
    val firedDate: LocalDate,
    val triggerSource: TriggerSource,
    val firedAt: Instant,
)

/**
 * Written at stage 9. The device generates [id], so re-uploading after a
 * failed sync is an upsert rather than a duplicate row. See ADR-003.
 */
data class LedgerEntry(
    val id: UUID,
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

    /**
     * How long the call ran, in minutes. Null for anything that was not a
     * call. Feeds the "calls with her usually run ~12 min" line on the cue,
     * which is there so the ask has a known size before anyone commits to it.
     */
    val callMinutes: Int?,

    /** How it left them, asked once afterwards. Null if they skipped it. */
    val feeling: Feeling?,

    /**
     * The flower this call grew. Derived from [feeling] at the time and kept,
     * rather than recomputed — the garden should not rearrange itself because
     * the mapping changed in a later release.
     */
    val flower: FlowerKind?,

    /** The shape the user gave the call before it started. */
    val topic: String?,

    /**
     * The words of a line the user left, when there were any.
     *
     * Harbor used to keep only that a line happened, so the history read as a
     * column of identical "You left a line." rows and told you nothing. It is
     * kept now, deliberately, and the screen says so rather than promising
     * otherwise.
     *
     * It syncs with the rest of the row. The study's export views are
     * aggregate rollups and do not select it, so the research extract stays
     * free of anyone's actual words -- keep it that way.
     */
    val note: String? = null,
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
