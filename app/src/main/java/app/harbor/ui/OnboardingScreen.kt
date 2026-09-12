package app.harbor.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.Thresholds
import app.harbor.sensing.ActivityTransitions
import app.harbor.sensing.Sensing
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.Notice
import app.harbor.ui.theme.PageIntro
import app.harbor.ui.theme.PrimaryAction
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import kotlinx.coroutines.launch

/**
 * The first run.
 *
 * Three things have to be true before Harbor can do the one thing it is for:
 * somebody to call, permission to notice you have stopped walking, and
 * permission to show you anything. All three are set up here and nowhere
 * else, so a person who reaches home without them has an app that opens,
 * looks finished, and never fires. See `docs/06-onboarding.md`.
 *
 * ## Why the permission ask is fifth and not first
 *
 * Nobody grants a movement permission to an app they have not understood yet.
 * By the time this asks, the person has named someone, chosen their colour and
 * heard their ringtone — so the ask reads as *so I can catch a good moment to
 * call her* rather than *so I can watch you walk*. That ordering is the single
 * most consequential decision in this file.
 *
 * Declining is a real answer and is respected. The app works without cues;
 * nothing nags, and nothing pretends to be on when it is not.
 */
@Composable
fun OnboardingScreen(
    store: HarborRepository,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun next() { step++ }
    fun finish() {
        scope.launch {
            store.setOnboarded()
            onFinished()
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (step) {
            0 -> Welcome(::next)
            1 -> YourName(store, ::next)
            // Reads the live value, not the collected copy: upsertContact
            // updates the flow before it returns, but collectAsState only
            // catches up on the next recomposition — so gating on the copy
            // meant saving a contact never advanced the step. The gate itself
            // has to stay, because this screen's Back also calls onDone and a
            // person must not reach home without somebody to call.
            2 -> ContactScreen(
                store,
                onDone = { if (store.contacts.value.isNotEmpty()) next() },
            )
            3 -> HearACue(store, ::next)
            4 -> AskPermission(store, ::next)
            5 -> YourPace(store, ::next)
            6 -> ScheduleScreen(store, onDone = ::finish)
            else -> finish()
        }

        // Seven steps is few enough to show honestly. A bar that only ever
        // creeps forward would be less useful than knowing there are two left.
        if (step in 1..5) {
            Progress(step, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
        }
    }
}

@Composable
private fun Progress(step: Int, modifier: Modifier = Modifier) {
    Row(modifier) {
        repeat(7) { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (i == step) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (i <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(28.dp))
        Flow(Modifier.pageContent()) { content() }
    }
}

/** What this is, and — just as important — what it will never do. */
@Composable
private fun Welcome(onNext: () -> Unit) = Page {
    PageIntro(
        eyebrow = "Harbor",
        title = "A little closer, every day.",
        subtitle = "Harbor notices the quiet moment just after a walk ends, and " +
            "offers you the chance to call home. That is the whole of it.",
    )
    Surface {
        SectionHeading("What it will not do")
        // Said now, before any permission is mentioned, because this is the
        // worry the permission dialog will otherwise raise on its own.
        SmallCopy(
            "Your family are not part of this. They install nothing, they are " +
                "never told anything, and they never see a thing you do here — " +
                "not your walking, not whether you answered, not even a summary.",
        )
        SmallCopy("There is no streak. Ignoring a cue costs you nothing.")
    }
    PrimaryAction("Start", onClick = onNext)
}

@Composable
private fun YourName(store: HarborRepository, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()
    var draft by remember { mutableStateOf(settings.name) }

    Page {
        PageIntro(
            eyebrow = "Step one",
            title = "What should we call you?",
            subtitle = "Only used to say hello. It never leaves this phone.",
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(40) },
            placeholder = { Text("your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryAction("Continue") {
            scope.launch { store.setSettings(settings.copy(name = draft.trim())) }
            onNext()
        }
        TextLink("Skip", onNext)
    }
}

/**
 * Hearing it once, before being asked for anything.
 *
 * A real cue with her photo and her ringtone explains the product better than
 * any screen about it, and it costs nothing to show — a manual cue does not
 * touch the daily allowance.
 */
@Composable
private fun HearACue(store: HarborRepository, onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    val who = contacts.firstOrNull()

    Page {
        PageIntro(
            eyebrow = "Step three",
            title = "This is what a cue looks like.",
            subtitle = "Full screen, their face, and the sound you chose. It is " +
                "shaped like a call because that is what makes it land as them.",
        )
        Surface {
            SmallCopy(
                "It never says anyone is calling you, because nobody is. It is " +
                    "an offer, and \"not now\" is always there.",
            )
            if (who != null) {
                PrimaryAction("Show me one") {
                    scope.launch { showManualCue(context, store, who) }
                }
            }
        }
        TextLink("Next", onNext)
    }
}

/**
 * The screen the study lives or dies on.
 *
 * The system dialog only ever appears after a deliberate tap, and a refusal is
 * accepted rather than argued with. Android stops asking after two refusals,
 * so the only route left is system settings — and the app has to notice the
 * grant when the person comes back, which is what the recheck is for.
 */
@Composable
private fun AskPermission(store: HarborRepository, onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()

    var granted by remember { mutableStateOf(ActivityTransitions.hasPermission(context)) }
    var refused by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val request = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val ok = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: granted
        granted = ok
        refused = !ok
        if (ok) scope.launch { failed = !Sensing.enable(context, store) }
    }

    fun ask() {
        refused = false
        failed = false
        val wanted = buildList {
            if (!granted) add(Manifest.permission.ACTIVITY_RECOGNITION)
            // Without this the cue is posted and silently dropped, which looks
            // exactly like a trigger that never fired.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (wanted.isEmpty()) scope.launch { failed = !Sensing.enable(context, store) }
        else request.launch(wanted.toTypedArray())
    }

    Page {
        PageIntro(
            eyebrow = "Step four",
            title = "May Harbor notice when you stop walking?",
            subtitle = "This is the part that makes a cue arrive on its own.",
        )
        Surface {
            SectionHeading("What Harbor reads")
            SmallCopy(
                "Whether your phone thinks you are walking or still. Not where " +
                    "you are, not what you are doing, not which apps you use.",
            )
            SectionHeading("Where it stays")
            SmallCopy(
                "On this phone. Your movement is never sent to us and never " +
                    "shared with your family — not as a summary, not ever.",
            )
            SectionHeading("What you keep control of")
            SmallCopy(
                "At most ${settings.thresholds.dailyCap} a day, with at least " +
                    "${settings.thresholds.cooldownMinutes} minutes between them. " +
                    "Every one can be dismissed, and dismissing costs nothing. " +
                    "You can turn it off whenever you like.",
            )
        }

        when {
            Sensing.isActive(context, store) -> {
                Notice("Cues are on. Harbor will wait for a real walk.")
                PrimaryAction("Continue", onClick = onNext)
            }

            refused -> {
                SmallCopy(
                    "That is completely fine. Cues stay off and nothing else " +
                        "changes — you can still start a moment yourself, and " +
                        "turn these on later under Account. Android may not ask " +
                        "again, so from here it would have to be system settings.",
                )
                TextLink("I have granted it — check again") {
                    granted = ActivityTransitions.hasPermission(context)
                    if (granted) ask()
                }
                PrimaryAction("Continue without cues", onClick = onNext)
            }

            failed -> {
                SmallCopy(
                    "Harbor could not start listening. Google Play services may " +
                        "be unavailable on this phone. Cues stay off rather than " +
                        "pretending to work.",
                )
                PrimaryAction("Continue", onClick = onNext)
            }

            else -> {
                PrimaryAction("Yes, notice for me", onClick = ::ask)
                TextLink("Not now", onNext)
            }
        }
    }
}

@Composable
private fun YourPace(store: HarborRepository, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()
    val t = settings.thresholds

    fun set(next: Thresholds) = scope.launch { store.setSettings(settings.copy(thresholds = next)) }

    Page {
        PageIntro(
            eyebrow = "Step five",
            title = "How often should Harbor speak up?",
            subtitle = "These are good defaults. You can change them any time.",
        )
        Surface {
            Stepper(
                label = "A walk counts after",
                value = "${t.walkingMinutes} min",
                onDown = { set(t.copy(walkingMinutes = (t.walkingMinutes - 1).coerceAtLeast(1))) },
                onUp = { set(t.copy(walkingMinutes = (t.walkingMinutes + 1).coerceAtMost(120))) },
            )
            Stepper(
                label = "At most, each day",
                value = "${t.dailyCap}",
                onDown = { set(t.copy(dailyCap = (t.dailyCap - 1).coerceAtLeast(1))) },
                onUp = { set(t.copy(dailyCap = (t.dailyCap + 1).coerceAtMost(10))) },
            )
            Stepper(
                label = "And never closer than",
                value = "${t.cooldownMinutes} min",
                onDown = { set(t.copy(cooldownMinutes = (t.cooldownMinutes - 30).coerceAtLeast(1))) },
                onUp = { set(t.copy(cooldownMinutes = (t.cooldownMinutes + 30).coerceAtMost(1440))) },
            )
        }
        PrimaryAction("Continue", onClick = onNext)
    }
}
