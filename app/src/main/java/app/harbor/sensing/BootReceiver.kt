package app.harbor.sensing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.harbor.data.HarborStore

/**
 * Re-registers transition updates after a reboot.
 *
 * Activity transition registrations do not survive a restart. Without this,
 * sensing would go quiet after every reboot and only come back the next time
 * the user happened to open Harbor — which, for an app whose entire premise
 * is that you forget to open it, means never.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext

        // Only if the user actually asked for cues. Re-registering for someone
        // who has them switched off would be sensing them without consent,
        // which is the one thing this app must never do.
        if (!HarborStore(app).settings.value.cuesEnabled) return

        val registered = ActivityTransitions.register(app)
        Log.i("HarborSensing", "boot re-register: $registered")
    }
}
