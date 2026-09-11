package app.harbor.ui

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path as NativePath
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.data.HarborRepository
import app.harbor.domain.Field
import app.harbor.domain.FlowerKind
import app.harbor.domain.Flowers
import app.harbor.domain.LedgerEntry
import app.harbor.domain.Resolution
import app.harbor.domain.Terrain
import app.harbor.domain.Tone
import app.harbor.ui.theme.Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * The field.
 *
 * A port of the `fieldtrial.html` prototype, which is the design authority
 * for this screen. The terrain is in [Terrain], the layout and camera in
 * [Field]; this draws them and takes the gestures.
 *
 * ## There is no 2D mode
 *
 * Pulling back *is* the plan view. [Field.tiltFor] blends flat and
 * perspective as you zoom, so the overhead map and the landscape are two ends
 * of one dial rather than two screens, and the readout names where on that
 * dial you are.
 *
 * ## Why this draws on the native canvas
 *
 * The field is tens of thousands of cells. Compose's `drawCircle` per cell
 * would be tens of thousands of draw calls a frame; instead every cell of the
 * same colour is added to one path and the bucket is filled once, which is
 * what the prototype does and the reason it stays smooth. The paths and
 * paints are held across frames and rewound, so a frame allocates nothing.
 */
@Composable
fun FieldCanvas(store: HarborRepository, modifier: Modifier = Modifier) {
    val contacts by store.contacts.collectAsState()
    val settings by store.settings.collectAsState()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }

    LaunchedEffect(Unit) { entries = store.recentEntries() }

    val people = remember(contacts, entries) {
        val grown = entries
            .filter { it.resolution == Resolution.CALLED && it.flower != null }
            .groupBy { it.contactId }
        contacts.map { contact ->
            val theirs = grown[contact.id].orEmpty()
            Field.Person(
                contactId = contact.id,
                label = contact.label,
                calls = theirs.size,
                // A patch is planted with whatever has been chosen for it
                // most often, so its colour is something the user picked
                // rather than something assigned.
                flower = theirs.mapNotNull { it.flower }
                    .groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key
                    ?: defaultFlower(contact.tone),
            )
        }
    }

    val patches = remember(people) { Field.patches(people) }
    val palette = remember(patches) { Field.palette(patches) }

    // Tens of thousands of cells, each sampling several octaves of noise.
    // Fast, but not fast enough to sit on the frame that shows the screen.
    val cells by produceState(initialValue = emptyList<Field.Cell>(), patches) {
        value = withContext(Dispatchers.Default) { Field.cells(patches) }
    }

    var frame by remember { mutableStateOf(IntSize.Zero) }
    val base = remember(frame) {
        Field.overviewZoom(frame.width.toDouble(), frame.height.toDouble())
    }

    var cam by remember {
        mutableStateOf(Field.Camera(Terrain.FIELD_W / 2, Terrain.FIELD_H / 2, 0.2))
    }
    var goal by remember { mutableStateOf(cam) }
    var settled by remember { mutableStateOf(false) }

    // Frame the whole island once the size is known.
    LaunchedEffect(base) {
        if (!settled && base > 0 && frame.width > 0) {
            settled = true
            cam = Field.Camera(Terrain.FIELD_W / 2, Terrain.FIELD_H / 2, base)
            goal = cam
        }
    }

    // The camera chases its goal rather than snapping, which is what makes a
    // tap on a patch read as travelling there.
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos {
                val next = Field.Camera(
                    x = cam.x + (goal.x - cam.x) * 0.13,
                    y = cam.y + (goal.y - cam.y) * 0.13,
                    zoom = cam.zoom + (goal.zoom - cam.zoom) * 0.13,
                )
                if (next != cam) cam = next
            }
        }
    }

    val tagInk = MaterialTheme.colorScheme.background

    // Held across frames so drawing allocates nothing.
    val kit = remember(palette.size) { DrawKit(palette.size) }

    Box(modifier.clip(RoundedCornerShape(30.dp))) {

        FieldSky(settings.weather, Modifier.fillMaxSize())

        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { frame = it }
                .pointerInput(base) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (base <= 0) return@detectTransformGestures
                        val tilt = Field.tiltFor(goal.zoom, base)
                        val nextZoom = Field.clampZoom(goal.zoom * zoom, base)
                        goal = Field.Camera(
                            x = goal.x - pan.x / goal.zoom,
                            // Dragging up the screen has to cover more ground
                            // once the view has tipped, or the world feels
                            // stuck to the finger near the horizon.
                            y = goal.y - (pan.y / goal.zoom) * (1 + tilt * 2.4),
                            zoom = nextZoom,
                        )
                    }
                }
                .pointerInput(patches, base) {
                    detectTapGestures { at ->
                        if (base <= 0 || frame.height == 0) return@detectTapGestures
                        val lens = Field.buildLens(cam, base, frame.height.toDouble())
                        val point = Field.Point()
                        var best: Field.Patch? = null
                        var bestDist = 76.0
                        for (patch in patches) {
                            Field.project(
                                patch.x, patch.y, Terrain.heightAt(patch.x, patch.y),
                                cam, lens, frame.width.toDouble(), frame.height.toDouble(),
                                point,
                            )
                            val d = hypot(point.x - at.x, point.y - at.y)
                            if (d < bestDist) {
                                bestDist = d
                                best = patch
                            }
                        }
                        best?.let { goal = Field.Camera(it.x, it.y, base * 6.5) }
                    }
                },
        ) {
            if (cells.isEmpty() || base <= 0) return@Canvas
            drawField(cells, patches, palette, cam, base, kit, tagInk)
        }

        FieldControls(
            onIn = { goal = goal.copy(zoom = Field.clampZoom(goal.zoom * 1.45, base)) },
            onOut = { goal = goal.copy(zoom = Field.clampZoom(goal.zoom / 1.45, base)) },
            onFit = {
                goal = Field.Camera(Terrain.FIELD_W / 2, Terrain.FIELD_H / 2, base)
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        )

        if (base > 0) {
            FieldReadout(
                relative = cam.zoom / base,
                tilt = Field.tiltFor(cam.zoom, base),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
    }
}

/** Paths and paints reused every frame. */
private class DrawKit(buckets: Int) {
    val paths = Array(buckets) { NativePath() }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val rock = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD93B3D39.toInt() }
    val till = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xCC7C5B3D.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x5733553D
        pathEffect = DashPathEffect(floatArrayOf(5f, 6f), 0f)
    }
    val tagBack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7C5B3D.toInt() }
    val tagText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val ring = NativePath()
    val stem = NativePath()
    val rect = RectF()
    val point = Field.Point()
}

