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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.harbor.data.HarborStore
import app.harbor.ui.ContactScreen
import app.harbor.ui.CuesSetupScreen
import app.harbor.ui.GardenScreen
import app.harbor.ui.HomeScreen
import app.harbor.ui.NotesScreen
import app.harbor.ui.ScheduleScreen
import app.harbor.ui.SettingsScreen
import app.harbor.ui.theme.HarborTheme

/**
 * Six screens, still on a state flag rather than a navigation library.
 *
 * It is now close to the point where that stops being reasonable — the
 * argument for holding out is that every destination here is one level deep
 * from home, so there is no back stack worth modelling. If a screen ever needs
 * to push another, reach for real navigation rather than nesting flags.
 */
class MainActivity : ComponentActivity() {

    private enum class Screen { Home, Cues, Contact, Garden, Schedule, Settings, Notes }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = HarborStore(applicationContext)

        setContent {
            HarborTheme {
                var screen by remember { mutableStateOf(Screen.Home) }
                val home = { screen = Screen.Home }

                // Back returns to home from anywhere else, and leaves the app
                // from home itself.
                BackHandler(enabled = screen != Screen.Home) { home() }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val inset = Modifier.padding(padding)

                    when (screen) {
                        Screen.Home -> HomeScreen(
                            store = store,
                            onOpenGarden = { screen = Screen.Garden },
                            onOpenCues = { screen = Screen.Cues },
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenNotes = { screen = Screen.Notes },
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

                        Screen.Notes -> NotesScreen(
                            store = store,
                            onDone = home,
                            modifier = inset,
                        )

                        Screen.Schedule -> ScheduleScreen(
                            store = store,
                            onDone = { screen = Screen.Settings },
                            modifier = inset,
                        )

                        Screen.Settings -> SettingsScreen(
                            store = store,
                            onEditSchedule = { screen = Screen.Schedule },
                            onDone = home,
                            modifier = inset,
                        )
                    }
                }
            }
        }
    }
}
