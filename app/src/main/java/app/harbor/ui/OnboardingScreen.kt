package app.harbor.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.Contact
import app.harbor.domain.CueSound
import app.harbor.sensing.ActivityTransitions
import app.harbor.sensing.Sensing
import app.harbor.ui.theme.Avatar
import app.harbor.ui.theme.AvatarSize
import app.harbor.ui.theme.Notice
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The first run: five questions that grow one flower.
 *
 * Ported from the Figma flow. The spine of it is [PetalProgress] — each answer
 * earns a petal, so setting the app up *is* the first thing you grow, rather
 * than a form standing between you and the app. The five petals are your name,
 * who you would call, their picture, their sound, and when you are free.
 *
 * ## What the design did not include, and why it is still here
 *
 * Two screens in this file have no frame in Figma and must not be dropped.
 *
 * **The permission ask.** Harbor cannot notice a walk without
 * `ACTIVITY_RECOGNITION`, and cannot show a cue without `POST_NOTIFICATIONS`.
 * The design's "while walking" screen chooses the *behaviour* but never asks
 * Android for the right, so on its own it would produce an app that looks set
 * up and never fires. It sits straight after that choice, which is the moment
 * the ask makes sense.
 *
 * **What Harbor will never do.** The promise that your family install nothing
 * and are told nothing used to be made before any permission was mentioned,
 * because it is the worry the permission dialog raises on its own. It is kept
 * on the welcome screen for the same reason.
 *
 * ## Not yet wired
 *
 * Searching your contacts, a contact's photo, and Spotify are drawn as the
 * design has them but are disabled, pending the integrations. Each says so
 * rather than failing silently when tapped. Google Fit is deliberately absent:
 * Harbor already detects walking on-device, without an account or a network
 * (ADR-008), and routing that through Fit would give up both.
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

    Box(modifier.fillMaxSize().background(FlowGround)) {
        when (step) {
            0 -> Welcome(::next)
            1 -> YourName(store, ::next)
            2 -> WhoToCall(store, ::next)
            3 -> TheirPicture(store, ::next)
            4 -> TheirSound(store, ::next)
            5 -> WhenFree(::next)
            6 -> AskPermission(store, ::next)
            7 -> AlmostComplete(store, ::next)
            8 -> GoodJob(::next)
            9 -> OneLastThing(onSetUp = ::next, onSkip = ::finish)
            10 -> ScheduleScreen(store, onDone = ::finish)
            else -> finish()
        }
    }
}

// --- the flow's own surface -----------------------------------------------
//
// Measured off the Figma frames rather than taken from the app's theme: this
// flow is drawn on white with its own grey pill controls, and runs once before
// the person ever reaches the app proper.

private val FlowGround = Color.White
private val FlowInk = Color.Black
private val FieldFill = Color(0xFFD9D9D9)
private val PillIdle = Color(0x8FD9D9D9)
private val PillInk = Color(0x47000000)
private val MutedInk = Color(0x99000000)

/** Every question is set the same way: serif, centred, unhurried. */
@Composable
private fun Question(text: String, size: Int = 20) = Text(
    text,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth(),
    style = MaterialTheme.typography.titleLarge.copy(fontSize = size.sp, color = FlowInk),
)

/** The flow's page: flower at the top, question beneath, answer under that. */
@Composable
private fun FlowPage(
    petal: Int? = null,
    bloom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        if (petal != null) {
            PetalProgress(step = petal, modifier = Modifier.size(210.dp))
            Spacer(Modifier.height(40.dp))
        } else if (bloom) {
            PetalProgress(step = 5, modifier = Modifier.size(230.dp))
            Spacer(Modifier.height(36.dp))
        }
        content()
        Spacer(Modifier.height(48.dp))
    }
}

/** The grey pill the design types into. */
@Composable
private fun FlowField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier
            .width(236.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(FieldFill)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(FlowInk),
            textStyle = TextStyle(fontSize = 16.sp, color = FlowInk),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(placeholder, style = TextStyle(fontSize = 16.sp, color = MutedInk))
        }
    }
}

/** Bottom-right, and dimmed until the question has an answer. */
@Composable
private fun FlowNext(enabled: Boolean, label: String = "Next", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .clip(RoundedCornerShape(29.dp))
                .background(if (enabled) FieldFill else PillIdle)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 22.dp, vertical = 6.dp),
        ) {
            Question(label, size = 18)
        }
    }
}

/** A wide grey pill: the design's ordinary button. */
@Composable
private fun FlowPill(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = Box(
    modifier
        .clip(RoundedCornerShape(29.dp))
        .background(if (enabled) FieldFill else PillIdle)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 26.dp, vertical = 11.dp),
) {
    Text(
        label,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 18.sp,
            color = if (enabled) FlowInk else PillInk,
        ),
    )
}

/** Something the design shows but nothing is wired to yet. */
@Composable
private fun ComingSoon(label: String, note: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FlowPill(label, enabled = false) {}
        Spacer(Modifier.height(6.dp))
        Text(note, style = MaterialTheme.typography.labelSmall.copy(color = PillInk))
    }
}

