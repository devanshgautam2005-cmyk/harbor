package app.harbor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.harbor.cue.CallFlow
import app.harbor.data.HarborStore
import app.harbor.domain.LedgerEntry
import app.harbor.ui.ContactScreen
import app.harbor.ui.CuesSetupScreen
import app.harbor.ui.GardenScreen
import app.harbor.ui.HarborShell
import app.harbor.ui.HarborTab
import app.harbor.ui.HomeScreen
import app.harbor.ui.NotesScreen
import app.harbor.ui.PersonScreen
import app.harbor.ui.ScheduleScreen
import app.harbor.ui.SettingsScreen
import app.harbor.ui.theme.HarborTheme
import kotlinx.coroutines.launch

/**
 * The app shell and its destinations.
 *
 * Three tabs sit in the pill at the bottom, as the prototype has them; the
 * rest are pushed over the top and show a way back instead. A state flag is
 * still doing the work of a navigation library, which is defensible only
 * because nothing here is more than one level deep.
 */
class MainActivity : ComponentActivity() {

    private enum class Screen(val tab: HarborTab?, val title: String?) {
        Home(HarborTab.Home, null),
        Schedule(HarborTab.Schedule, null),
        Settings(HarborTab.Account, null),
        Cues(null, "Cues"),
        Contact(null, "Your person"),
        Garden(null, "Your garden"),
        Notes(null, "A line"),
        Person(null, null),
        Reflect(null, null),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = HarborStore(applicationContext)

        setContent {
            HarborTheme {
                var screen by remember { mutableStateOf(Screen.Home) }
                var reflecting by remember { mutableStateOf<LedgerEntry?>(null) }
                var showing by remember { mutableStateOf<java.util.UUID?>(null) }
                val scope = rememberCoroutineScope()
                val home = { screen = Screen.Home }

                BackHandler(enabled = screen != Screen.Home) { home() }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val inset = Modifier.padding(padding)

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
                                onOpenSettings = { screen = Screen.Settings },
                                onOpenNotes = { screen = Screen.Notes },
                                onOpenPerson = { id ->
                                    showing = id
                                    screen = Screen.Person
                                },
                                onReflect = { entry ->
                                    reflecting = entry
                                    screen = Screen.Reflect
                                },
                                modifier = inset,
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
                                onDone = home,
                                modifier = inset,
                            )

                            Screen.Reflect -> reflecting?.let { entry ->
                                CallFlow(
                                    who = store.contacts.value
                                        .firstOrNull { it.id == entry.contactId }?.label
                                        ?: "them",
                                    measuredMinutes = entry.callMinutes ?: 10,
                                    initialTopic = entry.topic,
                                    onPlant = { minutes, feeling, flower, topic ->
                                        // Amends the existing row: append is
                                        // keyed on the id, so this replaces
                                        // rather than duplicates.
                                        scope.launch {
                                            store.append(
                                                entry.copy(
                                                    callMinutes = minutes,
                                                    feeling = feeling,
                                                    flower = flower,
                                                    topic = topic ?: entry.topic,
                                                ),
                                            )
                                        }
                                    },
                                    onPulse = { pulse ->
                                        scope.launch {
                                            store.append(entry.copy(feedbackPulse = pulse))
                                        }
                                    },
                                    onDone = {
                                        reflecting = null
                                        screen = Screen.Garden
                                    },
                                )
                            } ?: run { screen = Screen.Home }
                        }
                    }
                }
            }
        }
    }
}
