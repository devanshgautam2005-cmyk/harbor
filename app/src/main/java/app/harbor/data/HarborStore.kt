package app.harbor.data

import android.content.Context
import android.content.SharedPreferences
import app.harbor.domain.BusyWindow
import app.harbor.domain.Contact
import app.harbor.domain.Cue
import app.harbor.domain.CuePolicy
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.TriggerSource
import app.harbor.domain.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/**
 * SharedPreferences-backed [HarborRepository].
 *
 * Volumes here are tiny — a couple of cues a day, capped — so each collection
 * is held as one JSON array and rewritten on change. If that ever stops being
 * true, the fix is Room behind the same interface, not a cleverer version of
 * this.
 *
 * Writes are serialised through [writeLock] because the sensing service and
 * the UI can both reach this, and read-modify-write on a JSON blob is exactly
 * the shape that loses data under concurrency.
 */
class HarborStore(context: Context) : HarborRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val writeLock = Mutex()

    private val _settings = MutableStateFlow(readSettings())
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _contacts = MutableStateFlow(readContacts())
    override val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _busyWindows = MutableStateFlow(readBusyWindows())
    override val busyWindows: StateFlow<List<BusyWindow>> = _busyWindows.asStateFlow()

    private val _dailyAnswers = MutableStateFlow(readDailyAnswers())
    override val dailyAnswers: StateFlow<Map<LocalDate, String>> = _dailyAnswers.asStateFlow()

    // --- settings ---------------------------------------------------------

    private fun readSettings(): UserSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return UserSettings()
        // Corrupt, or written by an older shape. Defaults are a safe landing
        // spot: losing a calibration is bad, but crashing on boot is worse.
        // Note the default has cues OFF, so a failure here can never
        // over-notify someone.
        return runCatching { LedgerJson.settings(JSONObject(raw)) }
            .getOrDefault(UserSettings())
    }

    override suspend fun setSettings(settings: UserSettings) {
        write { putString(KEY_SETTINGS, LedgerJson.settings(settings).toString()) }
        _settings.value = settings
    }

    // --- contacts ---------------------------------------------------------

    private fun readContacts(): List<Contact> {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return runCatching { LedgerJson.contacts(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    override suspend fun upsertContact(contact: Contact) {
        val updated = (readContacts().filterNot { it.id == contact.id } + contact)
            .sortedBy { it.label }
        write { putString(KEY_CONTACTS, LedgerJson.contacts(updated).toString()) }
        _contacts.value = updated
    }

    override suspend fun deleteContact(id: UUID) {
        val updated = readContacts().filterNot { it.id == id }
        write { putString(KEY_CONTACTS, LedgerJson.contacts(updated).toString()) }
        _contacts.value = updated
    }

    // --- busy windows -----------------------------------------------------

    private fun readBusyWindows(): List<BusyWindow> {
        val raw = prefs.getString(KEY_BUSY, null) ?: return emptyList()
        return runCatching { LedgerJson.busyWindows(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    override suspend fun setBusyWindows(windows: List<BusyWindow>) {
        val sorted = windows.sortedWith(compareBy({ it.day }, { it.start }))
        write { putString(KEY_BUSY, LedgerJson.busyWindows(sorted).toString()) }
        _busyWindows.value = sorted
    }

    // --- the daily question -----------------------------------------------

    private fun readDailyAnswers(): Map<LocalDate, String> {
        val raw = prefs.getString(KEY_ANSWERS, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associate { LocalDate.parse(it) to o.getString(it) }
        }.getOrDefault(emptyMap())
    }

    override suspend fun setDailyAnswer(day: LocalDate, answer: String) {
        val updated = _dailyAnswers.value + (day to answer)
        write {
            putString(
                KEY_ANSWERS,
                JSONObject().apply {
                    updated.forEach { (d, text) -> put(d.toString(), text) }
                }.toString(),
            )
        }
        _dailyAnswers.value = updated
    }

    // --- reads ------------------------------------------------------------

    private fun readLedger(): List<LedgerEntry> {
        val raw = prefs.getString(KEY_LEDGER, null) ?: return emptyList()
        return runCatching { LedgerJson.entries(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private fun readCues(): List<Cue> {
        val raw = prefs.getString(KEY_CUES, null) ?: return emptyList()
        return runCatching { LedgerJson.cues(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    override suspend fun dayState(date: LocalDate): CuePolicy.DayState =
        withContext(Dispatchers.IO) {
            val ledger = readLedger()
            val cues = readCues()
            CuePolicy.DayState(
                entriesToday = ledger.filter { it.entryDate == date },
                // Manual cues are excluded on purpose. The daily cap limits
                // how often Harbor interrupts someone, and a cue they asked
                // for is not an interruption — it would be perverse for
                // trying the feature to use up the day's allowance.
                cuesToday = cues.count {
                    it.firedDate == date && it.triggerSource != TriggerSource.MANUAL
                },
                // Across every day, not just today: the cooldown has to
                // survive midnight.
                lastCueAt = cues.maxOfOrNull { it.firedAt },
                // Also across every day: a plan made on Tuesday for Friday is
                // still a plan.
                hasPendingReminder = ledger.any {
                    it.resolution == Resolution.PROPOSED_LATER && !it.reminderDone
                },
                busyNow = _busyWindows.value.any { it.covers(ZonedDateTime.now()) },
            )
        }

    override suspend fun recentEntries(): List<LedgerEntry> =
        withContext(Dispatchers.IO) { readLedger().sortedBy { it.occurredAt } }

    // --- writes -----------------------------------------------------------

    override suspend fun recordCue(cue: Cue) {
        writeList(KEY_CUES) {
            (readCues() + cue)
                .associateBy { it.id }
                .values
                .sortedBy { it.firedAt }
                .takeLast(RETAINED)
                .let(LedgerJson::cues)
        }
    }

    override suspend fun append(entry: LedgerEntry) {
        writeList(KEY_LEDGER) {
            // Idempotent: re-appending the same moment replaces it rather than
            // duplicating, matching the server's upsert key.
            (readLedger() + entry)
                .associateBy { it.id }
                .values
                .sortedBy { it.occurredAt }
                .takeLast(RETAINED)
                .let(LedgerJson::entries)
        }
    }

    override suspend fun markReminderDone(id: UUID) {
        writeList(KEY_LEDGER) {
            readLedger()
                .map { if (it.id == id) it.copy(reminderDone = true) else it }
                .let(LedgerJson::entries)
        }
        // The entry changed, so it has to go up to the server again.
        write {
            val synced = prefs.getStringSet(KEY_SYNCED_ENTRIES, emptySet()).orEmpty()
            putStringSet(KEY_SYNCED_ENTRIES, synced - id.toString())
        }
    }

    // --- sync bookkeeping -------------------------------------------------

    override suspend fun unsyncedCues(): List<Cue> = withContext(Dispatchers.IO) {
        val synced = prefs.getStringSet(KEY_SYNCED_CUES, emptySet()).orEmpty()
        readCues().filter { it.id.toString() !in synced }.sortedBy { it.firedAt }
    }

    override suspend fun unsyncedEntries(): List<LedgerEntry> = withContext(Dispatchers.IO) {
        val synced = prefs.getStringSet(KEY_SYNCED_ENTRIES, emptySet()).orEmpty()
        readLedger().filter { it.id.toString() !in synced }.sortedBy { it.occurredAt }
    }

    override suspend fun markSynced(cueIds: List<UUID>, entryIds: List<UUID>) {
        if (cueIds.isEmpty() && entryIds.isEmpty()) return
        write {
            // Only track ids we still hold, so these sets cannot grow forever
            // as old rows age out of the retained window.
            val heldCues = readCues().mapTo(mutableSetOf()) { it.id.toString() }
            val heldEntries = readLedger().mapTo(mutableSetOf()) { it.id.toString() }
            val cues = prefs.getStringSet(KEY_SYNCED_CUES, emptySet()).orEmpty()
            val entries = prefs.getStringSet(KEY_SYNCED_ENTRIES, emptySet()).orEmpty()

            putStringSet(
                KEY_SYNCED_CUES,
                (cues + cueIds.map(UUID::toString)) intersect heldCues,
            )
            putStringSet(
                KEY_SYNCED_ENTRIES,
                (entries + entryIds.map(UUID::toString)) intersect heldEntries,
            )
        }
    }

    // --- plumbing ---------------------------------------------------------

    private suspend fun write(block: SharedPreferences.Editor.() -> Unit) {
        withContext(Dispatchers.IO) {
            writeLock.withLock { prefs.edit().apply(block).commit() }
        }
    }

    private suspend fun writeList(key: String, build: () -> JSONArray) {
        withContext(Dispatchers.IO) {
            writeLock.withLock { prefs.edit().putString(key, build().toString()).commit() }
        }
    }

    private companion object {
        const val PREFS = "harbor"
        const val KEY_SETTINGS = "settings"
        const val KEY_CONTACTS = "contacts"
        const val KEY_BUSY = "busy_windows"
        const val KEY_ANSWERS = "daily_answers"
        const val KEY_LEDGER = "ledger"
        const val KEY_CUES = "cues"
        const val KEY_SYNCED_CUES = "synced_cue_ids"
        const val KEY_SYNCED_ENTRIES = "synced_entry_ids"

        /**
         * Roughly a month at the daily cap. Enough for the week-one study and
         * for any sync backlog worth retrying; older rows are the server's
         * problem, not the phone's.
         */
        const val RETAINED = 90
    }
}
