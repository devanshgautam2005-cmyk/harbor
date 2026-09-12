package app.harbor.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.CallStats
import app.harbor.domain.Contact
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import androidx.core.net.toUri
import app.harbor.ui.theme.Avatar
import app.harbor.ui.theme.AvatarSize
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import java.time.Instant

/**
 * Home, hand-translated from `components/harbor/home.tsx`.
 *
 * The order is the prototype's and it is the argument: greeting, then the
 * garden, then how life feels, then your people. The garden sits above the
 * fold because it is the point of the app rather than a page you navigate to
 * — opening Harbor should show you what calling people has grown, not a
 * console for an app.
 */
@Composable
fun HomeScreen(
    store: HarborRepository,
    onOpenGarden: () -> Unit,
    onOpenCues: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenPerson: (java.util.UUID) -> Unit,
    onReflect: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by store.settings.collectAsState()
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val grown = entries.count { it.resolution == Resolution.CALLED && it.flower != null }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // .home-greeting
        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 14.dp)) {
            Eyebrow("A little closer, every day")
            Spacer(Modifier.size(4.dp))
            Text(
                if (settings.name.isBlank()) "Hey there." else "Hey, ${settings.name}.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
            )
            Spacer(Modifier.size(6.dp))
            SmallCopy(
                if (grown > 0) "$grown calls have grown here."
                else "Start growing today.",
            )
        }

        // .field-holder — the field itself, on the home screen, exactly as
        // the trial page has it. Tapping opens it full bleed.
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(404.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenGarden),
        ) {
            FieldCanvas(store, Modifier.fillMaxSize(), interactive = false)
        }

        Flow(Modifier.pageContent()) {
            WeatherBar(store)

            // A call Harbor watched you start and never heard about.
            CallStats.pendingReflection(entries, Instant.now())?.let { waiting ->
                Surface {
                    SectionHeading("How did that go?")
                    SmallCopy(
                        "You called " +
                            (contacts.firstOrNull { it.id == waiting.contactId }?.label
                                ?: "someone") +
                            " earlier. It only takes a moment, and it is what grows " +
                            "the flower.",
                    )
                    TextLink("Tell me") { onReflect(waiting) }
                }
            }

            SectionHeading("Your people")
            if (contacts.isEmpty()) {
                SmallCopy("Nobody yet. Add someone, and their patch appears above.")
                TextLink("Choose someone", onOpenCues)
            }
            contacts.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { contact ->
                        PersonTile(
                            contact = contact,
                            calls = entries.count {
                                it.resolution == Resolution.CALLED &&
                                    it.contactId == contact.id &&
                                    it.flower != null
                            },
                            usual = CallStats.usualMinutes(entries, contact.id),
                            onClick = { onOpenPerson(contact.id) },
                            onCall = contact.phoneE164?.let { number ->
                                {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, "tel:$number".toUri()),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // One word about today lives inside the weather card now, and
            // finding a moment and setting your pace live under Account. Home
            // is the garden, your people, and a quick way to say something.
            QuickShare(onLine = onOpenNotes, onPicture = onOpenNotes)
        }
    }
}

/** `.chat-tile` — a person, their patch, and how it has been going. */
@Composable
private fun PersonTile(
    contact: Contact,
    calls: Int,
    usual: Int?,
    onClick: () -> Unit,
    onCall: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Avatar(contact.label, contact.tone, size = AvatarSize.MD)
        Spacer(Modifier.size(8.dp))
        Text(
            contact.label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.size(5.dp))
        SmallCopy(
            usual?.let { "Calls run about ${CallStats.formatDuration(it)}." }
                ?: "Say hello whenever.",
            size = 13,
        )
        Spacer(Modifier.size(10.dp))
        SmallCopy(
            when (calls) {
                0 -> "No calls yet"
                1 -> "1 flower in their patch"
                else -> "$calls flowers in their patch"
            },
            size = 12,
        )

        // The whole point of the app, said out loud.
        //
        // Calling used to be a text link one screen in, which made the
        // commonest thing someone opens Harbor to do the least visible thing
        // on the page. Tapping the tile still opens them; this dials.
        if (onCall != null) {
            val onInk = MaterialTheme.colorScheme.onPrimary
            Spacer(Modifier.size(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onCall)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(15.dp)) {
                    drawHandset(this, onInk)
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "Call " + contact.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onInk,
                    ),
                )
            }
        }
    }
}

