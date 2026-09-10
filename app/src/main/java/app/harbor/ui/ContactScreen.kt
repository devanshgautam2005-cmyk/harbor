package app.harbor.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.harbor.cue.Ringer
import app.harbor.data.HarborRepository
import app.harbor.domain.Contact
import app.harbor.domain.ContactKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Choosing who the cue is about — their name, number, ringtone and photo.
 *
 * The sound and the photo are not decoration. The cue borrows a conditioned
 * response that already exists: her ringtone means her, and has for years.
 * That is the mechanism (ADR-009), which makes this screen a functional part
 * of the trigger rather than a settings page.
 *
 * Nothing here costs a permission. The ringtone comes from the system picker,
 * the photo through `ACTION_GET_CONTENT` and then a copy into Harbor's own
 * storage. Reading the contact's entry from the address book would need
 * `READ_CONTACTS`, and a contacts prompt beside the activity one would cost
 * far more trust than it buys.
 */
@Composable
fun ContactScreen(
    store: HarborRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    val existing = contacts.firstOrNull()

    // The id is fixed for the life of this screen so the photo copy lands
    // under the same name the saved contact will carry.
    val id = remember(existing?.id) { existing?.id ?: UUID.randomUUID() }

    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var phone by remember(existing) { mutableStateOf(existing?.phoneE164 ?: "") }
    var soundRef by remember(existing) { mutableStateOf(existing?.cueSoundRef) }
    var photoRef by remember(existing) { mutableStateOf(existing?.photoRef) }
    var error by remember { mutableStateOf<String?>(null) }

    val previewRinger = remember { Ringer(context) }

    // Leaving a ringtone looping after the user navigates away would be a
    // small horror. Belt and braces alongside the explicit Stop button.
    DisposableEffect(Unit) { onDispose { previewRinger.stop() } }

    val pickSound = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = result.data
                ?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            soundRef = picked?.toString()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { picked ->
        if (picked != null) {
            scope.launch {
                photoRef = withContext(Dispatchers.IO) {
                    ContactPhotos.store(context, id, picked)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Who would you call?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Their ringtone and their face are what make a cue feel like them " +
                "rather than like an app.",
            style = MaterialTheme.typography.bodyMedium,
        )

        val preview by produceState<ImageBitmap?>(null, photoRef) {
            val ref = photoRef
            value = if (ref == null) null else withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(ref)).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            preview?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.size(16.dp))
            }
            OutlinedButton(onClick = { pickPhoto.launch("image/*") }) {
                Text(if (photoRef == null) "Choose a photo" else "Change photo")
            }
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it; error = null },
            label = { Text("What do you call them?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }; error = null },
            label = { Text("Their number") },
            supportingText = { Text("With the country code, like +919876543210") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    pickSound.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Their sound")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                soundRef?.let(Uri::parse),
                            )
                        },
                    )
                },
            ) {
                Text(if (soundRef == null) "Choose their sound" else "Change sound")
            }

            if (soundRef != null) {
                TextButton(onClick = { previewRinger.start(soundRef) }) { Text("Play") }
                TextButton(onClick = { previewRinger.stop() }) { Text("Stop") }
            }
        }

        Text(
            "Their real ringtone works best — it is the sound you already " +
                "associate with them. A song that reminds you of them works too.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )

        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                previewRinger.stop()
                val trimmedLabel = label.trim()
                val trimmedPhone = phone.trim()

                // The same shape the schema enforces, checked here so the user
                // finds out now rather than when a sync silently rejects them.
                when {
                    trimmedLabel.isEmpty() ->
                        error = "A name helps — it is what the cue will say."
                    !PHONE.matches(trimmedPhone) ->
                        error = "That does not look like a full number. Include " +
                            "the country code, like +919876543210."
                    else -> {
                        scope.launch {
                            store.upsertContact(
                                Contact(
                                    id = id,
                                    label = trimmedLabel,
                                    phoneE164 = trimmedPhone,
                                    kind = ContactKind.PERSON,
                                    cueSoundRef = soundRef,
                                    photoRef = photoRef,
                                ),
                            )
                            onDone()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        TextButton(onClick = { previewRinger.stop(); onDone() }) { Text("Back") }
    }
}

/** Matches the `phone_e164` CHECK constraint in 0001_init.sql. */
private val PHONE = Regex("""^\+[1-9]\d{7,14}$""")