// --- the five questions ---------------------------------------------------

/** What this is, what it will never do, and an offer to begin. */
@Composable
private fun Welcome(onNext: () -> Unit) = FlowPage(bloom = true) {
    Question("Let’s build our first Flower together", size = 22)
    Spacer(Modifier.height(14.dp))
    Question("Answer the questions\nto add petals", size = 18)
    Spacer(Modifier.height(28.dp))
    FlowPill("Continue", onClick = onNext)
    Spacer(Modifier.height(34.dp))

    // Said before any permission is mentioned, because this is the worry the
    // permission dialog will otherwise raise on its own.
    Surface {
        SectionHeading("What it will not do")
        SmallCopy(
            "Your family are not part of this. They install nothing, they are " +
                "never told anything, and they never see a thing you do here — " +
                "not your walking, not whether you answered, not even a summary.",
        )
        SmallCopy("There is no streak. Ignoring a cue costs you nothing.")
    }
}

/** Petal one. */
@Composable
private fun YourName(store: HarborRepository, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()
    var draft by remember { mutableStateOf(settings.name) }

    FlowPage(petal = 0) {
        Question("First, a little about yourself")
        Spacer(Modifier.height(46.dp))
        Question("What do we call you?")
        Spacer(Modifier.height(20.dp))
        FlowField(draft, "your name") { draft = it.take(40) }
        Spacer(Modifier.height(52.dp))
        FlowNext(enabled = draft.isNotBlank()) {
            scope.launch { store.setSettings(settings.copy(name = draft.trim())) }
            onNext()
        }
    }
}

/** Petal two. Contacts search is drawn but not wired, so the number is typed. */
@Composable
private fun WhoToCall(store: HarborRepository, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    val existing = contacts.firstOrNull()
    var name by remember { mutableStateOf(existing?.label.orEmpty()) }
    var number by remember { mutableStateOf(existing?.phoneE164.orEmpty()) }

    FlowPage(petal = 1) {
        Question("Who would you like to call more often")
        Spacer(Modifier.height(18.dp))
        Question("You can add more people later", size = 17)
        Spacer(Modifier.height(26.dp))

        ComingSoon("searching your contacts", "reading your contacts comes later")
        Spacer(Modifier.height(22.dp))

        FlowField(name, "their name") { name = it.take(40) }
        Spacer(Modifier.height(12.dp))
        FlowField(number, "their number") { number = it.take(20) }
        Spacer(Modifier.height(10.dp))
        Text(
            "Harbor only ever hands this to your dialler. It never places a call.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(color = PillInk),
        )

        Spacer(Modifier.height(36.dp))
        FlowNext(enabled = name.isNotBlank() && number.isNotBlank()) {
            scope.launch {
                store.upsertContact(
                    (existing ?: Contact(UUID.randomUUID(), name.trim(), number.trim()))
                        .copy(label = name.trim(), phoneE164 = number.trim()),
                )
            }
            onNext()
        }
    }
}

/** Petal three. Both ways to get a picture wait on reading your contacts. */
@Composable
private fun TheirPicture(store: HarborRepository, onNext: () -> Unit) {
    val contacts by store.contacts.collectAsState()
    val who = contacts.firstOrNull()

    FlowPage(petal = 2) {
        Question("Who would you like to call more often")
        Spacer(Modifier.height(34.dp))
        if (who != null) {
            Avatar(who.label, who.tone, size = AvatarSize.XL)
            Spacer(Modifier.height(14.dp))
            Question(who.label, size = 19)
        }
        Spacer(Modifier.height(30.dp))
        ComingSoon("Add a picture !", "choosing a photo comes later")
        Spacer(Modifier.height(16.dp))
        ComingSoon("Keep their profile picture", "needs your contacts")
        Spacer(Modifier.height(40.dp))
        FlowNext(enabled = true) { onNext() }
    }
}

