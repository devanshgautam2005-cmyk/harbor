package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.BusyWindow
import app.harbor.domain.FlowerKind
import app.harbor.domain.Windows
import app.harbor.ui.theme.Flow
import app.harbor.ui.theme.Eyebrow
import app.harbor.ui.theme.SectionHeader
import app.harbor.ui.theme.PageIntro
import app.harbor.ui.theme.SmallCopy
import app.harbor.ui.theme.pageContent
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * When the user is not reachable.
 *
 * The data source the in-class suppression rule was built without (ADR-011).
 * Self-entered: no campus API to depend on, no calendar permission, and
 * nobody asked for their college password.
 *
 * Not a port of the prototype's Schedule, which is built around sharing
 * availability with a parent and a mutual-consent handshake. There is no
 * parent side here (ADR-007), so there is nobody to share with.
 *
 * ## Why a grid and not a form
 *
 * This used to be day pills, two hour steppers and an "Add this block"
 * button. Entering an ordinary week of five classes ran to something like
 * sixty taps, and you never saw the week you were describing -- only a list
 * of times underneath it. People do not hold a timetable as text. They hold
 * it as a shape.
 *
 * So it is a week you draw on, the way a calendar or a piano roll works:
 * press empty space and drag to lay a block down, press a block to slide it
 * to another day or hour, and drag its bottom edge to make it longer.
 * Everything snaps to the half hour, which is the resolution a timetable
 * actually has.
 *
 * Press-and-drag rather than plain drag is deliberate: this grid sits inside
 * a scrolling page, and if a plain vertical drag drew blocks instead of
 * scrolling, the page would become a trap. The press is also the right feel
 * for putting something down.
 */
@Composable
fun ScheduleScreen(
    store: HarborRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val saved by store.busyWindows.collectAsState()

    // Held locally only while a gesture is in flight. Writing every frame of a
    // drag through to storage would be a prefs write per pointer event.
    var draft by remember { mutableStateOf<List<BusyWindow>?>(null) }
    var selected by remember { mutableStateOf<Int?>(null) }

    val blocks = draft ?: saved

    LaunchedEffect(saved.size) {
        if ((selected ?: -1) >= saved.size) selected = null
    }

    fun commit(next: List<BusyWindow>) {
        draft = null
        scope.launch { store.setBusyWindows(next) }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.padding(horizontal = 28.dp)) {
            PageIntro(
                eyebrow = "Room for real life",
                title = "When you are busy.",
                subtitle = "Press and drag to lay down a block. Harbor stays quiet " +
                    "during these.",
            )
        }

        Flow(Modifier.pageContent(), gap = 14) {
            // The sheet sets this promise as a caption rather than a notice:
            // the same words, in the voice the rest of the page labels things
            // in. Nothing is dropped from what it says.
            Eyebrow(
                "Only the times · no subjects, no locations · " +
                    "nothing leaves this phone",
            )

            // The sheet leads its schedule with this card, and it is the
            // best thing on the screen. Its copy says "your window" and never
            // "together": Harbor has no way to know anyone else's evening and
            // must never look as though it does. See domain/Windows.
            Windows.next(blocks, LocalDate.now().dayOfWeek, LocalTime.now())?.let { window ->
                LittleWindow(
                    headline = timeLabel(window.start) + " – " + timeLabel(window.end),
                    caption = window.minutes.toString() + " unhurried minutes, free today",
                    flower = FlowerKind.POPPY,
                )
            }

            SectionHeader("Your week", "press and drag to block time")

            WeekGrid(
                blocks = blocks,
                selected = selected,
                onSelect = { selected = it },
                onPreview = { draft = it },
                onCommit = { commit(it) },
            )

            val chosen = selected?.let { blocks.getOrNull(it) }
            if (chosen != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallCopy(
                        chosen.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                            "  " + timeLabel(chosen.start) + " to " + timeLabel(chosen.end),
                        modifier = Modifier.weight(1f),
                    )
                    Pill(text = "Remove", selected = false) {
                        val index = selected
                        if (index != null) {
                            selected = null
                            commit(blocks.filterIndexed { i, _ -> i != index })
                        }
                    }
                }
            } else {
                SmallCopy(
                    if (blocks.isEmpty()) {
                        "Nothing yet. Without any blocks, a cue can land during a class."
                    } else {
                        "Tap a block to remove it. Drag its bottom edge to make it longer."
                    },
                    size = 13,
                )
            }

            TextLink("Back", onDone)
        }
    }
}

