package app.harbor.data

import app.harbor.domain.Contact
import app.harbor.domain.Cue
import app.harbor.domain.CuePolicy
import app.harbor.domain.LedgerEntry
import app.harbor.domain.UserSettings
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.util.UUID

/**
 * Everything Harbor persists, behind one interface.
 *
 * The interface exists so the storage engine can be swapped without the
 * pipeline noticing. [HarborStore] is a SharedPreferences + JSON
 * implementation, which is more than enough for the volumes here. Room is the
 * obvious upgrade if querying ever gets interesting; write it against this
 * interface when that day comes.
 *
 * This is local storage only. Sync to Supabase reads through here and pushes
 * outward — never the reverse, and never on the path to showing a cue.
 * See ADR-003.
 */
interface HarborRepository {

    /** The user's preferences and calibration. Never null. */
    val settings: StateFlow<UserSettings>

    /** Everyone the user has added. Empty until onboarding picks someone. */
    val contacts: StateFlow<List<Contact>>

    suspend fun setSettings(settings: UserSettings)

    suspend fun upsertContact(contact: Contact)

    suspend fun deleteContact(id: UUID)

    /**
     * Assembles everything [CuePolicy.decide] needs to know about the user's
     * recent history.
     *
     * This lives here rather than in the policy because gathering it is IO,
     * and the policy has to stay pure. It is also the only place that knows
     * the three counters come from two different stores — cues for the cap and
     * the cooldown, entries for whether the user already connected.
     */
    suspend fun dayState(date: LocalDate): CuePolicy.DayState

    /** Records that a cue fired, before the user has answered it. */
    suspend fun recordCue(cue: Cue)

    /** Stage 9. */
    suspend fun append(entry: LedgerEntry)

    /** Marks a proposed-later plan as dealt with, so it stops suppressing. */
    suspend fun markReminderDone(clientId: UUID)

    /** Cues and entries not yet accepted by the server, oldest first. */
    suspend fun unsyncedCues(): List<Cue>

    suspend fun unsyncedEntries(): List<LedgerEntry>

    /** Called after the server has acknowledged an upsert. */
    suspend fun markSynced(cueIds: List<UUID>, entryIds: List<UUID>)
}
