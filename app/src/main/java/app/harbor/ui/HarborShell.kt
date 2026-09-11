package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.ui.theme.Gold

/**
 * The app shell, hand-translated from `app.tsx` and its `.app-header` and
 * `.bottom-nav` rules.
 *
 * A floating pill of three tabs, and nothing else. The prototype's wordmark
 * header is deliberately not here: it spent 70dp on every screen telling
 * someone which app they had opened.
 *
 * The pill is deliberately not a Material navigation bar either. It sits above
 * the content rather than dividing the screen, which keeps the garden feeling
 * like the whole surface rather than a pane with a bar under it.
 */
@Composable
fun HarborShell(
    tab: HarborTab?,
    onSelect: (HarborTab) -> Unit,
    onBack: (() -> Unit)?,
    title: String?,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            // No wordmark. It cost 70dp on every screen to tell someone which
            // app they had just opened, which they know — and on home that
            // space belongs to the garden.
            //
            // A pushed screen still gets a way back, because system back is
            // not a visible affordance and this is the only one.
            if (onBack != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 24.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Back",
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    title?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) { content() }

            // Room for the floating pill, so nothing hides beneath it.
            Spacer(Modifier.height(if (tab != null) 96.dp else 16.dp))
        }

        if (tab != null) {
            // .bottom-nav
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HarborTab.entries.forEach { candidate ->
                    NavItem(candidate, candidate == tab) { onSelect(candidate) }
                }
            }
        }
    }
}

enum class HarborTab(val label: String, val glyph: String) {
    Home("Home", "⌂"),
    Schedule("Schedule", "◴"),
    Account("Account", "●"),
}

/** `.nav-item` — dimmed until current, when its mark takes the gold disc. */
@Composable
private fun NavItem(tab: HarborTab, current: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (current) Gold else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                tab.glyph,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (current) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                ),
            )
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
                    .copy(alpha = if (current) 1f else 0.65f),
            ),
        )
    }
}
