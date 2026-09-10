package app.harbor.sensing

import android.content.Context
import java.time.Instant

/**
 * The only thing [BoutTracker] needs to remember between events.
 *
 * Deliberately its own tiny prefs file rather than part of `HarborStore`. The
 * transition receiver is woken by the system, runs for a few milliseconds and
 * dies, dozens of times a day; it should not be loading and reparsing the
 * whole ledger just to note that a walk began.
 */
internal class SensingStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("harbor_sensing", Context.MODE_PRIVATE)

    var state: BoutTracker.State
        get() {
            val millis = prefs.getLong(KEY_WALKING_SINCE, ABSENT)
            return BoutTracker.State(
                walkingSince = if (millis == ABSENT) null else Instant.ofEpochMilli(millis),
            )
        }
        set(value) {
            // commit, not apply: the process is likely to be killed the moment
            // this receiver returns, and a lost write means a lost walk.
            prefs.edit().apply {
                val since = value.walkingSince
                if (since == null) remove(KEY_WALKING_SINCE)
                else putLong(KEY_WALKING_SINCE, since.toEpochMilli())
            }.commit()
        }

    private companion object {
        const val KEY_WALKING_SINCE = "walking_since"
        const val ABSENT = -1L
    }
}
