package app.harbor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import app.harbor.data.HarborStore
import app.harbor.ui.CuesSetupScreen
import app.harbor.ui.theme.HarborTheme

/**
 * For now this is the whole app: the permission and privacy explainer, and the
 * switch that turns cues on.
 *
 * That is deliberate rather than unfinished. Sensing cannot be tested on a
 * real phone until someone can grant the permission and enable cues, and a cue
 * surface built on a trigger nobody has watched fire is very hard to debug.
 * The remaining screens are build-order items 5 onward.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = HarborStore(applicationContext)

        setContent {
            HarborTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    CuesSetupScreen(
                        store = store,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
