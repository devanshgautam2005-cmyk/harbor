package app.harbor.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.domain.Tone

/**
 * Harbor's building blocks, hand-translated from the class rules in the
 * prototype's `app/globals.css`.
 *
 * Compose has no stylesheet, so a design expressed as CSS classes has to
 * become composables or it becomes a hundred copies of the same padding
 * number. Each of these names its source rule; if the CSS changes, this is the
 * file that changes with it.
 *
 * Everything here is layout and colour only. No behaviour lives in this file.
 */

/** `.flow` — the vertical rhythm every page is built on. */
@Composable
fun Flow(
    modifier: Modifier = Modifier,
    gap: Int = 20,
    content: @Composable ColumnScope.() -> Unit,
) = Column(modifier, verticalArrangement = Arrangement.spacedBy(gap.dp), content = content)

/** `.page-content` — 24px 28px. */
fun Modifier.pageContent(): Modifier = padding(horizontal = 28.dp, vertical = 24.dp)

/**
 * `.surface` — a raised card: cream, generously rounded, quietly lifted.
 *
 * The shadow is deliberately almost invisible. In the prototype it is
 * `0 8px 22px -18px` of the foreground colour at 45% — a suggestion of lift
 * rather than a drop shadow.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(22.dp),
    content = content,
)

/** `.soft-surface` — pale green, for the gentler of two adjacent things. */
@Composable
fun SoftSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(SurfaceGreen)
        .padding(22.dp),
    content = content,
)

/** `.gold-surface` — gold mixed into the card colour. Used sparingly. */
@Composable
fun GoldSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        // color-mix(accent 55%, card), resolved by hand.
        .background(Color(0xFFF6D68C))
        .padding(22.dp),
    content = content,
)

/** `.page-intro` — the serif title and its one line of explanation. */
@Composable
fun PageIntro(title: String, subtitle: String? = null, eyebrow: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp)) {
        eyebrow?.let {
            Eyebrow(it)
            Spacer(Modifier.size(8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-0.5).sp,
            ),
        )
        subtitle?.let {
            Spacer(Modifier.size(8.dp))
            SmallCopy(it, size = 15)
        }
    }
}

/** `.section-heading h2` — serif, 22px, tight. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) = Text(
    text,
    modifier = modifier,
    style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
)

/** `.eyebrow` — small, spaced, upper, muted. Labels a thing without shouting. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    modifier = modifier,
    style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 13.sp,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
)

/** `.small-copy` — the muted voice Harbor explains itself in. */
@Composable
fun SmallCopy(text: String, modifier: Modifier = Modifier, size: Int = 14) = Text(
    text,
    modifier = modifier,
    style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = size.sp,
        lineHeight = (size * 1.6).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
)

/**
 * `.notice` — a quiet reassurance with a mark beside it.
 *
 * Almost always the privacy line. Muted rather than warned: this app never
 * has anything alarming to say, and styling it like a warning would make the
 * promise read as a caveat.
 */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) = Row(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(9.dp),
) {
    Box(
        Modifier
            .padding(top = 5.dp)
            .size(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
    )
    SmallCopy(text)
}

/**
 * `.avatar` — initials on a tone, or nothing to show yet.
 *
 * Sized from the prototype's avatar-xs through avatar-xl. The 3px ring in
 * card colour is what lets an avatar sit on a coloured surface without
 * looking stuck to it.
 */
@Composable
fun Avatar(
    label: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MD,
) {
    Box(
        modifier
            .size(size.dp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
            .clip(CircleShape)
            .background(tone.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(label),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = size.text.sp,
                color = tone.ink,
            ),
        )
    }
}

enum class AvatarSize(val dp: Int, val text: Int) {
    XS(26, 12), SM(34, 15), MD(44, 18), LG(54, 21), XL(96, 36),
}

/** First letters of the first two words, as the prototype does it. */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "·" }

private val Tone.fill: Color
    get() = when (this) {
        Tone.GREEN -> SurfaceGreen
        Tone.GOLD -> Color(0xFFF6D68C)
        Tone.ORANGE -> SurfaceOrange
        Tone.SKY -> SurfaceSky
    }

private val Tone.ink: Color
    get() = when (this) {
        Tone.GREEN, Tone.SKY -> Forest
        Tone.GOLD, Tone.ORANGE -> DeepGreen
    }

/**
 * `.btn-primary` — the one thing a screen is actually asking for.
 *
 * Written here rather than reached for as a Material Button because a filled
 * Button brings Material's own shape, elevation and ripple, and those are the
 * three things this design most wants to not have. Forest on paper, flat,
 * softly rounded, full width by default.
 */
@Composable
fun PrimaryAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = Box(
    modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(999.dp))
        .background(
            if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(vertical = 16.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * `.btn-quiet` — an action that must not compete with the primary one.
 *
 * An outline and nothing else. Used where two actions sit together and only
 * one of them is the answer.
 */
@Composable
fun QuietAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = Box(
    modifier
        .clip(RoundedCornerShape(999.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 13.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
