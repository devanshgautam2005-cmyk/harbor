package app.harbor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.domain.FlowerKind
import app.harbor.domain.Flowers
import app.harbor.domain.Tone
import app.harbor.ui.theme.CardEdge
import app.harbor.ui.theme.Forest
import app.harbor.ui.theme.Hairline
import app.harbor.ui.theme.LeafLight
import app.harbor.ui.theme.Stem
import app.harbor.ui.theme.fill

/**
 * A person, shown the way the specimen sheet shows a flower.
 *
 * This is the signature of the design and the thing a reskin of colour alone
 * cannot produce: a single stem standing under a thin glass arch, on a frosted
 * plinth that names it. It is why the sheet reads as a botanical catalogue
 * rather than a list of contacts, and it is what a person's patch looks like
 * on home now.
 *
 * The arch is drawn, not filled — one hairline, rounded hard at the top and
 * square at the foot, so the glass is implied rather than rendered. Everything
 * inside it is illustration and carries the only saturated colour on the page.
 *
 * An empty arch is deliberate: somebody with no calls yet gets the glass and
 * the label and nothing growing in it, which says "not yet" far better than an
 * empty state sentence would.
 */

/** The glass: a tall arch standing on a square foot. */
private val ArchShape = RoundedCornerShape(
    topStart = 46.dp,
    topEnd = 46.dp,
    bottomEnd = 4.dp,
    bottomStart = 4.dp,
)

/** The label the specimen stands on. */
private val PlinthShape = RoundedCornerShape(5.dp)

/** The little petal mark that carries a person's tone. */
private val MarkShape = RoundedCornerShape(
    topStart = 3.dp,
    topEnd = 3.dp,
    bottomEnd = 6.dp,
    bottomStart = 6.dp,
)

@Composable
fun Specimen(
    name: String,
    caption: String,
    tone: Tone,
    flower: FlowerKind?,
    modifier: Modifier = Modifier,
    archHeight: Int = 150,
) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(archHeight.dp)
                .padding(horizontal = 4.dp)
                .border(1.dp, Hairline, ArchShape),
        ) {
            if (flower != null) {
                Canvas(Modifier.fillMaxSize()) { drawSpecimen(flower) }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(PlinthShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, CardEdge, PlinthShape)
                .padding(horizontal = 9.dp, vertical = 7.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                )
                Box(Modifier.size(11.dp).clip(MarkShape).background(tone.fill))
            }
            Text(
                caption.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.3.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * One stem, from the foot of the arch to the bloom.
 *
 * Drawn rather than shipped as art for the same reason [FlowerMark] is: the
 * shape is a function of the flower's spec, so adding a flower to the library
 * stays a data change.
 */
private fun DrawScope.drawSpecimen(kind: FlowerKind) {
    val cx = size.width / 2f
    val foot = size.height * 0.97f
    val bloomY = size.height * 0.34f
    val unit = size.minDimension

    drawPath(
        Path().apply {
            moveTo(cx, foot)
            cubicTo(
                cx - unit * 0.04f, foot - unit * 0.22f,
                cx + unit * 0.03f, bloomY + unit * 0.24f,
                cx, bloomY,
            )
        },
        color = Stem,
        style = Stroke(width = unit * 0.035f, cap = StrokeCap.Round),
    )

    drawLeaf(cx, foot - unit * 0.20f, -1f, unit * 0.30f, Forest)
    drawLeaf(cx, foot - unit * 0.38f, 1f, unit * 0.26f, LeafLight)

    translate(left = cx, top = bloomY) {
        drawFlower(Flowers.spec(kind), unit * 0.26f)
    }
}

/** A leaf: out from the stem, and back to it. */
private fun DrawScope.drawLeaf(x: Float, y: Float, dir: Float, len: Float, colour: Color) {
    drawPath(
        Path().apply {
            moveTo(x, y)
            cubicTo(
                x + dir * len * 0.50f, y - len * 0.45f,
                x + dir * len * 0.95f, y - len * 0.30f,
                x + dir * len, y - len * 0.02f,
            )
            cubicTo(
                x + dir * len * 0.62f, y + len * 0.18f,
                x + dir * len * 0.22f, y + len * 0.14f,
                x, y,
            )
        },
        color = colour,
    )
}
