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
import app.harbor.ui.theme.HarborTheme

/**
 * Two screens: the permission explainer with the cue switch, and the contact
 * the cue is about.
 *
 * A plain state flag rather than a navigation library. Two destinations do not
 * justify a dependency, and the screens the prototype has beyond these are not
 * built yet — when they are, this is the moment to reach for real navigation.
 */
class MainActivity : ComponentActivity() {

    private enum class Screen { Setup, Contact }

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
                            modifier = Modifier.padding(padding),
                        )

                        Screen.Contact -> ContactScreen(
                            store = store,
                            onDone = { screen = Screen.Setup },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
