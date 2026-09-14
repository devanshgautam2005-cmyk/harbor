package app.harbor.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * The flower library, and how a call becomes one.
 *
 * The colours, petal counts and names are measured off the flower sheet,
 * which is the authority on them now.
 *
 * Twenty flowers, and each of them is a feeling rather than a species. What a
 * flower is *called* is the biggest thing this file carries: the screen after
 * a call asks how it went, so the shelf has to be a list of answers to that
 * question. "Sunflower — the long, good kind" asked somebody to translate; the
 * sheet's "Loved · you belong" does not.
 *
 * The notes are the sheet's own second line, and they are addressed to the
 * person planting them. That is why the hard ones are gentle: Lonely says "you
 * are not alone" and Anxious says "still growing", because the moment somebody
 * picks one of those is not the moment to be neutral at them.
 *
 * Changing a value here changes every flower in the app at once: the bloom
 * after a call, a person's specimen, the picker, and the field.
 */
data class FlowerSpec(
    val kind: FlowerKind,
    val name: String,
    /** What this flower means, shown when picking one. */
    val note: String,
    val petal: Long,
    val petalDeep: Long,
    val heart: Long,
    val petals: Int,
    val shape: Shape,
) {
    enum class Shape { ROUND, POINT, CUP }
}

object Flowers {

    val LIBRARY: List<FlowerSpec> = listOf(
        FlowerSpec(FlowerKind.HAPPY, "Happy", "A brighter you.",
            0xFFFFD95E, 0xFFF0A81E, 0xFFB9740C, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.UPBEAT, "Upbeat", "Move forward.",
            0xFFFF7B8A, 0xFFEE3B57, 0xFFFFD1A8, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.LOVED, "Loved", "You belong.",
            0xFFFF8A5C, 0xFFEF4B3C, 0xFFFFC26E, 6, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.VALUED, "Valued", "You matter.",
            0xFFC98BE0, 0xFF8E3FB0, 0xFF5E1F7A, 4, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.PEACEFUL, "Peaceful", "Breathe easy.",
            0xFFFFF3D6, 0xFFF3E0B4, 0xFFE3C98C, 5, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.GROUNDED, "Grounded", "Root yourself.",
            0xFF9CC47A, 0xFF3E7A46, 0xFF2A5733, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.CALM, "Calm", "Take your time.",
            0xFF9DBBF8, 0xFF4E76E8, 0xFF2F4FB8, 4, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.CONFIDENT, "Confident", "You got this.",
            0xFFFFDE72, 0xFFF5B92B, 0xFFD98F12, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.INSPIRED, "Inspired", "Find your spark.",
            0xFFFF8878, 0xFFE83C3C, 0xFFFFCF9A, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.CURIOUS, "Curious", "Keep exploring.",
            0xFFC4A6F5, 0xFF8B63DE, 0xFF5F3BA8, 6, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.HOPEFUL, "Hopeful", "Brighter days.",
            0xFFFFA48C, 0xFFF2604E, 0xFFFFD0A0, 6, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.REFLECTIVE, "Reflective", "Look within.",
            0xFFFDF6E4, 0xFFEDDCBE, 0xFFCBB48A, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.TENSE, "Tense", "You can unwind.",
            0xFFA8A6F7, 0xFF5F5BE0, 0xFFE8E4FF, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.MOTIVATED, "Motivated", "Small steps.",
            0xFF7FB05E, 0xFF2F6B39, 0xFF1F4B2A, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.CONTENT, "Content", "Right here.",
            0xFFFFA86B, 0xFFF06A38, 0xFFC44A22, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.INSECURE, "Insecure", "It's ok.",
            0xFFB98CE8, 0xFF7C45C4, 0xFFF0E6FF, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.BRAVE, "Brave", "Take the leap.",
            0xFFFF9257, 0xFFEE4426, 0xFFFFD08A, 7, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.GRATEFUL, "Grateful", "Notice more.",
            0xFFFFB3B8, 0xFFF2727F, 0xFFFFD9DC, 8, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.LONELY, "Lonely", "You are not alone.",
            0xFF9CB6F7, 0xFF5B7DE8, 0xFF3E5BB8, 5, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.ANXIOUS, "Anxious", "Still growing.",
            0xFFFFE07A, 0xFFF2B62E, 0xFFCF8A12, 5, FlowerSpec.Shape.POINT),
    )

    /**
     * The spec for a kind, falling back to the first rather than throwing.
     *
     * The fallback is why `FlowersTest` insists every kind has one: a new
     * variant added to the enum and forgotten here would not crash, it would
     * quietly draw daisies for a flower somebody chose on purpose, and nothing
     * on screen would look wrong enough to notice.
     */
    fun spec(kind: FlowerKind?): FlowerSpec =
        LIBRARY.firstOrNull { it.kind == kind } ?: LIBRARY.first()

    /**
     * How many flowers one call grows: one for every minute of it.
     *
     * It used to be one flower per call, which is tidy and made the field
     * almost impossible to fill — a week of good calls put seven dots on a
     * meadow built to hold thousands, and the reward surface read as empty no
     * matter how well the week had gone. A minute is the honest unit anyway:
     * what grows a garden is time spent talking, not the number of times you
     * pressed dial.
     *
     * A call with no duration recorded still counts for one, because it
     * happened. The ceiling matches `Thresholds`' own bound on call length, so
     * a mis-tapped three-hour call cannot flood somebody's patch.
     */
    fun flowerCount(minutes: Int?): Int = (minutes ?: 1).coerceIn(1, 180)

    /**
     * How wide the bloom opens, from how long the call ran.
     *
     * Bounded at both ends on purpose. A two-minute call is still a whole
     * flower — the floor of 0.68 is what stops the garden turning into a
     * ranking of calls by length, which is exactly the scoring the design
     * refuses to do. The square root keeps an hour from dwarfing ten minutes.
     */
    fun bloomScale(minutes: Int?): Double {
        val m = (minutes ?: 8).coerceAtLeast(0)
        val raw = 0.6 + sqrt(m.toDouble()) / 7
        return round(min(1.45, max(0.68, raw)) * 100) / 100
    }
}
