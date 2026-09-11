package app.harbor.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.CallStats
import app.harbor.domain.Contact
import app.harbor.domain.Field
import app.harbor.domain.Flowers
import app.harbor.domain.Garden
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.ui.theme.SurfaceGreen
import kotlinx.coroutines.launch

/**
 * The garden from inside it.
 *
 * The plan view answers what you have grown. This answers what it is like to
 * stand in it — a field of buds that opens as you walk through, which is a
 * feeling a hundred dots seen from above cannot carry.
 *
 * All the arithmetic lives in [Field] and is unit-tested. This file is the
 * part that cannot be: gestures, colour, and the order things are painted in.
 *
 * ## Flowers are circles on purpose, for now
 *
 * Each bloom is one draw call, using the petal and heart colours the flower
 * library already defines, so the placeholder is on-palette rather than a grey
 * dot. When the real artwork arrives it replaces the body of [drawBloom] and
 * nothing else moves — not the projection, not the gestures, not the tests.
 */
@Composable
fun FieldCanvas(store: HarborRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    // Oldest first, so a flower's index — and therefore its place in the
    // world — never changes once planted.
    val blooms = remember(contacts, entries) {
        val grown = entries
            .filter { it.resolution == Resolution.CALLED && it.flower != null }
            .sortedBy { it.occurredAt }
            .groupBy { it.contactId }

        Field.layout(
            contacts.mapIndexed { index, contact ->
                Field.Cluster(
                    contactId = contact.id,
                    plot = Garden.plotFor(contact.id.toString(), index),
                    flowers = grown[contact.id].orEmpty().map { it.flower!! to it.callMinutes },
                )
            },
        )
    }

    val byId = remember(contacts) { contacts.associateBy { it.id } }

    var camera by remember { mutableStateOf(Field.openingCamera(blooms)) }
    var chosen by remember { mutableStateOf<Field.Bloom?>(null) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    // Re-aim when the garden changes size, not on every recomposition.
    LaunchedEffect(blooms.size) {
        if (blooms.isNotEmpty()) camera = Field.openingCamera(blooms)
    }

    fun flyTo(target: Field.Camera) {
        scope.launch {
            val from = camera
            animate(0f, 1f, animationSpec = tween(520)) { t, _ ->
                camera = Field.Camera(
                    x = from.x + (target.x - from.x) * t,
                    z = from.z + (target.z - from.z) * t,
                    height = from.height + (target.height - from.height) * t,
                )
            }
        }
    }

    val projected = remember(blooms, camera, frame) {
        Field.project(blooms, camera, frame.width.toDouble(), frame.height.toDouble())
    }
    val focus = remember(projected, chosen) { Field.focused(projected, chosen) }

    val sky = MaterialTheme.colorScheme.background
    val ground = SurfaceGreen
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { frame = it }
                .pointerInput(blooms) {
                    // Drag walks: up the screen is forward, across is sideways.
                    // Pinch lifts the eye, which tips this view towards the
                    // plan the other mode shows.
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Pace comes from the size of the field, and rises a
                        // little as the eye lifts, so the ground moves under
                        // the finger at about the same rate either way.
                        val pace = Field.pace(blooms) *
                            (camera.height / Field.EYE).coerceIn(0.7, 2.5)
                        camera = Field.clamp(
                            camera.copy(
                                x = camera.x - pan.x * pace,
                                z = camera.z - pan.y * pace * 1.4,
                                height = camera.height / zoom,
                            ),
                            blooms,
                        )
                        chosen = null
                    }
                }
                .pointerInput(projected) {
                    detectTapGestures { at ->
                        val hit = Field.hit(projected, at.x.toDouble(), at.y.toDouble())
                        chosen = hit?.bloom
                        if (hit != null) flyTo(Field.facing(hit.bloom, blooms))
                    }
                },
        ) {
            val horizon = size.height * Field.HORIZON.toFloat()

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sky, Color(0xFFEFE6D2)),
                    startY = 0f,
                    endY = horizon,
                ),
                size = Size(size.width, horizon),
            )
            // The ground runs to the horizon and pales as it gets there, which
            // is the only depth cue a flat colour can give.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(ground.copy(alpha = 0.45f), ground),
                    startY = horizon,
                    endY = size.height,
                ),
                topLeft = Offset(0f, horizon),
                size = Size(size.width, size.height - horizon),
            )

            // Far to near: the painter's algorithm is the whole depth buffer.
            val focused = focus?.bloom
            projected.forEach { p ->
                drawBloom(p, focusing = focused != null && Field.same(p.bloom, focused))
            }
        }

        if (focus != null) {
            val who = focus.bloom.contactId?.let { byId[it] }
            if (who != null) {
                FieldLabel(who, focus.bloom.minutes, ink, muted)
            }
        }

        // Bare ground with no explanation is indistinguishable from a bug --
        // it was one, twice, while this was being built.
        if (projected.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (blooms.isEmpty()) {
                            "Nothing planted here yet."
                        } else {
                            "Nothing in view."
                        },
                        style = MaterialTheme.typography.titleLarge.copy(color = ink),
                    )
                    Text(
                        "Walk back, or switch to Top.",
                        style = MaterialTheme.typography.bodySmall.copy(color = muted),
                    )
                }
            }
        }
    }
}