private class Tag(val text: String, val x: Float, var y: Float, val stemY: Float)

private fun DrawScope.drawField(
    cells: List<Field.Cell>,
    patches: List<Field.Patch>,
    palette: LongArray,
    cam: Field.Camera,
    base: Double,
    kit: DrawKit,
    tagInk: Color,
) {
    val unit = 1.dp.toPx()
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    val lens = Field.buildLens(cam, base, h)
    val canvas = drawContext.canvas.nativeCanvas
    val p = kit.point

    for (path in kit.paths) path.rewind()
    kit.stem.rewind()

    // Flowers and rock are drawn after the bulk, so they sit on top of it.
    val blooms = ArrayList<FloatArray>()
    var tillWidth = -1f

    for (i in cells.indices) {
        val c = cells[i]
        Field.project(c.x, c.y, c.z, cam, lens, w, h, p)
        if (p.x < -26 || p.x > w + 26 || p.y < -26 || p.y > h + 26) continue

        var r = c.size * Field.DOT_SCALE * p.s
        if (r < 0.1) continue

        when (c.kind) {
            Field.Kind.CROSS -> {
                if (r > 0.5) {
                    if (tillWidth < 0) tillWidth = maxOf(0.6, r * 0.4).toFloat()
                    val a = (r * 1.1).toFloat()
                    val x = p.x.toFloat()
                    val y = p.y.toFloat()
                    kit.stem.moveTo(x - a, y - a); kit.stem.lineTo(x + a, y + a)
                    kit.stem.moveTo(x + a, y - a); kit.stem.lineTo(x - a, y + a)
                }
            }

            Field.Kind.SQUARE -> {
                val s = (r * 1.6).toFloat()
                canvas.drawRect(
                    (p.x - s / 2).toFloat(), (p.y - s / 2).toFloat(),
                    (p.x + s / 2).toFloat(), (p.y + s / 2).toFloat(),
                    kit.rock,
                )
            }

            Field.Kind.FLOWER -> {
                // Close enough in, a planted dot opens into the flower it
                // was standing for. Nothing is animated; it simply got big.
                if (r > Field.FLOWER_AT) {
                    blooms += floatArrayOf(
                        p.x.toFloat(), p.y.toFloat(), r.toFloat(),
                        c.patch.toFloat(), c.tone.toFloat(), c.paint.toFloat(),
                    )
                } else {
                    if (r < 0.75) r = 0.75
                    kit.paths[c.paint].addCircle(p.x.toFloat(), p.y.toFloat(), r.toFloat(), NativePath.Direction.CW)
                }
            }

            Field.Kind.DOT -> {
                if (r < 0.75) r = 0.75
                kit.paths[c.paint].addCircle(p.x.toFloat(), p.y.toFloat(), r.toFloat(), NativePath.Direction.CW)
            }
        }
    }

    for (bucket in kit.paths.indices) {
        if (kit.paths[bucket].isEmpty) continue
        kit.fill.color = palette[bucket].toInt()
        kit.fill.alpha = (Field.alphaFor(bucket) * 255).toInt()
        canvas.drawPath(kit.paths[bucket], kit.fill)
    }

    if (!kit.stem.isEmpty) {
        kit.till.strokeWidth = if (tillWidth > 0) tillWidth else 0.6f
        canvas.drawPath(kit.stem, kit.till)
    }

    drawPatchOutlines(canvas, patches, cam, lens, w, h, kit, unit)

    for (b in blooms) {
        drawFlower(canvas, patches[b[3].toInt()].flower, b[0], b[1], b[2], b[4], b[5].toInt(), kit)
    }

    drawTags(canvas, patches, cam, lens, w, h, kit, tagInk, unit)
}

