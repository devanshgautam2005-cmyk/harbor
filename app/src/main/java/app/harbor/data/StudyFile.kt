package app.harbor.data

import android.content.Context
import app.harbor.domain.StudyExport
import app.harbor.sensing.Sensing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * The week's export, written without anybody being asked to.
 *
 * Handing the week over used to be a card in Account: a participant had to
 * remember, at the end of the study, to go and find it and save a file. That
 * asks the person being studied to do the study's own admin, and a week of
 * somebody's data is lost every time one of them forgets.
 *
 * So the file writes itself. It is refreshed whenever Harbor goes to the back,
 * into the app's own external files directory, where it can be collected over
 * USB or from the phone's Files app without any permission and without the
 * participant doing a thing.
 *
 * ## This is not a network
 *
 * Nothing here sends anything. Harbor still holds no `INTERNET` permission
 * (ADR-004) and this adds none — it writes a file to the phone it is running
 * on, and getting that file off the phone is still a deliberate act by
 * somebody holding it. The contents are exactly what [StudyExport] allows:
 * shapes, counts and timestamps, with the redactions that file documents.
 */
object StudyFile {

    /** Where a collected file will be, on any phone running the study build. */
    const val FOLDER = "study"

    /**
     * Rewrite the export.
     *
     * Cheap enough to do on every trip to the background: a week of beats is a
     * few hundred rows, and the whole file is tens of kilobytes. Failure is
     * swallowed on purpose — a study file that cannot be written is not a
     * reason to crash somebody's phone, and the next pause will try again.
     */
    suspend fun refresh(context: Context, store: HarborRepository): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bundle = StudyExport.Bundle(
                    participant = store.participantId(),
                    exportedAt = Instant.now(),
                    appVersion = versionOf(context),
                    settings = store.settings.value,
                    contacts = store.contacts.value,
                    blocks = store.weekBlocks.value,
                    beats = store.beats(),
                    cues = store.allCues(),
                    entries = store.recentEntries(),
                    lastTransitionAt = Sensing.lastTransition(context),
                )
                val dir = File(context.getExternalFilesDir(null), FOLDER).apply { mkdirs() }
                // One file, overwritten, rather than one per pause. The
                // participant id is in the name so a folder of them collected
                // from several phones can still be told apart.
                val file = File(dir, StudyExport.filename(bundle))
                file.writeText(StudyExport.json(bundle))
                file
            }.getOrNull()
        }

    /** Which build produced a file, so an odd export can be traced to a version. */
    private fun versionOf(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        "${info.versionName} ($code)"
    }.getOrDefault("unknown")
}
