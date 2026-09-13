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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.domain.Tone

/**
 * Harbor's building blocks, in the language of "Harbor Specimen - All Screens".
 *
 * Compose has no stylesheet, so a design has to become composables or it
 * becomes a hundred copies of the same padding number. Everything here is
 * layout and colour only; no behaviour lives in this file.
 *
 * ## What the reskin actually changed
 *
 * The old set was filled-and-rounded: cream cards at 24dp with no edge, bold
 * serif headings, a green pill button. Four moves account for nearly all of
 * the difference:
 *
 *  1. **Cards are frosted glass.** Near-white, laid on a grey ground, rimmed
 *     in [CardEdge] -- a line *lighter* than both. See [Surface].
 *  2. **The action is ink.** Every filled button in the specimen is near-black
 *     with ground-coloured text, and its label is serif.
 *  3. **Nothing is bold.** Headings and buttons are regular-weight serif;
 *     emphasis comes from size and air.
 *  4. **Radii collapsed.** 8dp for an action, 10dp for a card. The pill
 *     survives in exactly one place -- a chip -- because the specimen keeps it
 *     there.
 *
 * No screen's structure, order or controls changed. This is the same app in
 * different clothes.
 */

/** A card. The specimen draws them at 10px. */
private val CardShape = RoundedCornerShape(10.dp)

/** An action. The specimen draws them at 8px. */
private val ActionShape = RoundedCornerShape(8.dp)

/** A chip, and the only pill left in the design. */
private val ChipShape = RoundedCornerShape(99.dp)

/** The vertical rhythm every page is built on. */
@Composable
fun Flow(
    modifier: Modifier = Modifier,
    gap: Int = 14,
    content: @Composable ColumnScope.() -> Unit,
) = Column(modifier, verticalArrangement = Arrangement.spacedBy(gap.dp), content = content)

/** The page's own margin. */
fun Modifier.pageContent(): Modifier = padding(horizontal = 24.dp, vertical = 20.dp)

/**
 * A card: frosted glass laid on the page.
 *
 * In the specimen this is white at about 70% over the ground, rimmed with
 * white at 90%. The rim is the whole trick — it is lighter than the card *and*
 * lighter than the ground, so the edge reads as a catch of light rather than
 * as a border, and the card appears to float without any shadow at all.
 *
 * The fill is composited rather than genuinely translucent. See [Cream].
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, CardEdge, CardShape)
        .padding(18.dp),
    content = content,
)

/**
 * The gentler of two adjacent cards.
 *
 * This used to be pale green. It is neutral now on purpose: its one use holds
 * a contact's avatar, and the avatar already carries that person's tone.
 * Tinting the card as well gave the page two competing colours and left the
 * avatar with nothing to stand out against.
 */
@Composable
fun SoftSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, CardEdge, CardShape)
        .padding(18.dp),
    content = content,
)

/** Gold mixed into the card colour. The accent card, used sparingly. */
@Composable
fun GoldSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Flow(
    modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(SurfaceGold)
        .border(1.dp, CardEdge, CardShape)
        .padding(18.dp),
    content = content,
)

/** The serif title of a page, over its letterspaced label. */
@Composable
fun PageIntro(title: String, subtitle: String? = null, eyebrow: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp)) {
        eyebrow?.let {
            Eyebrow(it)
            Spacer(Modifier.size(10.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
        )
        subtitle?.let {
            Spacer(Modifier.size(8.dp))
            SmallCopy(it, size = 14)
        }
    }
}

/** A section's serif heading. Regular weight -- the size is the emphasis. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) = Text(
    text,
    modifier = modifier,
    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
)

/**
 * The specimen's caption: small, spaced wide, muted.
 *
 * The tracking is what makes this read as a catalogue label rather than a UI
 * string, so it is wider than a label would normally want.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    /** End-aligned when it is the right half of a [SectionHeader]. */
    textAlign: TextAlign? = null,
) = Text(
    text.uppercase(),
    modifier = modifier,
    textAlign = textAlign,
    style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        letterSpacing = 2.0.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
)

