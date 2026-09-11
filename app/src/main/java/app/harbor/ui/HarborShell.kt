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
 * A header that shows the mark on a top-level page and a way back elsewhere,
 * and a floating pill of three tabs. The pill is deliberately not a Material
 * navigation bar: it sits above the content rather than dividing the screen,
 * which keeps the garden feeling like the whole surface rather than a pane.
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
            // .app-header
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onBack == null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "harbor",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                        )
                        Text(
                            ".",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                            ),
                        )
                    }
                } else {
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
                        Text(it, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                    }
                    Spacer(Modifier.width(44.dp))
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