/** Petal four. Spotify and the file picker wait; the built-in sounds work. */
@Composable
private fun TheirSound(store: HarborRepository, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState()

    FlowPage(petal = 3) {
        Question("What sound do you associate\nwith this person?")
        Spacer(Modifier.height(30.dp))

        ComingSoon("search Spotify", "Spotify comes later")
        Spacer(Modifier.height(24.dp))

        Question("browse ringtones", size = 17)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CueSound.entries.forEach { option ->
                val chosen = option == settings.sound
                Box(
                    Modifier
                        .clip(RoundedCornerShape(29.dp))
                        .background(if (chosen) FieldFill else PillIdle)
                        .clickable {
                            scope.launch { store.setSettings(settings.copy(sound = option)) }
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(
                        option.name.lowercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 16.sp,
                            color = if (chosen) FlowInk else PillInk,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        ComingSoon("browse files", "picking a file comes later")
        Spacer(Modifier.height(40.dp))
        FlowNext(enabled = true) { onNext() }
    }
}

/**
 * Petal five.
 *
 * The design attributes walking to Google Fit. Harbor reads it on-device
 * through the Activity Recognition Transition API instead, which needs no
 * account and no network (ADR-008), so the option stays and the attribution
 * goes. Watching which apps you use is a different promise entirely and is not
 * something this app is going to start doing quietly, so it is drawn and left
 * off.
 */
@Composable
private fun WhenFree(onNext: () -> Unit) {
    FlowPage(petal = 4) {
        Question("When would you be free for calls")
        Spacer(Modifier.height(30.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FieldFill)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.width(210.dp)) {
                Question("While walking", size = 18)
                Spacer(Modifier.height(4.dp))
                Text(
                    "noticed on this phone, never sent anywhere",
                    style = MaterialTheme.typography.labelSmall.copy(color = PillInk),
                )
            }
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(FlowGround),
                contentAlignment = Alignment.Center,
            ) { Question("✓", size = 15) }
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PillIdle)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                "While doomscrolling",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, color = PillInk),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "would mean Harbor watching which apps you open. Not yet, and not quietly.",
                style = MaterialTheme.typography.labelSmall.copy(color = PillInk),
            )
        }

        Spacer(Modifier.height(40.dp))
        FlowNext(enabled = true) { onNext() }
    }
}

// --- the parts the design did not draw, and the finish --------------------

/**
 * The screen the study lives or dies on.
 *
 * Kept from the previous onboarding, unchanged in behaviour. The system dialog
 * only ever appears after a deliberate tap, a refusal is accepted rather than
 * argued with, and because Android stops asking after two refusals the only
 * route left is system settings — which is what the recheck is for.
 *
 * It sits here, right after the walking choice, because that is the moment the
 * ask reads as *so I can catch a good moment to call her* rather than *so I can
 * watch you walk*.
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

    FlowPage {
        Question("May Harbor notice when\nyou stop walking?")
        Spacer(Modifier.height(10.dp))
        Question("This is the part that makes a cue arrive on its own.", size = 16)
        Spacer(Modifier.height(26.dp))

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
        Spacer(Modifier.height(26.dp))

        when {
            Sensing.isActive(context, store) -> {
                Notice("Cues are on. Harbor will wait for a real walk.")
                Spacer(Modifier.height(18.dp))
                FlowPill("Continue", onClick = onNext)
            }

            refused -> {
                SmallCopy(
                    "That is completely fine. Cues stay off and nothing else " +
                        "changes — you can still start a moment yourself, and " +
                        "turn these on later under Account. Android may not ask " +
                        "again, so from here it would have to be system settings.",
                )
                Spacer(Modifier.height(14.dp))
                TextLink("I have granted it — check again") {
                    granted = ActivityTransitions.hasPermission(context)
                    if (granted) ask()
                }
                Spacer(Modifier.height(10.dp))
                FlowPill("Continue without cues", onClick = onNext)
            }

            failed -> {
                SmallCopy(
                    "Harbor could not start listening. Google Play services may " +
                        "be unavailable on this phone. Cues stay off rather than " +
                        "pretending to work.",
                )
                Spacer(Modifier.height(14.dp))
                FlowPill("Continue", onClick = onNext)
            }

            else -> {
                FlowPill("Yes, notice for me", onClick = ::ask)
                Spacer(Modifier.height(12.dp))
                TextLink("Not now", onNext)
            }
        }
    }
}

/**
 * Four petals in, and a real cue to look at.
 *
 * Seeing one explains the product better than any screen about it, and it
 * costs nothing to show: a manual cue does not touch the daily allowance.
 */
@Composable
private fun AlmostComplete(store: HarborRepository, onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    val who = contacts.firstOrNull()

    FlowPage(petal = 4) {
        Question("Our flower is almost complete")
        Spacer(Modifier.height(34.dp))
        if (who != null) {
            FlowPill("check the notification out") {
                scope.launch { showManualCue(context, store, who) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "It never says anyone is calling you, because nobody is.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(color = PillInk),
            )
            Spacer(Modifier.height(22.dp))
        }
        TextLink("I’ve already seen it", onNext)
    }
}

/** The flower, whole. */
@Composable
private fun GoodJob(onNext: () -> Unit) = FlowPage(bloom = true) {
    Question("Good job!", size = 22)
    Spacer(Modifier.height(36.dp))
    FlowPill("Continue", onClick = onNext)
}

/** The one thing left, and a way past it. */
@Composable
private fun OneLastThing(onSetUp: () -> Unit, onSkip: () -> Unit) = FlowPage(bloom = true) {
    Question("One last thing,", size = 20)
    Spacer(Modifier.height(18.dp))
    Question("when are you busy so we know\nwhen NOT to send the cue.")
    Spacer(Modifier.height(30.dp))
    FlowPill("Set Up", onClick = onSetUp)
    Spacer(Modifier.height(18.dp))
    TextLink("Skip for now", onSkip)
}
