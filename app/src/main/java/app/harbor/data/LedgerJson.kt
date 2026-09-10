package app.harbor.data

import app.harbor.domain.Contact
import app.harbor.domain.ContactKind
import app.harbor.domain.Cue
import app.harbor.domain.CueSound
import app.harbor.domain.FeedbackPulse
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Minimum
import app.harbor.domain.Resolution
import app.harbor.domain.RewardShown
import app.harbor.domain.Thresholds
import app.harbor.domain.TriggerSource
import app.harbor.domain.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Hand-rolled JSON mapping for the persisted types.
 *
 * Deliberately not kotlinx-serialization: that needs a compiler plugin whose
 * version has to track Kotlin's exactly, and the same constraint has bitten
 * this team before on the sibling project. `org.json` ships with Android and
 * costs nothing.
 *
 * The wire names match the Postgres column names in
 * `backend/supabase/migrations/` so the sync layer does not need a second
 * mapping. Keep them in step.
 */
internal object LedgerJson {

    // --- settings ---------------------------------------------------------

    fun thresholds(t: Thresholds): JSONObject = JSONObject()
        .put("walking_minutes", t.walkingMinutes)
        .put("session_minutes", t.sessionMinutes)
        .put("daily_cap", t.dailyCap)
        .put("cooldown_minutes", t.cooldownMinutes)

    fun thresholds(o: JSONObject): Thresholds = Thresholds(
        walkingMinutes = o.getInt("walking_minutes"),
        sessionMinutes = o.getInt("session_minutes"),
        dailyCap = o.getInt("daily_cap"),
        cooldownMinutes = o.getInt("cooldown_minutes"),
    )

    fun settings(s: UserSettings): JSONObject = thresholds(s.thresholds)
        .put("cues_enabled", s.cuesEnabled)
        .put("minimum", s.minimum?.wire)
        .put("sound", s.sound.wire)
        .put("reduced_motion", s.reducedMotion)

    fun settings(o: JSONObject): UserSettings = UserSettings(
        thresholds = thresholds(o),
        cuesEnabled = o.optBoolean("cues_enabled", false),
        minimum = o.optStringOrNull("minimum")?.let { Minimum.entries.fromWire(it) },
        sound = o.optStringOrNull("sound")
            ?.let { CueSound.entries.fromWire(it) } ?: CueSound.CHIME,
        reducedMotion = o.optBoolean("reduced_motion", false),
    )

    // --- contact ----------------------------------------------------------

    fun contact(c: Contact): JSONObject = JSONObject()
        .put("id", c.id.toString())
        .put("label", c.label)
        .put("phone_e164", c.phoneE164)
        .put("kind", c.kind.wire)
        .put("cue_sound_ref", c.cueSoundRef)
        .put("photo_ref", c.photoRef)

    fun contact(o: JSONObject): Contact = Contact(
        id = UUID.fromString(o.getString("id")),
        label = o.getString("label"),
        phoneE164 = o.optStringOrNull("phone_e164"),
        kind = o.optStringOrNull("kind")
            ?.let { ContactKind.entries.fromWire(it) } ?: ContactKind.PERSON,
        cueSoundRef = o.optStringOrNull("cue_sound_ref"),
        photoRef = o.optStringOrNull("photo_ref"),
    )

    fun contacts(array: JSONArray): List<Contact> =
        (0 until array.length()).map { contact(array.getJSONObject(it)) }

    fun contacts(list: List<Contact>): JSONArray =
        JSONArray().apply { list.forEach { put(contact(it)) } }

    // --- cue --------------------------------------------------------------

    fun cue(c: Cue): JSONObject = JSONObject()
        .put("id", c.id.toString())
        .put("fired_date", c.firedDate.toString())
        .put("trigger_source", c.triggerSource.wire)
        .put("fired_at", c.firedAt.toString())

    fun cue(o: JSONObject): Cue = Cue(
        id = UUID.fromString(o.getString("id")),
        firedDate = LocalDate.parse(o.getString("fired_date")),
        triggerSource = TriggerSource.entries.fromWire(o.getString("trigger_source")),
        firedAt = Instant.parse(o.getString("fired_at")),
    )

    fun cues(array: JSONArray): List<Cue> =
        (0 until array.length()).map { cue(array.getJSONObject(it)) }

    fun cues(list: List<Cue>): JSONArray =
        JSONArray().apply { list.forEach { put(cue(it)) } }

    // --- ledger -----------------------------------------------------------

    fun entry(e: LedgerEntry): JSONObject = JSONObject()
        .put("id", e.id.toString())
        .put("entry_date", e.entryDate.toString())
        .put("cue_id", e.cueId?.toString())
        .put("contact_id", e.contactId?.toString())
        .put("trigger_source", e.triggerSource.wire)
        .put("threshold_snapshot", thresholds(e.thresholdSnapshot))
        .put("resolution", e.resolution.wire)
        .put("proposed_time", e.proposedTime?.toString())
        .put("reminder_done", e.reminderDone)
        .put("feedback_pulse", e.feedbackPulse?.wire)
        .put("reward_shown", e.rewardShown?.wire)
        .put("occurred_at", e.occurredAt.toString())

    fun entry(o: JSONObject): LedgerEntry = LedgerEntry(
        id = UUID.fromString(o.getString("id")),
        entryDate = LocalDate.parse(o.getString("entry_date")),
        cueId = o.optStringOrNull("cue_id")?.let(UUID::fromString),
        contactId = o.optStringOrNull("contact_id")?.let(UUID::fromString),
        triggerSource = TriggerSource.entries.fromWire(o.getString("trigger_source")),
        thresholdSnapshot = thresholds(o.getJSONObject("threshold_snapshot")),
        resolution = Resolution.entries.fromWire(o.getString("resolution")),
        proposedTime = o.optStringOrNull("proposed_time")?.let(Instant::parse),
        reminderDone = o.optBoolean("reminder_done", false),
        feedbackPulse = o.optStringOrNull("feedback_pulse")
            ?.let { FeedbackPulse.entries.fromWire(it) },
        rewardShown = o.optStringOrNull("reward_shown")
            ?.let { RewardShown.entries.fromWire(it) },
        occurredAt = Instant.parse(o.getString("occurred_at")),
    )

    fun entries(array: JSONArray): List<LedgerEntry> =
        (0 until array.length()).map { entry(array.getJSONObject(it)) }

    fun entries(list: List<LedgerEntry>): JSONArray =
        JSONArray().apply { list.forEach { put(entry(it)) } }
}

/**
 * Postgres enum labels are lower_snake_case; Kotlin's are UPPER_SNAKE. One
 * lowercase() keeps the two in step without a hand-written table to drift.
 */
private val Enum<*>.wire: String get() = name.lowercase()

private fun <E : Enum<E>> List<E>.fromWire(value: String): E =
    firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: error("unknown ${first()::class.simpleName} '$value' in stored data")

/**
 * `JSONObject.optString` returns the string "null" for a JSON null, which has
 * caused more bugs than it has ever prevented.
 */
private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
