package app.harbor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.harbor.cue.CallFlow
import app.harbor.data.HarborStore
import app.harbor.data.StudyFile
import app.harbor.domain.FlowerKind
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.CallStats
import app.harbor.domain.Moment
import app.harbor.ui.ContactScreen
import app.harbor.ui.CuesSetupScreen
import app.harbor.ui.FlowerLanding
import app.harbor.ui.GardenScreen
import app.harbor.ui.HarborShell
import app.harbor.ui.HarborTab
import app.harbor.ui.HomeScreen
import app.harbor.ui.OnboardingScreen
import app.harbor.ui.NotesScreen
import app.harbor.ui.PersonScreen
import app.harbor.ui.ScheduleScreen
import app.harbor.ui.SettingsScreen
import app.harbor.ui.theme.HarborTheme
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * The app shell and its destinations.
 *
 * Three tabs sit in the pill at the bottom, as the prototype has them; the
 * rest are pushed over the top and show a way back instead. A state flag is
 * still doing the work of a navigation library, which is defensible only
 * because nothing here is more than one level deep.
 */
class MainActivity : ComponentActivity() {

    /**
     * Bumped every time this activity comes back to the front.
     *
     * The composition watches it so that returning from the dialer is
     * something the UI can react to. A plain counter rather than a lifecycle
     * observer: it needs no extra dependency and there is no ambiguity about
     * which of the several `LocalLifecycleOwner`s is in scope.
     */
    private var resumes by mutableIntStateOf(0)

    private var cameForward: Instant? = null

    override fun onResume() {
        super.onResume()
        resumes++
        cameForward = Instant.now()
        lifecycleScope.launch { store.note(Moment.APP_OPENED) }
    }

    /**
     * How long they stayed, recorded on the way out.
     *
     * Paired with APP_OPENED this is the session length the study wants, and
     * it costs nothing to keep: the alternative was asking participants at the
     * end of the week how often they had opened the app, which nobody knows.
     */
    override fun onPause() {
        super.onPause()
        val since = cameForward ?: return
        cameForward = null
        val seconds = Duration.between(since, Instant.now()).seconds.toInt()
        lifecycleScope.launch {
            store.note(Moment.APP_LEFT, value = seconds)
            // And refresh the study file, so nobody has to remember to export
            // it. See data/StudyFile - it writes to this app's own folder and
            // sends nothing anywhere.
            StudyFile.refresh(applicationContext, store)
        }
    }

    private enum class Screen(val tab: HarborTab?, val title: String?) {
        Home(HarborTab.Home, null),
        Schedule(HarborTab.Schedule, null),
        Settings(HarborTab.Account, null),
        Cues(null, "Cues"),
        Contact(null, "Your person"),
        Garden(null, "Your garden"),
        Notes(null, "A petal"),
        Person(null, null),
        Reflect(null, null),
    }

    private lateinit var store: HarborStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark bars, stated rather than inferred.
        //
        // enableEdgeToEdge() with no arguments picks its bar style from the
        // system's light/dark setting, not from the app's. Harbor is dark on
        // every phone (see HarborTheme), so on a phone in light mode the
        // platform would draw a dark clock and battery over the dusk ground,
        // where they all but disappear.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // A field as well as a local, because onResume and onPause need it
        // too and they run outside the composition.
        store = HarborStore(applicationContext)

