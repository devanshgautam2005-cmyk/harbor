package app.harbor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.ui.theme.Hairline
import app.harbor.ui.theme.NavGlass

/** The pill the nav and its tabs are both cut from. */
private val NavShape = RoundedCornerShape(99.dp)

/**
 * The app shell.
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
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind { drawDusk() },
    ) {
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
                        // This Column is the whole window - the Scaffold's
                        // inset goes to the content inside, not to here - so
                        // without this the word "Back" sits under the clock.
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 24.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Back",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
            //
            // 72 rather than 88: the content inside already carries the
            // system navigation inset from the Scaffold, and the pill now sits
            // above that inset too, so the old number was reserving the same
            // band twice. Everything below the fold stopped short of the pill
            // by an inch of nothing and looked cut off.
            Spacer(Modifier.height(if (tab != null) 72.dp else 16.dp))
        }

        if (tab != null) {
            // A frosted pill, the same glass the cards are made of, holding
            // three sans labels. The bar used to be a block of colour with an
            // icon disc per tab; the design has neither, and against a ground
            // this dark the one amber tab is enough to say where you are.
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    // Above the system navigation bar, not on top of it. This
                    // Box is not inset by the Scaffold - it is the full window
                    // - so the pill has to step over the gesture bar itself.
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .clip(NavShape)
                    .background(NavGlass)
                    .border(1.dp, Hairline, NavShape)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HarborTab.entries.forEach { candidate ->
                    NavItem(candidate, candidate == tab) { onSelect(candidate) }
                }
            }
        }
    }
}

/**
 * The dusk every screen stands in.
 *
 * This is the design, not a backdrop to it. The reference it was drawn from is
 * a photograph of a sunset with a glass card laid across the middle of it, and
 * every screen here is built the same way: a lit gradient, and then translucent
 * white over the top. If this is weak, the cards have nothing to be windows
 * onto and the whole language collapses into "dark mode".
 *
 * Two layers, because one will not do it. The wash carries the colour down the
 * page; the glow is the sun itself, a small bright core just off the top edge
 * that keeps the brightest point genuinely bright rather than leaving the
 * gradient to peak at a mid-tone.
 *
 * Drawn rather than declared because the design states it in percentages of
 * the screen and a Brush needs pixels. Behind the content, so anything that
 * paints its own ground -- the field, the garden -- covers it rather than
 * fighting it.
 */
internal fun DrawScope.drawDusk() {
    // The wash: blue overhead, falling through ember to the ground.
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFF2E6B96),
                0.26f to Color(0xFF8A5230),
                0.46f to Color(0xFF52251C),
                0.68f to Color(0xFF1A1216),
                0.92f to Color(0x000D0E11),
            ),
            center = Offset(size.width / 2f, -size.height * 0.10f),
            radius = size.height * 0.78f,
        ),
        size = size,
    )
    // The sun, sitting just off the top edge.
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color(0x66FFC98C),
                0.45f to Color(0x33F0A35F),
                1.00f to Color(0x00F0783C),
            ),
            center = Offset(size.width / 2f, size.height * 0.02f),
            radius = size.width * 0.72f,
        ),
        size = size,
    )
}

enum class HarborTab(val label: String) {
    Home("Home"),
    Schedule("Schedule"),
    Account("Account"),
}

/**
 * Muted until current, when it takes the amber pill.
 *
 * Gold rather than `primary`: the design gives the current tab the brighter of
 * the two ambers and saves the [app.harbor.ui.theme.Ember] gradient for a
 * button. This is one of the exactly three places amber is allowed to appear.
 */
@Composable
private fun NavItem(tab: HarborTab, current: Boolean, onClick: () -> Unit) = Box(
    Modifier
        .clip(NavShape)
        .background(if (current) MaterialTheme.colorScheme.tertiary else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        tab.label,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 13.sp,
            color = if (current) MaterialTheme.colorScheme.onTertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