private fun drawPatchOutlines(
    canvas: android.graphics.Canvas,
    patches: List<Field.Patch>,
    cam: Field.Camera,
    lens: Field.Lens,
    w: Double,
    h: Double,
    kit: DrawKit,
    unit: Float,
) {
    kit.outline.strokeWidth = 1.2f * unit
    for (patch in patches) {
        kit.ring.rewind()
        var visible = false
        val pts = ArrayList<Offset>(patch.ring.size)
        for (spot in patch.ring) {
            Field.project(spot.x, spot.y, Terrain.heightAt(spot.x, spot.y), cam, lens, w, h, kit.point)
            if (kit.point.x > -90 && kit.point.x < w + 90 && kit.point.y > -90 && kit.point.y < h + 90) {
                visible = true
            }
            pts += Offset(kit.point.x.toFloat(), kit.point.y.toFloat())
        }
        if (!visible) continue
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            if (i == 0) kit.ring.moveTo(a.x, a.y)
            kit.ring.quadTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2)
        }
        kit.ring.close()
        canvas.drawPath(kit.ring, kit.outline)
    }
}

/**
 * One flower.
 *
 * Petals are ellipses walked around the centre, rotated to face outward, in
 * the kind's own colours — the same shapes [FlowerMark] draws in the
 * reflection flow, so a flower looks like itself wherever it appears.
 */
private fun drawFlower(
    canvas: android.graphics.Canvas,
    kind: FlowerKind,
    x: Float,
    y: Float,
    r: Float,
    tone: Float,
    paint: Int,
    kit: DrawKit,
) {
    val spec = Flowers.spec(kind)
    val petals = spec.petals
    val spin = tone * 6.283185f
    kit.fill.color = (if (paint % 2 == 1) spec.petal else spec.petalDeep).toInt()
    kit.fill.alpha = 255
    for (i in 0 until petals) {
        val a = (i.toFloat() / petals) * 6.283185f + spin
        val cx = x + kotlin.math.cos(a) * r * 0.5f
        val cy = y + kotlin.math.sin(a) * r * 0.5f
        canvas.save()
        canvas.rotate(a * 57.29578f, cx, cy)
        kit.rect.set(cx - r * 0.44f, cy - r * 0.3f, cx + r * 0.44f, cy + r * 0.3f)
        canvas.drawOval(kit.rect, kit.fill)
        canvas.restore()
    }
    kit.fill.color = spec.heart.toInt()
    canvas.drawCircle(x, y, r * 0.3f, kit.fill)
}

