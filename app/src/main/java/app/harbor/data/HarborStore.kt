package app.harbor.data

import android.content.Context
import android.content.SharedPreferences
import app.harbor.domain.Contact
import app.harbor.domain.LedgerEntry
import app.harbor.domain.UserThresholds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * SharedPreferences-backed [HarborRepository].
 *
 * Volumes here are tiny — a couple of ledger entries a day, capped — so the
 * whole ledger is held as one JSON array and rewritten on append. If that ever
 * stops being true, the fix is Room behind the same interface, not a cleverer
 * version of this.
 *
 * Writes are serialised through [writeLock] because the sensing service and
 * the UI can both reach this, and read-modify-write on a JSON blob is exactly
 * the shape that loses data under concurrency.
 */
class HarborStore(context: Context) : HarborRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val writeLock = Mutex()

    private val _thresholds = MutableStateFlow(readThresholds())
    override val thresholds: StateFlow<UserThresholds> = _thresholds.asStateFlow()

    private val _contact = MutableStateFlow(readContact())
    override val contact: StateFlow<Contact?> = _contact.asStateFlow()

    // --- settings ---------------------------------------------------------

    private fun readThresholds(): UserThresholds {
        val raw = prefs.getString(KEY_THRESHOLDS, null) ?: return UserThresholds.SUGGESTED
        return runCatching { LedgerJson.thresholds(JSONObject(raw)) }
            // Corrupt or from an older shape. The suggestion is a safe landing
            // spot; losing a calibration is bad but crashing on boot is worse.
            .getOrDefault(UserThresholds.SUGGESTED)
    }

    private fun readContact(): Contact? {
        val raw = prefs.getString(KEY_CONTACT, null) ?: return null
        return runCatching { LedgerJson.contact(JSONObject(raw)) }.getOrNull()
    }

    override suspend fun setThresholds(thresholds: UserThresholds) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                prefs.edit()
                    .putString(KEY_THRESHOLDS, LedgerJson.thresholds(thresholds).toString())
                    .commit()
            }
        }
        _thresholds.value = thresholds
    }

    override suspend fun setContact(contact: Contact?) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                prefs.edit().apply {
                    if (contact == null) remove(KEY_CONTACT)
                    else putString(KEY_CONTACT, LedgerJson.contact(contact).toString())
                }.commit()
            }
        }
        _contact.value = contact
    }

    // --- ledger -----------------------------------------------------------

    private fun readLedger(): List<LedgerEntry> {
        val raw = prefs.getString(KEY_LEDGER, null) ?: return emptyList()
        return runCatching { LedgerJson.entries(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    override suspend fun entriesOn(date: LocalDate): List<LedgerEntry> =
        withContext(Dispatchers.IO) { readLedger().filter { it.entryDate == date } }

    override suspend fun lastCueAt(): Instant? =
        withContext(Dispatchers.IO) { readLedger().maxOfOrNull { it.occurredAt } }

    override suspend fun append(entry: LedgerEntry) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val kept = (readLedger() + entry)
                    // Idempotent: re-appending the same cue replaces it rather
                    // than duplicating, matching the server's upsert key.
                    .associateBy { it.clientId }
                    .values
                    .sortedBy { it.occurredAt }
                    .takeLast(RETAINED_ENTRIES)

                prefs.edit()
                    .putString(KEY_LEDGER, LedgerJson.entries(kept).toString())
                    .commit()
            }
        }
    }

    override suspend fun unsynced(): List<LedgerEntry> = withContext(Dispatchers.IO) {
        val synced = prefs.getStringSet(KEY_SYNCED, emptySet()).orEmpty()
        readLedger()
            .filter { it.clientId.toString() !in synced }
            .sortedBy { it.occurredAt }
    }

    override suspend fun markSynced(clientIds: List<UUID>) {
        if (clientIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val existing = prefs.getStringSet(KEY_SYNCED, emptySet()).orEmpty()
                val retained = readLedger().mapTo(mutableSetOf()) { it.clientId.toString() }
                // Only track ids we still hold, so this set cannot grow forever
                // as old entries age out of the ledger.
                val updated = (existing + clientIds.map(UUID::toString)) intersect retained

                prefs.edit().putStringSet(KEY_SYNCED, updated).commit()
            }
        }
    }

    private companion object {
        const val PREFS = "harbor"
        const val KEY_THRESHOLDS = "thresholds"
        const val KEY_CONTACT = "contact"
        const val KEY_LEDGER = "ledger"
        const val KEY_SYNCED = "synced_client_ids"

        /**
         * Roughly a month at the daily cap. Enough for the week-one study and
         * for any sync backlog worth retrying; old entries are the server's
         * problem, not the phone's.
         */
        const val RETAINED_ENTRIES = 90
    }
}