/**
 * One bloom.
 *
 * A bud is a tight ball of the deep petal colour; opening fades it to the
 * lighter petal, grows it, and brings up a heart and a stem. That progression
 * is the whole animation — there is no keyframe anywhere. It is a function of
 * where you are standing.
 */
private fun DrawScope.drawBloom(p: Field.Projected, focusing: Boolean) {
    val radius = (p.size / 2).toFloat()
    if (radius < 0.4f) return

    val spec = Flowers.spec(p.bloom.kind)
    val alpha = p.alpha.toFloat()
    val open = p.openness.toFloat()
    val baseY = p.baseY.toFloat()
    val centre = Offset(p.screenX.toFloat(), baseY - radius)

    // Sitting on the ground rather than floating above it.
    drawOval(
        color = StemGreen.copy(alpha = 0.16f * alpha),
        topLeft = Offset(centre.x - radius * 0.85f, baseY - radius * 0.2f),
        size = Size(radius * 1.7f, radius * 0.42f),
    )

    if (open > 0.25f && radius > 3f) {
        drawLine(
            color = StemGreen.copy(alpha = 0.55f * alpha * open),
            start = Offset(centre.x, baseY),
            end = Offset(centre.x, centre.y),
            strokeWidth = (radius * 0.13f).coerceAtLeast(1f),
        )
    }

    val petal = lerpColor(Color(spec.petalDeep), Color(spec.petal), open)
    drawCircle(color = petal.copy(alpha = alpha), radius = radius, center = centre)

    if (open > 0.4f && radius > 5f) {
        drawCircle(
            color = Color(spec.heart).copy(alpha = alpha * ((open - 0.4f) / 0.6f)),
            radius = radius * 0.32f,
            center = centre,
        )
    }

    if (focusing && radius > 6f) {
        drawCircle(
            color = FocusInk.copy(alpha = 0.5f * alpha),
            radius = radius * 1.28f,
            center = centre,
            style = Stroke(width = 2f),
        )
    }
}

private val StemGreen = Color(0xFF6E8A63)
private val FocusInk = Color(0xFF2F4A37)

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * k,
        green = from.green + (to.green - from.green) * k,
        blue = from.blue + (to.blue - from.blue) * k,
        alpha = from.alpha + (to.alpha - from.alpha) * k,
    )
}

/** Whose flower is in front of you, and how long that call ran. */
@Composable
private fun FieldLabel(who: Contact, minutes: Int?, ink: Color, muted: Color) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(who.label, style = MaterialTheme.typography.titleLarge.copy(color = ink))
            Text(
                minutes?.let { "a call of about " + CallStats.formatDuration(it) } ?: "a call",
                style = MaterialTheme.typography.bodySmall.copy(color = muted),
            )
        }
    }
}
