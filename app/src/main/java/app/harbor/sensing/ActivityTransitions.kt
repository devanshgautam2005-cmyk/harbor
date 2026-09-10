package app.harbor.sensing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.time.Instant

/**
 * Registration for Google Play services' Activity Recognition Transition API.
 *
 * This is the API the handoff settled on, and it is **not** Google Fit —
 * Fit is being retired, this is independent of it, and nothing here should
 * ever be migrated onto Health Connect unless step *history* is wanted
 * somewhere else in Harbor. See ADR-005.
 *
 * There is no foreground service. Play services delivers transitions to a
 * PendingIntent whether or not Harbor is running, so a service of our own
 * would add a permanent notification, a battery footprint and an extra
 * permission in exchange for nothing. See ADR-008.
 */
object ActivityTransitions {

    /**
     * The transitions we ask to hear about.
     *
     * WALKING and STILL are what [BoutTracker] runs on. The other three
     * ENTERs exist only so a bout can be discarded when a walk turns into a
     * commute rather than into stillness — without them, sitting still in a
     * car half an hour later would close a bout that had nothing to do with
     * it.
     */
    private val TRANSITIONS: List<ActivityTransition> = buildList {
        add(transition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER))
        add(transition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT))
        add(transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER))
        add(transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER))
        add(transition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER))
        add(transition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER))
    }

    private fun transition(activity: Int, type: Int) = ActivityTransition.Builder()
        .setActivityType(activity)
        .setActivityTransition(type)
        .build()

    /**
     * Whether the user has granted activity recognition.
     *
     * The runtime permission only exists from API 29. Below that the Play
     * services permission applies and is granted at install, so there is
     * nothing to ask for and nothing to check.
     */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Start listening. Safe to call repeatedly — Play services replaces the
     * existing registration rather than stacking another.
     *
     * @return false if the permission is not granted, in which case nothing
     *   was registered and the caller should not pretend sensing is running.
     */
    fun register(context: Context): Boolean {
        if (!hasPermission(context)) return false

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(
                ActivityTransitionRequest(TRANSITIONS),
                pendingIntent(context),
            )
        return true
    }

    /** Stop listening. Called when the user turns cues off. */
    fun unregister(context: Context) {
        if (!hasPermission(context)) return
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, TransitionReceiver::class.java)

        // MUTABLE is required, not a choice: Play services fills the result
        // into this intent before delivering it. An immutable one is accepted
        // at registration and then silently delivers nothing.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        return PendingIntent.getBroadcast(context.applicationContext, REQUEST_CODE, intent, flags)
    }

    /**
     * Convert a transition's timestamp to wall-clock time.
     *
     * Play services reports elapsed realtime since boot, which is monotonic
     * and says nothing about the date. The ledger and the suppression window
     * both work in wall-clock terms, so the conversion has to happen — and it
     * has to happen using the boot instant, not `now`, or a transition that
     * was queued while the device was dozing arrives stamped with the moment
     * we noticed it rather than the moment it happened.
     */
    fun toInstant(elapsedRealtimeNanos: Long): Instant {
        val bootMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        return Instant.ofEpochMilli(bootMillis + elapsedRealtimeNanos / 1_000_000)
    }

    private const val REQUEST_CODE = 1
}