/** Whose patch is whose, with colliding labels nudged apart. */
private fun drawTags(
    canvas: android.graphics.Canvas,
    patches: List<Field.Patch>,
    cam: Field.Camera,
    lens: Field.Lens,
    w: Double,
    h: Double,
    kit: DrawKit,
    ink: Color,
    unit: Float,
) {
    val tagH = 20 * unit
    val lift = 34 * unit
    val pad = 20 * unit
    kit.tagText.textSize = 11 * unit
    kit.tagText.color = android.graphics.Color.argb(
        255,
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt(),
    )
    val tags = ArrayList<Tag>()
    for (patch in patches) {
        Field.project(patch.x, patch.y, Terrain.heightAt(patch.x, patch.y), cam, lens, w, h, kit.point)
        if (kit.point.x < -50 || kit.point.x > w + 50) continue
        if (kit.point.y < -20 || kit.point.y > h + 40) continue
        // The zoom controls own the top-right corner; a tag under them is
        // unreadable anyway.
        if (kit.point.x > w - 80 * unit && kit.point.y < 210 * unit) continue
        tags += Tag(
            patch.label.uppercase() + " · " + patch.calls,
            kit.point.x.toFloat(),
            kit.point.y.toFloat(),
            kit.point.y.toFloat(),
        )
    }

    tags.sortBy { it.y }
    for (i in 1 until tags.size) {
        for (j in 0 until i) {
            if (kotlin.math.abs(tags[i].x - tags[j].x) < 104 * unit &&
                kotlin.math.abs(tags[i].y - tags[j].y) < 28 * unit
            ) {
                tags[i].y = tags[j].y + 28 * unit
            }
        }
    }

    for (tag in tags) {
        val wide = kit.tagText.measureText(tag.text) + pad
        val top = tag.y - lift - tagH
        kit.rect.set(tag.x - wide / 2, top, tag.x + wide / 2, top + tagH)
        canvas.drawRoundRect(kit.rect, 5 * unit, 5 * unit, kit.tagBack)
        // Only an unnudged tag still points at its patch; a moved one would
        // point at open ground.
        if (kotlin.math.abs(tag.y - tag.stemY) < 2) {
            kit.ring.rewind()
            kit.ring.moveTo(tag.x - 5 * unit, top + tagH)
            kit.ring.lineTo(tag.x + 5 * unit, top + tagH)
            kit.ring.lineTo(tag.x, top + tagH + 7 * unit)
            kit.ring.close()
            canvas.drawPath(kit.ring, kit.tagBack)
        }
        canvas.drawText(tag.text, tag.x, top + tagH * 0.72f, kit.tagText)
    }
}

/** Zoom in, zoom out, and pull back to the whole island. */
@Composable
private fun FieldControls(
    onIn: () -> Unit,
    onOut: () -> Unit,
    onFit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ControlButton("+", "Zoom in", onIn)
        ControlButton("−", "Zoom out", onOut)
        ControlButton("⤡", "Pull back", onFit)
    }
}

@Composable
private fun ControlButton(glyph: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Where you are on the dial between map and landscape. */
@Composable
private fun FieldReadout(relative: Double, tilt: Double, modifier: Modifier = Modifier) {
    val stage = when {
        tilt < 0.02 -> "plan"
        tilt > 0.98 -> "landscape"
        else -> "tipping"
    }
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            String.format("%.1f×", relative),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = muted),
        )
        Box(
            Modifier
                .width(46.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(tilt.toFloat().coerceIn(0f, 1f))
                    .background(Gold),
            )
        }
        Text(
            stage,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = muted),
        )
    }
}

/** What a patch is planted with before anyone has chosen a flower for it. */
private fun defaultFlower(tone: Tone): FlowerKind = when (tone) {
    Tone.GOLD -> FlowerKind.MARIGOLD
    Tone.GREEN -> FlowerKind.DAISY
    Tone.ORANGE -> FlowerKind.POPPY
    Tone.SKY -> FlowerKind.BLUEBELL
}
