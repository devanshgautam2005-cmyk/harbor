package app.harbor.cue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.harbor.domain.Contact
import app.harbor.domain.FeedbackPulse
import app.harbor.domain.Resolution
import app.harbor.ui.theme.Avatar
import app.harbor.ui.theme.AvatarSize
import app.harbor.ui.theme.SmallCopy
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * The cue, hand-translated from `cue.tsx` and its `.cue-*` rules.
 *
 * Call-shaped and never claiming to be a call (ADR-009): the harbor mark sits
 * at the top, the copy is Harbor's own voice, and the privacy line is on the
 * screen itself. What makes it land is the person — their face, their sound —
 * not an impersonation of the system dialer.
 */
@Composable
internal fun CueSurface(
    contact: Contact?,
    usualMinutes: Int?,
    onRecord: (Resolution, Instant?, FeedbackPulse?) -> Unit,
    onCall: (topic: String?, number: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(CueStep.Cue) }
    var topic by remember { mutableStateOf<String?>(null) }
    var line by remember { mutableStateOf("") }
    var settled by remember { mutableStateOf(false) }

    BackHandler(enabled = step == CueStep.Cue) { onDismiss() }

    val who = contact?.label ?: "someone at home"
    val usual = usualMinutes ?: 12

    Column(
        Modifier
            .fillMaxSize()
            // .cue-screen — a sky that warms from green through paper to sand
            .background(
                Brush.linearGradient(
                    0f to Color(0xFFE7F0DF),
                    0.52f to Color(0xFFFBF1DE),
                    1f to Color(0xFFF5E7C9),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // .cue-top — whose app this is, and when. Never "incoming call".
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "harbor",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Text(
                "now",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        // .cue-person — the face, breathing
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PulsingRing {
                if (contact != null) {
                    Avatar(contact.label, contact.tone, size = AvatarSize.XL)
                } else {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                }
            }
            Text(
                who,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        when (step) {
            CueStep.Cue -> Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                CueTitle("Looks like you're free.")
                CueSub("You just stopped walking — a good moment, if you want it.")

                // .cue-length — the ask, with a stated size
                Box(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "calls with $who usually run ~$usual min",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }

                // .cue-topics — give the call a shape before it starts
                TOPICS.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            TopicChip(option, option == topic) {
                                topic = if (topic == option) null else option
                            }
                        }
                    }
                }

                Spacer(Modifier.size(2.dp))

                // .cue-paths — three ways through, equal weight, no default
                CuePath(
                    main = "Call now",
                    sub = "~$usual min, usually",
                    enabled = contact?.phoneE164 != null,
                ) {
                    settled = true
                    onCall(topic, contact?.phoneE164)
                }
                CuePath(main = "Send a reaction", sub = "one line, nothing owed") {
                    step = CueStep.React
                }
                CuePath(main = "Propose a later time", sub = "becomes today's next nudge") {
                    step = CueStep.Later
                }

                Spacer(Modifier.size(4.dp))
                CueOut("not now") { onDismiss() }

                // .cue-privacy
                SmallCopy(
                    "Your walking stays on this phone. $who never sees it.",
                    size = 13,
                )
            }

            CueStep.React -> Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                CueTitle("A little love, then.", small = true)
                CueSub("One line is plenty. No call, no explanation.")
                OutlinedTextField(
                    value = line,
                    onValueChange = { line = it.take(120) },
                    placeholder = { Text("thinking of you, that's all") },
                    modifier = Modifier.fillMaxWidth(),
                )
                CuePath(main = "Send it", sub = "nothing owed either way", enabled = line.isNotBlank()) {
                    settled = true
                    onRecord(Resolution.REACTED, null, null)
                    onDismiss()
                }
                CueOut("back") { step = CueStep.Cue }
            }

            CueStep.Later -> Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                CueTitle("When would suit you?", small = true)
                CueSub("A possibility, not a promise. It becomes today's next nudge and nothing more.")

                // Presets rather than a clock face. At the end of a walk, with
                // a phone half out of a pocket, three taps of a time picker is
                // friction the moment will not survive.
                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                listOf(
                    "In an hour" to now.plus(Duration.ofHours(1)),
                    "This evening" to now.atZone(zone).with(LocalTime.of(20, 0))
                        .let { if (it.toInstant().isAfter(now)) it.toInstant() else it.plusDays(1).toInstant() },
                    "Tomorrow" to now.atZone(zone).plusDays(1).with(LocalTime.of(18, 0)).toInstant(),
                ).forEach { (label, at) ->
                    CuePath(main = label, sub = "a reminder inside Harbor") {
                        settled = true
                        onRecord(Resolution.PROPOSED_LATER, at, null)
                        onDismiss()
                    }
                }
                CueOut("back") { step = CueStep.Cue }
            }
        }
    }
}

private enum class CueStep { Cue, React, Later }

/** The shapes a call can be given beforehand. Ported from the prototype. */
private val TOPICS = listOf("Catch up", "Ask for help", "Share news", "Just because")

/** `.cue-title` — serif, large, centred, tight. Harbor's own voice. */
@Composable
private fun CueTitle(text: String, small: Boolean = false) = Text(
    text,
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.displayMedium.copy(
        fontSize = if (small) 26.sp else 34.sp,
        lineHeight = if (small) 30.sp else 37.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
)

/** `.cue-sub` — one muted line under the title, deliberately narrow. */
@Composable
private fun CueSub(text: String) = Text(
    text,
    textAlign = TextAlign.Center,
    modifier = Modifier.widthIn(max = 320.dp),
    style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
)

/** `.cue-topic` — a pill that fills in when chosen. */
@Composable
private fun TopicChip(text: String, selected: Boolean, onClick: () -> Unit) = Text(
    text,
    modifier = Modifier
        .clip(RoundedCornerShape(99.dp))
        .background(
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        )
        .border(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(99.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 15.dp, vertical = 10.dp),
    style = MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
    ),
)

/**
 * `.cue-path` — one of the three ways through.
 *
 * A card with a mark, a serif line and a muted one. Equal visual weight
 * between them is the guardrail: no option is the default, and none is a
 * lesser answer.
 */
@Composable
private fun CuePath(
    main: String,
    sub: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
        Column {
            Text(
                main,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** `.cue-out` — the way out, underlined and quiet. Never styled as a loss. */
@Composable
private fun CueOut(text: String, onClick: () -> Unit) = Text(
    text,
    modifier = Modifier.clickable(onClick = onClick).padding(12.dp),
    style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.Underline,
    ),
)

/** `.cue-ring` with `cue-pulse` — the face breathes while the cue waits. */
@Composable
private fun PulsingRing(content: @Composable () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "cue-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "cue-pulse-scale",
    )
    Box(
        Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(6.dp),
    ) { content() }
}