        setContent {
            HarborTheme {
                var screen by remember { mutableStateOf(Screen.Home) }

                // Which screens get looked at, and in what order. A category
                // per screen; nothing about what was on it.
                LaunchedEffect(screen) { store.note(Moment.SCREEN, screen.name) }
                var reflecting by remember { mutableStateOf<LedgerEntry?>(null) }

                // The flower on its way into the field, drawn over whatever
                // is underneath. Null the rest of the time.
                var landing by remember { mutableStateOf<FlowerKind?>(null) }
                var landed by remember { mutableStateOf<java.util.UUID?>(null) }
                var showing by remember { mutableStateOf<java.util.UUID?>(null) }
                val scope = rememberCoroutineScope()
                val home = { screen = Screen.Home }

                // Null until we know, so the first frame is not the wrong
                // screen: flashing home at somebody who has never set the app
                // up would be the worst possible first impression of it.
                var onboarded by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) { onboarded = store.hasOnboarded() }

                // Shown once per call, so backing out of the reflection does
                // not fling you straight back into it on the next resume.
                var offered by remember { mutableStateOf<java.util.UUID?>(null) }

                /**
                 * Coming back from the dialer *is* the end of the call, near
                 * enough — it is the only signal available without reading the
                 * call log, which would cost a permission this app will not
                 * spend (ADR-002). So the flower flow opens on return rather
                 * than waiting behind a card on home: the reward should arrive
                 * while the call is still in the room.
                 *
                 * CueActivity does the same on its own resume, but it lives in
                 * a task excluded from recents, so returning to Harbor any
                 * other way lands here instead. This is the path that actually
                 * fires most of the time.
                 */
                LaunchedEffect(resumes, onboarded) {
                    if (onboarded != true) return@LaunchedEffect
                    val waiting = CallStats.pendingReflection(
                        store.recentEntries(),
                        Instant.now(),
                    ) ?: return@LaunchedEffect
                    // Not while they are mid-way through typing something of
                    // their own. Anywhere else, the flower takes the screen.
                    //
                    // Schedule used to be on this list, which is why a call
                    // started from the little window on that screen was the
                    // one call in the app that never got a flower: you came
                    // back to the screen you left, and the screen you left
                    // suppressed the reward. Drawing a week is not the kind
                    // of half-finished thought this guard is for.
                    val busy = screen == Screen.Reflect || screen == Screen.Contact ||
                        screen == Screen.Notes
                    if (waiting.id != offered && !busy) {
                        offered = waiting.id
                        reflecting = waiting
                        screen = Screen.Reflect
                        store.note(
                            Moment.CALL_RETURNED,
                            value = CallStats.minutesAway(waiting.occurredAt, Instant.now()),
                        )
                    }
                }

                /**
                 * The same landing, for a flower planted in the cue's own
                 * activity.
                 *
                 * CueActivity runs the reflection itself and then finishes,
                 * which drops the user back here with the flower already in
                 * the ground and nothing having been seen to happen. Rather
                 * than a second channel between the two, this notices a
                 * freshly planted row on the way back in. `landed` makes it
                 * once per flower.
                 */
                LaunchedEffect(resumes, onboarded) {
                    if (onboarded != true) return@LaunchedEffect
                    val fresh = store.recentEntries()
                        .filter { it.flower != null }
                        .maxByOrNull { it.occurredAt } ?: return@LaunchedEffect
                    val age = java.time.Duration.between(fresh.occurredAt, Instant.now())
                    if (fresh.id != landed && !age.isNegative &&
                        age < java.time.Duration.ofMinutes(3)
                    ) {
                        landed = fresh.id
                        landing = fresh.flower
                    }
                }

                BackHandler(enabled = onboarded == true && screen != Screen.Home) { home() }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    // imePadding here rather than on each screen: the app is
                    // edge to edge, so the window no longer resizes itself
                    // when the keyboard opens and every screen has to give
                    // back the inset. Without it the field you are typing in
                    // sits behind the keyboard — which is exactly what
                    // happened all through onboarding.
                    val inset = Modifier.padding(padding).imePadding()

                    // Home keeps the bottom inset and gives up the top one.
                    //
                    // Its field is full bleed and is meant to run under the
                    // status bar, the way the reference runs its sky under the
                    // clock. Nothing on home needs the top inset: the greeting
                    // sits at the *foot* of the field, so the only thing level
                    // with the clock is sky.
                    val homeInset = Modifier
                        .padding(bottom = padding.calculateBottomPadding())
                        .imePadding()

                    if (onboarded != true) {
                        if (onboarded == false) {
                            OnboardingScreen(
                                store = store,
                                onFinished = { onboarded = true },
                                modifier = inset,
                            )
                        }
                        return@Scaffold
                    }

                    HarborShell(
                        tab = screen.tab,
                        onSelect = { tab ->
                            screen = when (tab) {
                                HarborTab.Home -> Screen.Home
                                HarborTab.Schedule -> Screen.Schedule
                                HarborTab.Account -> Screen.Settings
                            }
                        },
                        onBack = if (screen.tab == null) home else null,
                        title = screen.title,
                    ) {
                        when (screen) {
                            Screen.Home -> HomeScreen(
                                store = store,
                                onOpenGarden = { screen = Screen.Garden },
                                onOpenCues = { screen = Screen.Cues },
                                onOpenNotes = { screen = Screen.Notes },
                                onOpenPerson = { id ->
                                    showing = id
                                    screen = Screen.Person
                                },
                                onReflect = { entry ->
                                    reflecting = entry
                                    screen = Screen.Reflect
                                },
                                modifier = homeInset,
                            )

                            Screen.Cues -> CuesSetupScreen(
                                store = store,
                                onEditContact = { screen = Screen.Contact },
                                onOpenGarden = { screen = Screen.Garden },
                                modifier = inset,
                            )

                            Screen.Contact -> ContactScreen(
                                store = store,
                                onDone = { screen = Screen.Cues },
                                modifier = inset,
                            )

                            Screen.Garden -> GardenScreen(store = store, modifier = inset)

                            Screen.Person -> PersonScreen(
                                store = store,
                                contactId = showing,
                                onLeaveLine = { screen = Screen.Notes },
                                onEdit = { screen = Screen.Contact },
                                modifier = inset,
                            )

                            Screen.Notes -> NotesScreen(
                                store = store,
                                onDone = home,
                                modifier = inset,
                            )

                            Screen.Schedule -> ScheduleScreen(
                                store = store,
                                onDone = home,
                                modifier = inset,
                            )

                            Screen.Settings -> SettingsScreen(
                                store = store,
                                onEditSchedule = { screen = Screen.Schedule },
                                onOpenCues = { screen = Screen.Cues },
                                onDone = home,
                                modifier = inset,
                            )

                            Screen.Reflect -> reflecting?.let { entry ->
                                // Each amendment builds on the last write, not
                                // on the row as it was when this screen opened.
                                // Both callbacks touch the same row, and
                                // copying twice from the original meant the
                                // pulse answer put the flower back to null.
                                var amended by remember(entry.id) {
                                    mutableStateOf(entry)
                                }
                                CallFlow(
                                    who = store.contacts.value
                                        .firstOrNull { it.id == entry.contactId }?.label
                                        ?: "them",
                                    // Timed from when the call was placed to
                                    // when they came back, not a stand-in ten
                                    // minutes. The row's own occurredAt is the
                                    // moment Harbor dialled.
                                    measuredMinutes = entry.callMinutes
                                        ?: CallStats.minutesAway(
                                            entry.occurredAt,
                                            Instant.now(),
                                        ),
                                    initialTopic = entry.topic,
                                    reducedMotion = store.settings.value.reducedMotion,
                                    onPlant = { minutes, flower, topic ->
                                        // Amends the existing row: append is
                                        // keyed on the id, so this replaces
                                        // rather than duplicates.
                                        val next = amended.copy(
                                            callMinutes = minutes,
                                            flower = flower,
                                            topic = topic ?: amended.topic,
                                        )
                                        amended = next
                                        scope.launch {
                                            store.append(next)
                                            store.note(
                                                Moment.FLOWER_PLANTED,
                                                flower.name,
                                                minutes,
                                            )
                                        }
                                    },
                                    onPulse = { pulse ->
                                        val next = amended.copy(feedbackPulse = pulse)
                                        amended = next
                                        scope.launch { store.append(next) }
                                    },
                                    onNotReached = {
                                        // Nothing to plant, and nothing to
                                        // ask about. The row stops claiming a
                                        // call happened and the flow ends.
                                        val next = amended.copy(
                                            resolution = Resolution.NOT_REACHED,
                                            callMinutes = null,
                                            feeling = null,
                                            flower = null,
                                        )
                                        amended = next
                                        scope.launch { store.append(next) }
                                        reflecting = null
                                        screen = Screen.Home
                                    },
                                    onDone = {
                                        // Home rather than the garden, and the
                                        // flower goes with them: the bloom
                                        // travels from the picker into the
                                        // field it was added to, so the reward
                                        // is something you watch happen rather
                                        // than something you go and verify.
                                        landing = amended.flower
                                        landed = amended.id
                                        reflecting = null
                                        screen = Screen.Home
                                    },
                                )
                            } ?: run { screen = Screen.Home }
                        }
                    }

                    // Over the top of everything, including the shell's nav
                    // pill: the flower is passing in front of the app, not
                    // inside one of its screens.
                    landing?.let { kind ->
                        FlowerLanding(
                            kind = kind,
                            modifier = inset,
                            reducedMotion = store.settings.value.reducedMotion,
                        ) { landing = null }
                    }
                }
            }
        }
    }
}