/** `.text-link` — a quiet way onward, never a button competing for attention. */
@Composable
internal fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * Saying something without opening anything.
 *
 * Leaving a line was a text link in a stack of text links, which made a thing
 * you might do daily look like a settings row. Two marks, the way a quick
 * share works everywhere else -- the point is that it reads as an action
 * before it reads as words.
 */
@Composable
private fun QuickShare(onLine: () -> Unit, onPicture: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickShareAction("Leave a line", QuickMark.Bubble, Modifier.weight(1f), onLine)
        QuickShareAction("Send a picture", QuickMark.Picture, Modifier.weight(1f), onPicture)
    }
}

private enum class QuickMark { Bubble, Picture }

@Composable
private fun QuickShareAction(
    label: String,
    mark: QuickMark,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val ink = MaterialTheme.colorScheme.onSurface
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(15.dp)) {
                when (mark) {
                    QuickMark.Bubble -> drawBubble(this, ink)
                    QuickMark.Picture -> drawPicture(this, ink)
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun drawBubble(scope: DrawScope, ink: Color) = with(scope) {
    val s = size.minDimension
    val line = Stroke(width = s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        Path().apply {
            moveTo(s * 0.5f, s * 0.12f)
            cubicTo(s * 0.92f, s * 0.12f, s * 0.92f, s * 0.68f, s * 0.5f, s * 0.68f)
            lineTo(s * 0.3f, s * 0.68f)
            lineTo(s * 0.16f, s * 0.88f)
            lineTo(s * 0.18f, s * 0.66f)
            cubicTo(s * 0.02f, s * 0.54f, s * 0.1f, s * 0.12f, s * 0.5f, s * 0.12f)
            close()
        },
        color = ink,
        style = line,
    )
}

private fun drawPicture(scope: DrawScope, ink: Color) = with(scope) {
    val s = size.minDimension
    val line = Stroke(width = s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(
        color = ink,
        topLeft = Offset(s * 0.12f, s * 0.18f),
        size = Size(s * 0.76f, s * 0.64f),
        cornerRadius = CornerRadius(s * 0.12f, s * 0.12f),
        style = line,
    )
    drawCircle(ink, radius = s * 0.07f, center = Offset(s * 0.34f, s * 0.38f))
    drawPath(
        Path().apply {
            moveTo(s * 0.18f, s * 0.74f)
            lineTo(s * 0.42f, s * 0.5f)
            lineTo(s * 0.62f, s * 0.68f)
            lineTo(s * 0.72f, s * 0.58f)
            lineTo(s * 0.84f, s * 0.72f)
        },
        color = ink,
        style = line,
    )
}

private fun drawHandset(scope: DrawScope, ink: Color) = with(scope) {
    val s = size.minDimension
    val line = Stroke(width = s * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        Path().apply {
            moveTo(s * 0.26f, s * 0.12f)
            lineTo(s * 0.44f, s * 0.12f)
            lineTo(s * 0.52f, s * 0.36f)
            lineTo(s * 0.38f, s * 0.46f)
            cubicTo(s * 0.46f, s * 0.64f, s * 0.58f, s * 0.74f, s * 0.72f, s * 0.8f)
            lineTo(s * 0.82f, s * 0.66f)
            lineTo(s * 0.94f, s * 0.76f)
            lineTo(s * 0.94f, s * 0.92f)
            cubicTo(s * 0.6f, s * 0.94f, s * 0.22f, s * 0.56f, s * 0.26f, s * 0.12f)
        },
        color = ink,
        style = line,
    )
}
