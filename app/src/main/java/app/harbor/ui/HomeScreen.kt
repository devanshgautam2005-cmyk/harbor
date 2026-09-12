package app.harbor.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.harbor.cue.Dialer
import app.harbor.data.HarborRepository
import app.harbor.domain.CallStats
import app.harbor.domain.Contact
import app.harbor.domain.FlowerKind
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.ui.theme.CardEdge
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.SectionHeader
import app.harbor.ui.theme.SectionHeading
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.Surface
import app.harbor.ui.theme.pageContent
import java.time.Instant

/**
 * Home, hand-translated from `components/harbor/home.tsx`.
 *
 * The garden sits high because it is the point of the app rather than a page
 * you navigate to — opening Harbor should show you what calling people has
 * grown, not a console for an app.
 *
 * But the prototype's order put how-life-feels between the garden and your
 * people, and on a real phone that pushed the call button — the single thing
 * this whole product exists to make easy — below the fold. So your people now
 * come straight after the garden, the greeting is one line shorter, the field
 * is shorter, and the weather follows rather than interrupts. Nothing was
 * removed; the one thing you might open Harbor to do is simply above the fold
 * now, which it was not.
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
    val scope = rememberCoroutineScope()
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
        // Two lines, not three. "A little closer, every day" was a tagline
        // above a greeting above a count, and the third of those is the only
        // one that says anything the reader did not already know.
        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 10.dp)) {
            Text(
                if (settings.name.isBlank()) "Hey there." else "Hey, ${settings.name}.",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
            )
            Spacer(Modifier.size(7.dp))
            // Caps, not a sentence. In the specimen the line under a greeting
            // is a catalogue caption rather than a second voice.
            Eyebrow(
                if (grown > 0) "$grown calls have grown here"
                else "A little closer, every day",
            )
        }

        // .field-holder — the field itself, on the home screen, exactly as
        // the trial page has it. Tapping opens it full bleed.
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                // 404dp put the call button off the bottom of the screen on
                // its own. The field is still the largest thing on the page.
                .height(304.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenGarden),
        ) {
            FieldCanvas(store, Modifier.fillMaxSize(), interactive = false)
        }

        Flow(Modifier.pageContent()) {
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

            SectionHeader("Your people", "one patch each")
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
                            flower = entries
                                .filter { it.contactId == contact.id && it.flower != null }
                                .maxByOrNull { it.occurredAt }
                                ?.flower,
                            onClick = { onOpenPerson(contact.id) },
                            // Through Dialer, not a bare intent: that is what
                            // writes the row the flower flow looks for when
                            // you come back (see cue/Dialer).
                            onCall = contact.phoneE164?.let {
                                { Dialer.handOff(context, store, scope, contact) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // How life feels, after the call button rather than in front of
            // it. Setting your mood is a thing you might do; calling your mum
            // is the thing the app is for.
            WeatherBar(store)

            // One word about today lives inside the weather card now, and
            // finding a moment and setting your pace live under Account. Home
            // is the garden, your people, and a quick way to say something.
            SendAPetal(onOpenNotes)
        }
    }
}

/** A person as a specimen: their patch under glass, and a way to call them. */
@Composable
private fun PersonTile(
    contact: Contact,
    calls: Int,
    usual: Int?,
    flower: FlowerKind?,
    onClick: () -> Unit,
    onCall: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clickable(onClick = onClick)) {
        Specimen(
            name = contact.label,
            caption = when (calls) {
                0 -> "nothing yet"
                1 -> "one flower"
                else -> "$calls flowers"
            },
            tone = contact.tone,
            flower = flower,
        )

        // The whole point of the app, said out loud.
        //
        // Calling used to be a text link one screen in, which made the
        // commonest thing someone opens Harbor to do the least visible thing
        // on the page. Tapping the specimen still opens them; this dials.
        if (onCall != null) {
            val onInk = MaterialTheme.colorScheme.onPrimary
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onCall)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(13.dp)) {
                    drawHandset(this, onInk)
                }
                Spacer(Modifier.size(7.dp))
                Text(
                    "Call " + contact.label,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 13.sp,
                        color = onInk,
                    ),
                )
            }
        }

        usual?.let {
            Spacer(Modifier.size(7.dp))
            Eyebrow("usually ${CallStats.formatDuration(it)}")
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
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * Saying something without opening anything.
 *
 * This used to be two tiles — "Leave a line" behind a speech bubble and "Send
 * a picture" behind a camera. Two borrowed icons for two things the app is
 * not, in front of what is really one act: the smallest thing you can send
 * somebody. One name and one mark now, and the mark is Harbor's own. Which of
 * the two you actually send is chosen on the screen it opens, where it is a
 * choice rather than a fork in the road.
 */
@Composable
private fun SendAPetal(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, CardEdge, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PetalMark(Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Send a petal",
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
            )
            SmallCopy("A line or a picture. Nothing owed back.", size = 12)
        }
    }
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
