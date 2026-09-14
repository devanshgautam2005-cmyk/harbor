package app.harbor.cue

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import app.harbor.R
import app.harbor.domain.Contact
import app.harbor.domain.Cue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Posts the cue, and gets the full-screen surface in front of the user.
 *
 * Two paths, and both have to work:
 *
 *  - With `USE_FULL_SCREEN_INTENT`, [CueActivity] opens directly, the way an
 *    incoming call would.
 *  - Without it — Android 14 restricts that permission to calling and alarm
 *    apps, and Harbor hands off to the dialer rather than placing calls — the
 *    system degrades this to a heads-up notification. That is the designed
 *    fallback, not an error. The cue still rings with the contact's sound,
 *    still shows their name, and still opens the full surface when tapped.
 *
 * A missing full-screen permission is never a reason to suppress a cue.
 * See ADR-009.
 */
object CueNotifier {

    const val EXTRA_CUE_ID = "cue_id"
    const val EXTRA_CONTACT_ID = "contact_id"
    const val EXTRA_SOURCE = "trigger_source"

    /** One id, so a second cue replaces rather than stacks. */
    private const val NOTIFICATION_ID = 1

    fun post(context: Context, cue: Cue, contact: Contact?) {
        val sound = contact?.cueSoundRef?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val channelId = ensureChannel(context, sound)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, CueActivity::class.java).apply {
                putExtra(EXTRA_CUE_ID, cue.id.toString())
                putExtra(EXTRA_CONTACT_ID, contact?.id?.toString())
                putExtra(EXTRA_SOURCE, cue.triggerSource.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val who = contact?.label ?: "someone at home"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            // Deliberately Harbor's voice, not an impersonation of an incoming
            // call. "Mom is calling" would be a lie, and a frightening one for
            // a student far from home. ADR-009.
            .setContentTitle("A quiet moment")
            .setContentText("Call $who?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Shown on the lock screen rather than hidden behind "Harbor has a
            // notification". The channel's own lockscreenVisibility is PRIVATE,
            // which is the right default for a channel but wrong for this one
            // message: a cue somebody cannot read from the lock screen is a cue
            // they have to unlock the phone to understand, which is most of the
            // friction the cue exists to remove. Nothing in it is private -- a
            // first name and an offer.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .setAutoCancel(true)
            // Dismissible in one gesture, at no cost. Handoff, section 7.
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /**
     * Whether the cue can actually take the screen, or will only ever be a
     * notification.
     *
     * This is the difference between the cue working and the cue not working,
     * and it is invisible: from Android 14 the system grants
     * `USE_FULL_SCREEN_INTENT` only to apps it considers calling or alarm
     * apps, and silently downgrades everyone else's full-screen intent to a
     * heads-up notification. Declaring the permission is not enough and there
     * is no error -- [post] succeeds, the notification appears, the surface
     * never opens, and a participant who was not looking at their phone at
     * that second simply never sees the cue.
     *
     * That is the whole study's measurement, so the app has to be able to
     * report it. See [fullScreenSettings] for the way to fix it.
     */
    fun canTakeTheScreen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return context.getSystemService(NotificationManager::class.java)
            .canUseFullScreenIntent()
    }

    /**
     * The one screen where a person can grant it.
     *
     * There is no runtime prompt for this permission -- it can only be turned
     * on by hand in settings, which is why the app has to take somebody there
     * rather than asking.
     */
    fun fullScreenSettings(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.fromParts("package", context.packageName, null),
        )
    }

    /**
     * A channel per sound.
     *
     * A notification channel's sound is immutable once created, and the whole
     * point here is that the sound is the user's choice and can change. So the
     * channel id is derived from the sound itself: choosing a new one creates
     * a new channel and retires the old.
     *
     * The alternative — one channel, sound played only by the activity — would
     * leave the heads-up fallback silent, which loses exactly the association
     * the cue depends on.
     */
    private fun ensureChannel(context: Context, sound: Uri?): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return LEGACY_CHANNEL

        val id = "cue_${sound?.toString()?.hashCode() ?: 0}"
        val manager = context.getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(
                id,
                "Gentle cues",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "The moment after a walk, when calling home is easy."
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)

            // Retire channels for sounds no longer chosen, so the app's
            // notification settings do not accumulate one row per song the
            // user ever tried.
            manager.notificationChannels
                .filter { it.id.startsWith("cue_") && it.id != id }
                .forEach { manager.deleteNotificationChannel(it.id) }
        }
        return id
    }

    private const val LEGACY_CHANNEL = "cue"
}