// The window the grid shows. Before 7am and after 11pm someone is either
// asleep or already covered by the cue thresholds.
private const val FIRST_HOUR = 7
private const val LAST_HOUR = 23
private const val SNAP_MINUTES = 30
private val HOUR_HEIGHT = 46.dp
private val GUTTER = 38.dp

private enum class Grab { Move, ResizeEnd }

/**
 * The week, as something you draw on.
 *
 * Blocks are addressed by index, which is stable for the length of one
 * gesture because nothing else writes the list in that time.
 */
@Composable
private fun WeekGrid(
    blocks: List<BusyWindow>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onPreview: (List<BusyWindow>) -> Unit,
    onCommit: (List<BusyWindow>) -> Unit,
) {
    val days = DayOfWeek.entries
    val hours = LAST_HOUR - FIRST_HOUR
    val density = LocalDensity.current

    val line = MaterialTheme.colorScheme.outlineVariant
    val blockFill = MaterialTheme.colorScheme.primary
    val blockInk = MaterialTheme.colorScheme.onPrimary
    val accent = MaterialTheme.colorScheme.tertiary
    val edgeInk = MaterialTheme.colorScheme.onBackground

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(GUTTER))
            days.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        Spacer(Modifier.size(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(HOUR_HEIGHT * hours)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val columnWidth = (maxWidth - GUTTER) / days.size
            val colPx = with(density) { columnWidth.toPx() }
            val gutterPx = with(density) { GUTTER.toPx() }
            val hourPx = with(density) { HOUR_HEIGHT.toPx() }
            val edgePx = with(density) { 16.dp.toPx() }

            fun dayAt(x: Float): Int =
                ((x - gutterPx) / colPx).toInt().coerceIn(0, days.size - 1)

            fun minuteAt(y: Float): Int {
                val raw = FIRST_HOUR * 60 + (y / hourPx) * 60f
                val snapped = (raw / SNAP_MINUTES).roundToInt() * SNAP_MINUTES
                return snapped.coerceIn(FIRST_HOUR * 60, LAST_HOUR * 60)
            }

            // Read through a snapshot rather than closing over `blocks`.
            //
            // pointerInput restarts whenever one of its keys changes, so
            // keying it on the list meant the first preview frame of a drag
            // tore down the detector that was producing it: a block could be
            // created but never sized, and every drag ended as a cancel.
            val latest by rememberUpdatedState(blocks)

            fun hitTest(at: Offset): Pair<Int, Grab>? {
                latest.forEachIndexed { i, b ->
                    val x = gutterPx + colPx * days.indexOf(b.day)
                    val top = (b.start.toMinutes() - FIRST_HOUR * 60) / 60f * hourPx
                    val bottom = (b.end.toMinutes() - FIRST_HOUR * 60) / 60f * hourPx
                    if (at.x >= x && at.x < x + colPx && at.y >= top && at.y <= bottom) {
                        return i to if (at.y > bottom - edgePx) Grab.ResizeEnd else Grab.Move
                    }
                }
                return null
            }

            var index by remember { mutableStateOf(-1) }
            var grab by remember { mutableStateOf(Grab.Move) }
            var working by remember { mutableStateOf<List<BusyWindow>>(emptyList()) }
            var cursor by remember { mutableStateOf(Offset.Zero) }
            var grabOffset by remember { mutableStateOf(0) }

            Canvas(Modifier.fillMaxSize()) {
                for (h in 0..hours) {
                    val y = h * hourPx
                    drawLine(
                        color = line,
                        start = Offset(gutterPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                for (d in 1 until days.size) {
                    val x = gutterPx + colPx * d
                    drawLine(
                        color = line,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                }
            }

            for (h in 0 until hours) {
                Text(
                    shortHour(FIRST_HOUR + h),
                    modifier = Modifier
                        .offset(y = HOUR_HEIGHT * h + 4.dp)
                        .width(GUTTER)
                        .padding(start = 7.dp, end = 5.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }

            blocks.forEachIndexed { i, b ->
                val top = (b.start.toMinutes() - FIRST_HOUR * 60) / 60f
                val span = (b.end.toMinutes() - b.start.toMinutes()) / 60f
                val isSelected = i == selected
                val ink = if (isSelected) edgeInk else blockInk
                Box(
                    Modifier
                        .offset(
                            x = GUTTER + columnWidth * days.indexOf(b.day),
                            y = HOUR_HEIGHT * top,
                        )
                        .width(columnWidth)
                        .height(HOUR_HEIGHT * span)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) accent else blockFill)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, edgeInk, RoundedCornerShape(7.dp))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (span >= 0.75f) {
                        Text(
                            timeLabel(b.start),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = ink,
                            ),
                        )
                    }
                    // The grip. Visible so that "drag the edge" is something
                    // you can see rather than something you have to be told.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                            .width(18.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ink.copy(alpha = 0.55f)),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(colPx, hourPx) {
                        detectTapGestures { at -> onSelect(hitTest(at)?.first) }
                    }
                    .pointerInput(colPx, hourPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { at ->
                                cursor = at
                                val hit = hitTest(at)
                                if (hit == null) {
                                    // Lay a block down and start sizing it at
                                    // once, so a single press-and-drag both
                                    // creates it and sets how long it runs.
                                    val start = minuteAt(at.y)
                                    val end = (start + SNAP_MINUTES)
                                        .coerceAtMost(LAST_HOUR * 60)
                                    if (end > start) {
                                        working = latest + BusyWindow(
                                            day = days[dayAt(at.x)],
                                            start = minutesToTime(start),
                                            end = minutesToTime(end),
                                        )
                                        index = working.lastIndex
                                        grab = Grab.ResizeEnd
                                        grabOffset = 0
                                        onSelect(index)
                                        onPreview(working)
                                    }
                                } else {
                                    working = latest
                                    index = hit.first
                                    grab = hit.second
                                    grabOffset = minuteAt(at.y) -
                                        latest[hit.first].start.toMinutes()
                                    onSelect(index)
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                if (index in working.indices) {
                                    cursor += amount
                                    val b = working[index]
                                    val next = when (grab) {
                                        Grab.Move -> {
                                            val length =
                                                b.end.toMinutes() - b.start.toMinutes()
                                            val start = (minuteAt(cursor.y) - grabOffset)
                                                .coerceIn(
                                                    FIRST_HOUR * 60,
                                                    LAST_HOUR * 60 - length,
                                                )
                                            b.copy(
                                                day = days[dayAt(cursor.x)],
                                                start = minutesToTime(start),
                                                end = minutesToTime(start + length),
                                            )
                                        }

                                        Grab.ResizeEnd -> {
                                            val end = minuteAt(cursor.y).coerceIn(
                                                b.start.toMinutes() + SNAP_MINUTES,
                                                LAST_HOUR * 60,
                                            )
                                            b.copy(end = minutesToTime(end))
                                        }
                                    }
                                    working =
                                        working.toMutableList().also { it[index] = next }
                                    onPreview(working)
                                }
                            },
                            onDragEnd = {
                                if (index >= 0) onCommit(working)
                                index = -1
                            },
                            onDragCancel = {
                                if (index >= 0) onCommit(working)
                                index = -1
                            },
                        )
                    },
            )
        }
    }
}

private fun LocalTime.toMinutes(): Int = hour * 60 + minute

private fun minutesToTime(total: Int): LocalTime {
    val clamped = total.coerceIn(0, 23 * 60 + 59)
    return LocalTime.of(clamped / 60, clamped % 60)
}

private fun shortHour(hour: Int): String {
    val display = if (hour % 12 == 0) 12 else hour % 12
    return display.toString() + if (hour < 12) "a" else "p"
}

private fun timeLabel(at: LocalTime): String {
    val display = if (at.hour % 12 == 0) 12 else at.hour % 12
    val suffix = if (at.hour < 12) "am" else "pm"
    return if (at.minute == 0) {
        display.toString() + suffix
    } else {
        display.toString() + ":" + at.minute.toString().padStart(2, '0') + suffix
    }
}
