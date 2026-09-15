package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.domain.BlockKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * The two things you put on your week.
 *
 * A **thorn** is time you are busy: spiked, dark, obviously something you
 * would not want to be interrupted in the middle of. A **flower** is time you
 * would welcome a call. Both are drawn rather than shipped as assets, for the
 * same reason the garden's flowers are — they have to size to whatever slot
 * they land in, and a PNG stretched to four hours of a Tuesday looks like a
 * mistake.
 *
 * The colours are measured off the Figma frames, not guessed at. The one
 * liberty taken is that spikes and petal heads are allowed to overflow their
 * block: a thorn whose spikes were clipped to its own time span would read as
 * a rectangle with serrations, which is not the same drawing at all.
 */

// --- measured off the frames ----------------------------------------------

private val ThornEdge = Color(0xFF4A6743)
private val ThornDeep = Color(0xFF26462C)
private val ThornLit = Color(0xFF4C6C34)
private val SpikeTop = Color(0xFF4E6A50)
private val SpikeFoot = Color(0xFF33502F)

private val PetalLight = Color(0xFFFFCA8E)
private val PetalDeep = Color(0xFFFFB987)
private val ThroatTop = Color(0xFFFF5151)
private val ThroatFoot = Color(0xFFFF6158)
private val StemTop = Color(0xFFFF9778)
private val StemFoot = Color(0xFFFF6E68)

/**
 * A thorn filling [body], with its spikes outside it.
 *
 * [body] is the block's own time span, so the top of the drawn body is exactly
 * the hour it starts. The spikes are extra.
 */
internal fun DrawScope.drawThorn(body: Rect) {
    val spike = (body.width * 0.17f).coerceIn(2.5f, 9f)
    val left = body.left + spike
    val right = body.right - spike
    if (right <= left) return
    val width = right - left

    // Light at the top right, dark through the middle, light again at the
    // bottom left: a sweep across the diagonal rather than down the face,
    // which is what stops a tall thorn reading as a flat bar.
    val skin = Brush.linearGradient(
        colors = listOf(ThornEdge, ThornDeep, ThornLit),
        start = Offset(right, body.top),
        end = Offset(left, body.bottom),
    )
    val bristle = Brush.verticalGradient(
        colors = listOf(SpikeTop, SpikeFoot),
        startY = body.top,
        endY = body.bottom,
    )

    val path = Path()

    // Down each long edge, one row offset half a step from the other so the
    // silhouette does not come out symmetrical.
    val step = (width * 0.62f).coerceAtLeast(6f)
    val rows = (body.height / step).toInt().coerceAtLeast(1)
    val pitch = body.height / rows
    val halfBase = (pitch * 0.32f).coerceAtMost(width * 0.30f)
    for (i in 0 until rows) {
        val y = body.top + pitch * (i + 0.5f)
        path.spikeAt(left, y, -spike, halfBase, vertical = true)
        val mirrored = y + pitch * 0.5f
        if (mirrored < body.bottom) {
            path.spikeAt(right, mirrored, spike, halfBase, vertical = true)
        }
    }

    // Three across the cap and three across the foot.
    val acrossHalf = (width / 3f * 0.34f)
    for (i in 0 until 3) {
        val x = left + width * (i + 0.5f) / 3f
        path.spikeAt(body.top, x, -spike, acrossHalf, vertical = false)
        path.spikeAt(body.bottom, x, spike, acrossHalf, vertical = false)
    }

    drawPath(path, bristle)
    drawRoundRect(
        brush = skin,
        topLeft = Offset(left, body.top),
        size = Size(width, body.height),
        cornerRadius = CornerRadius(width * 0.26f),
    )
}

/**
 * One triangle, pointing out of an edge by [reach].
 *
 * [along] runs down the edge for a vertical side and across it for a
 * horizontal one; [edge] is the edge's own coordinate.
 */
private fun Path.spikeAt(
    edge: Float,
    along: Float,
    reach: Float,
    halfBase: Float,
    vertical: Boolean,
) {
    if (vertical) {
        moveTo(edge, along - halfBase)
        lineTo(edge + reach, along)
        lineTo(edge, along + halfBase)
    } else {
        moveTo(along - halfBase, edge)
        lineTo(along, edge + reach)
        lineTo(along + halfBase, edge)
    }
    close()
}

/**
 * A flower filling [body]: a head at the top, and a stem down the rest.
 *
 * The head is sized off the column rather than off the block, so half an hour
 * and four hours grow the same flower and only the stem gets longer. It is
 * allowed to sit slightly proud of the block's top edge, exactly as the
 * frames draw it.
 */
internal fun DrawScope.drawFlowerBlock(body: Rect) {
    val headR = (body.width * 0.46f).coerceAtLeast(3f)
    val cx = body.center.x
    val cy = body.top + headR * 0.74f

    // Stem first: the petals overlap its shoulders, which is what gives the
    // head somewhere to sit rather than something to float above.
    val stemLeft = body.left + body.width * 0.09f
    val stemRight = body.right - body.width * 0.09f
    if (body.bottom > cy && stemRight > stemLeft) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(StemTop, StemFoot),
                startY = cy,
                endY = body.bottom,
            ),
            topLeft = Offset(stemLeft, cy),
            size = Size(stemRight - stemLeft, body.bottom - cy),
            cornerRadius = CornerRadius(body.width * 0.22f),
        )
    }

    val petals = Brush.verticalGradient(
        colors = listOf(PetalLight, PetalDeep),
        startY = cy - headR,
        endY = cy + headR,
    )
    val lobe = headR * 0.44f
    val orbit = headR * 0.56f
    for (i in 0 until 6) {
        val angle = Math.toRadians(i * 60.0 - 90.0)
        drawCircle(
            brush = petals,
            radius = lobe,
            center = Offset(
                cx + (orbit * cos(angle)).toFloat(),
                cy + (orbit * sin(angle)).toFloat(),
            ),
        )
    }
    drawCircle(brush = petals, radius = headR * 0.62f, center = Offset(cx, cy))

    val throatW = headR * 1.18f
    val throatH = headR * 0.80f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(ThroatTop, ThroatFoot),
            startY = cy - throatH / 2f,
            endY = cy + throatH / 2f,
        ),
        topLeft = Offset(cx - throatW / 2f, cy - throatH / 2f),
        size = Size(throatW, throatH),
        cornerRadius = CornerRadius(throatH * 0.36f),
    )
}

/**
 * What you are about to plant.
 *
 * The frames show a hand cursor resting on the thorn to say which one is
 * picked up. There is no cursor on a phone, so the chosen one takes a soft
 * tile behind it instead — without something, nothing on the screen says what
 * a press on the grid is going to put down.
 */
@Composable
internal fun PaletteChip(
    kind: BlockKind,
    label: String,
    selected: Boolean,
    tile: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) tile else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 46.dp, height = 58.dp)) {
            val body = Rect(
                left = size.width * 0.14f,
                top = size.height * 0.10f,
                right = size.width * 0.86f,
                bottom = size.height * 0.90f,
            )
            when (kind) {
                BlockKind.BUSY -> drawThorn(body)
                BlockKind.FREE -> drawFlowerBlock(body)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, color = ink),
        )
    }
}

/** The same two drawings, at whatever size the caller sizes the modifier to. */
@Composable
internal fun BlockGlyph(kind: BlockKind, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val body = Rect(0f, 0f, size.width, size.height)
        when (kind) {
            BlockKind.BUSY -> drawThorn(body)
            BlockKind.FREE -> drawFlowerBlock(body)
        }
    }
}
