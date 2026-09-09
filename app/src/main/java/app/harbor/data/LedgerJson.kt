package app.harbor.data

import app.harbor.domain.Contact
import app.harbor.domain.FeedbackPulse
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.RewardShown
import app.harbor.domain.TriggerSource
import app.harbor.domain.UserThresholds
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
 * costs nothing. The model is six types; this is cheaper than the dependency.
 *
 * The wire names here match the Postgres column names in
 * `backend/supabase/migrations/0001_init.sql` so the sync layer does not need
 * a second mapping. Keep them in step.
 */
internal object LedgerJson {

    // --- thresholds -------------------------------------------------------

    fun thresholds(t: UserThresholds): JSONObject = JSONObject()
        .put("walking_minutes", t.walkingMinutes)
        .put("session_minutes", t.sessionMinutes)
        .put("daily_cap", t.dailyCap)
        .put("cooldown_minutes", t.cooldownMinutes)

    fun thresholds(o: JSONObject): UserThresholds = UserThresholds(
        walkingMinutes = o.getInt("walking_minutes"),
        sessionMinutes = o.getInt("session_minutes"),
        dailyCap = o.getInt("daily_cap"),
        cooldownMinutes = o.getInt("cooldown_minutes"),
    )

    // --- contact ----------------------------------------------------------

    fun contact(c: Contact): JSONObject = JSONObject()
        .put("label", c.label)
        .put("phone_e164", c.phoneE164)
        .put("cue_sound_ref", c.cueSoundRef)

    fun contact(o: JSONObject): Contact = Contact(
        label = o.getString("label"),
        phoneE164 = o.getString("phone_e164"),
        cueSoundRef = o.optStringOrNull("cue_sound_ref"),
    )

    // --- ledger -----------------------------------------------------------

    fun entry(e: LedgerEntry): JSONObject = JSONObject()
        .put("client_id", e.clientId.toString())
        .put("entry_date", e.entryDate.toString())
        .put("trigger_source", e.triggerSource.wire)
        .put("threshold_snapshot", thresholds(e.thresholdSnapshot))
        .put("resolution", e.resolution.wire)
        .put("proposed_time", e.proposedTime?.toString())
        .put("feedback_pulse", e.feedbackPulse?.wire)
        .put("reward_shown", e.rewardShown?.wire)
        .put("occurred_at", e.occurredAt.toString())

    fun entry(o: JSONObject): LedgerEntry = LedgerEntry(
        clientId = UUID.fromString(o.getString("client_id")),
        entryDate = LocalDate.parse(o.getString("entry_date")),
        triggerSource = TriggerSource.entries.fromWire(o.getString("trigger_source")),
        thresholdSnapshot = thresholds(o.getJSONObject("threshold_snapshot")),
        resolution = Resolution.entries.fromWire(o.getString("resolution")),
        proposedTime = o.optStringOrNull("proposed_time")?.let(Instant::parse),
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
