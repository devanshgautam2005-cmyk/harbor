package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.harbor.data.HarborRepository
import app.harbor.domain.Contact
import app.harbor.domain.FlowerKind
import app.harbor.domain.Flowers
import app.harbor.domain.Garden
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution

/**
 * The garden: one plot per person, one flower per call.
 *
 * The reward surface. It counts nothing and cannot be failed — a flower that
 * grew stays grown, which is what makes this a record of calls rather than a
 * score for them. See ADR-009 and docs/02.
 *
 * All geometry comes from [Garden], which is pure and tested against the
 * prototype's own JavaScript. This file only draws.
 */
@Composable
fun GardenScreen(store: HarborRepository, modifier: Modifier = Modifier) {
    val contacts by store.contacts.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val plots = remember(contacts) {
        contacts.mapIndexed { index, contact -> contact to Garden.plotFor(contact.id.toString(), index) }
    }

    // Calls with a flower, oldest first, so a flower's index — and therefore
    // its spot — never changes once planted.
    val flowersByContact = remember(entries) {
        entries
            .filter { it.resolution == Resolution.CALLED && it.flower != null }
            .sortedBy { it.occurredAt }
            .groupBy { it.contactId }
    }

    var camera by remember { mutableStateOf(Garden.Camera(0.0, 0.0, 1.0)) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val measurer = rememberTextMeasurer()
    val nameStyle = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF4A5347))

    // Fit once the Canvas has a size, and again if the garden grows. Done in
    // a side effect rather than during the draw phase: assigning state while
    // drawing is how you get a recomposition loop.
    LaunchedEffect(viewport, plots.size) {
        if (viewport.width > 0 && viewport.height > 0) {
            camera = Garden.fitCamera(
                plots.map { it.second },
                viewport.width.toDouble(),
                viewport.height.toDouble(),
            )
        }
    }

    Box(modifier.fillMaxSize().background(GROUND_SKY)) {
        if (plots.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Bare ground, for now.", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Add someone, and the first call you have plants the first flower.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            return@Box
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewport = it }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Zoom about the centroid so the ground under the
                        // fingers stays under them, then apply the pan.
                        val zoomed = Garden.zoomAt(
                            camera,
                            centroid.x.toDouble(),
                            centroid.y.toDouble(),
                            zoom.toDouble(),
                        )
                        camera = zoomed.copy(
                            x = zoomed.x + pan.x,
                            y = zoomed.y + pan.y,
                        )
                    }
                },
        ) {
            translate(camera.x.toFloat(), camera.y.toFloat()) {
                scale(camera.k.toFloat(), pivot = Offset.Zero) {
                    // Painter's order: plots further back are drawn first, so
                    // nearer ones overlap them the way an isometric scene
                    // should.
                    plots.sortedBy { it.second.depth }.forEach { (contact, plot) ->
                        drawPlot(
                            plot = plot,
                            contact = contact,
                            flowers = flowersByContact[contact.id].orEmpty().mapNotNull { it.flower },
                            detailed = camera.k >= Garden.DETAIL_ZOOM,
                            measurer = measurer,
                            nameStyle = nameStyle,
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPlot(
    plot: Garden.Plot,
    contact: Contact,
    flowers: List<FlowerKind>,
    detailed: Boolean,
    measurer: TextMeasurer,
    nameStyle: TextStyle,
) {
    translate(plot.x.toFloat(), plot.y.toFloat()) {
        // The ground: a closed Catmull-Rom loop around a noisy radius, so a
        // plot is organic and never a circle — and always the same shape for
        // this person.
        val ground = blobPath(Garden.blobPoints(plot.seed, plot.radius))
        scale(1f, Garden.GROUND_SQUASH.toFloat(), pivot = Offset.Zero) {
            drawPath(ground, color = toneOf(contact))
        }

        // Whose patch this is. sceneBounds already reserves room below each
        // plot for exactly this — without it the garden is a set of anonymous
        // blobs, which is a much colder thing to be shown.
        val label = measurer.measure(contact.label, nameStyle)
        drawText(
            textLayoutResult = label,
            topLeft = Offset(
                -label.size.width / 2f,
                (plot.radius * Garden.GROUND_SQUASH).toFloat() + 10f,
            ),
        )

        if (flowers.isEmpty()) return@translate

        if (detailed) {
            flowers.forEachIndexed { index, kind ->
                val spot = Garden.flowerSpot(plot.seed, index, plot.radius)
                translate(spot.x.toFloat(), spot.y.toFloat()) {
                    drawFlower(Flowers.spec(kind), radius = 11f)
                }
            }
        } else {
            // Zoomed out, one flower stands for the patch — otherwise a busy
            // garden turns to mush at a distance.
            val dominant = flowers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            if (dominant != null) {
                drawFlower(Flowers.spec(dominant), radius = 18f)
            }
        }
    }
}

/**
 * A closed Catmull-Rom loop through the given points, as cubic segments.
 *
 * The control-point formula is the prototype's, so the outline drawn here is
 * the outline drawn on the web.
 */
private fun blobPath(points: List<Garden.Spot>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    val n = points.size
    moveTo(points[0].x.toFloat(), points[0].y.toFloat())

    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]

        cubicTo(
            (p1.x + (p2.x - p0.x) / 6).toFloat(), (p1.y + (p2.y - p0.y) / 6).toFloat(),
            (p2.x - (p3.x - p1.x) / 6).toFloat(), (p2.y - (p3.y - p1.y) / 6).toFloat(),
            p2.x.toFloat(), p2.y.toFloat(),
        )
    }
    close()
}

private fun toneOf(contact: Contact): Color = when (contact.tone) {
    app.harbor.domain.Tone.GREEN -> Color(0xFF9CB682)
    app.harbor.domain.Tone.GOLD -> Color(0xFFC9AE6A)
    app.harbor.domain.Tone.ORANGE -> Color(0xFFC79A72)
    app.harbor.domain.Tone.SKY -> Color(0xFF8FA6B8)
}

private val GROUND_SKY = Color(0xFFEFF3E8)
