package app.harbor

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import app.harbor.ui.theme.HarborTheme

/**
 * Three screens: the permission explainer with the cue switch, the contact the
 * cue is about, and the garden the calls grow in.
 *
 * Still a plain state flag rather than a navigation library. Once Home, Notes
 * and Schedule arrive this stops being reasonable — that is the moment to
 * reach for real navigation, not before.
 */
class MainActivity : ComponentActivity() {

    private enum class Screen { Setup, Contact, Garden }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = HarborStore(applicationContext)

        setContent {
            HarborTheme {
                var screen by remember { mutableStateOf(Screen.Setup) }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    when (screen) {
                        Screen.Setup -> CuesSetupScreen(
                            store = store,
                            onEditContact = { screen = Screen.Contact },
                            onOpenGarden = { screen = Screen.Garden },
                            modifier = Modifier.padding(padding),
                        )

                        Screen.Contact -> ContactScreen(
                            store = store,
                            onDone = { screen = Screen.Setup },
                            modifier = Modifier.padding(padding),
                        )

                        Screen.Garden -> GardenScreen(
                            store = store,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
