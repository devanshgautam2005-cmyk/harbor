package app.harbour.data

import app.harbour.domain.Contact
import app.harbour.domain.LedgerEntry
import app.harbour.domain.UserThresholds
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Everything Harbour persists, behind one interface.
 *
 * The interface exists so the storage engine can be swapped without the
 * pipeline noticing. [HarbourStore] is a SharedPreferences + JSON
 * implementation, which is more than enough for the volumes here (a couple of
 * ledger entries a day). Room is the obvious upgrade if querying ever gets
 * interesting; write it against this interface when that day comes.
 *
 * This is local storage only. Sync to Supabase reads through here and pushes
 * outward — never the reverse, and never on the path to showing a cue.
 * See ADR-003.
 */
interface HarbourRepository {

    /** The user's current calibration. Never null — falls back to SUGGESTED. */
    val thresholds: StateFlow<UserThresholds>

    /** Null until the user has picked someone in onboarding. */
    val contact: StateFlow<Contact?>

    suspend fun setThresholds(thresholds: UserThresholds)

    suspend fun setContact(contact: Contact?)

    /**
     * Every entry written for [date], local time. This is what stage 3 reads
     * to decide whether to suppress.
     */
    suspend fun entriesOn(date: LocalDate): List<LedgerEntry>

    /**
     * When the last cue fired, on any day, or null if none ever has.
     *
     * Separate from [entriesOn] because the cooldown has to survive midnight:
     * a cue at 23:55 must still suppress one at 00:05, by which point "today"
     * holds nothing.
     */
    suspend fun lastCueAt(): Instant?

    /** Stage 9. */
    suspend fun append(entry: LedgerEntry)

    /** Entries not yet accepted by the server, oldest first. */
    suspend fun unsynced(): List<LedgerEntry>

    /** Called after the server has acknowledged an upsert. */
    suspend fun markSynced(clientIds: List<UUID>)
}