/** The muted voice Harbor explains itself in. */
@Composable
fun SmallCopy(text: String, modifier: Modifier = Modifier, size: Int = 13) = Text(
    text,
    modifier = modifier,
    style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = size.sp,
        lineHeight = (size * 1.6).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
)

/**
 * A quiet reassurance with a mark beside it.
 *
 * Almost always the privacy line. Muted rather than warned: this app never has
 * anything alarming to say, and styling it like a warning would make the
 * promise read as a caveat.
 */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) = Row(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
) {
    Box(
        Modifier
            .padding(top = 6.dp)
            .size(5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
    )
    SmallCopy(text)
}

/**
 * Initials on a tone, or nothing to show yet.
 *
 * The ring in card colour is what lets an avatar sit on a coloured surface
 * without looking stuck to it. With the chrome drained, this is now one of the
 * few places colour appears outside the garden, which is the point -- and the
 * initial is serif, because in the specimen a person's name always is.
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
            .padding(2.dp)
            .clip(CircleShape)
            .background(tone.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(label),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = size.text.sp,
                color = Ink,
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

/** The four tones, tuned to sit under [Ink] so there is one ink and not two. */
internal val Tone.fill: Color
    get() = when (this) {
        Tone.GREEN -> SurfaceGreen
        Tone.GOLD -> SurfaceGold
        Tone.ORANGE -> SurfaceOrange
        Tone.SKY -> SurfaceSky
    }

/** The same tone at full strength, for a mark too small to carry a tint. */
internal val Tone.mark: Color
    get() = when (this) {
        Tone.GREEN -> MarkGreen
        Tone.GOLD -> MarkGold
        Tone.ORANGE -> MarkOrange
        Tone.SKY -> MarkSky
    }

/**
 * The one thing a screen is actually asking for.
 *
 * Written here rather than reached for as a Material Button because a filled
 * Button brings Material's own shape, elevation and ripple, and those are the
 * three things this design most wants to not have.
 *
 * Ink, not green. Every filled action in the specimen is near-black with
 * ground-coloured text and a serif label -- a green button looks plausible and
 * is not what the design does.
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
        .clip(ActionShape)
        .background(
            if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(vertical = 15.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 17.sp,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * An action that must not compete with the primary one.
 *
 * The specimen's outline chip: a pill, a thin dark rule, a serif label and
 * nothing else. This is the one place the pill survives the reskin.
 */
@Composable
fun QuietAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = Box(
    modifier
        .clip(ChipShape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 14.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * A heading with its note, set on the same line.
 *
 * The specimen almost never leaves a heading alone: "Your people" carries
 * "one patch each" out at the right margin, in the same tracked caps as every
 * other caption. It is a small thing that does a lot of the work of making a
 * screen read as a printed page rather than a settings list.
 */
@Composable
fun SectionHeader(title: String, meta: String, modifier: Modifier = Modifier) = Row(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Bottom,
) {
    // Both halves are given a share of the row rather than pushed to its
    // ends.
    //
    // SpaceBetween puts an unbounded Text at each end and lets them overlap
    // when the two together are wider than the row, which on Account they
    // were - "What you call yourself" ran straight through "NEVER LEAVES THIS
    // PHONE". The title gets what it needs up to two thirds, the caption
    // takes the rest and wraps.
    SectionHeading(title, Modifier.weight(1f, fill = false))
    Eyebrow(meta, Modifier.weight(1f), textAlign = TextAlign.End)
}

/**
 * The quietest row in the design: one line of something, and where it came from.
 *
 * Frosted like a card but a fraction of the height, so a list of ten of them
 * still reads as a page rather than a stack of boxes. This is what the
 * specimen uses for the notes on home and for a conversation, and it is the
 * reason those screens look like a catalogue index instead of a feed.
 */
@Composable
fun QuietRow(text: String, meta: String, modifier: Modifier = Modifier) = Row(
    modifier
        .fillMaxWidth()
        .clip(ActionShape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, CardEdge, ActionShape)
        .padding(horizontal = 14.dp, vertical = 11.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        meta.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** A rule inside a card, between one row and the next. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) = Box(
    modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.outlineVariant),
)
