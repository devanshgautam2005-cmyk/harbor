package app.harbor.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * The flower library, and how a call becomes one.
 *
 * The colours and petal counts are measured off the Harbor specimen sheet,
 * which is the authority on them now. They used to come from the prototype's
 * `lib/harbor/model.ts` and were considerably more muted, with nine to
 * fourteen petals each — which drew a dense little rosette rather than a
 * flower. The sheet's blooms are luminous and carry four to eight broad
 * petals, and at that count the gradient and the overlap of one petal on the
 * next are actually visible, which is where the whole look lives.
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
        FlowerSpec(FlowerKind.DAISY, "Daisy", "An ordinary, easy call.",
            0xFFFEF2DC, 0xFFF3DCA8, 0xFFFEBD3A, 6, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.MARIGOLD, "Marigold", "Warm, a little loud, full of news.",
            0xFFFECA4D, 0xFFE3922B, 0xFF8B5A1C, 7, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.COSMOS, "Cosmos", "Light and drifting. No agenda.",
            0xFFFEC9BC, 0xFFFD8E8C, 0xFFFEBD3A, 6, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.POPPY, "Poppy", "Something honest got said.",
            0xFFFE8A6B, 0xFFDE4B3C, 0xFF3B2A22, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.TULIP, "Tulip", "Short, and enough.",
            0xFFE182AD, 0xFF8E3B96, 0xFF5F1687, 4, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.BLUEBELL, "Bluebell", "Quiet. Mostly listening.",
            0xFFA8C0FB, 0xFF5B86F5, 0xFF3F63C4, 5, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.ASTER, "Aster", "Tangled, then untangled.",
            0xFFC9A9F5, 0xFF9366DE, 0xFFFEBD3A, 7, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.SUNFLOWER, "Sunflower", "The long, good kind.",
            0xFFFEDC7A, 0xFFE8A81F, 0xFF6B4A22, 8, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.LAVENDER, "Lavender", "Calm, and it lasted.",
            0xFFD6CBF2, 0xFF8E7BC8, 0xFF574A86, 6, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.ZINNIA, "Zinnia", "Bright, and a bit daft.",
            0xFFFFA9B8, 0xFFE2506F, 0xFFFEBD3A, 8, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.CAMELLIA, "Camellia", "Careful, and worth it.",
            0xFFFFD9DE, 0xFFE99AA9, 0xFFC4566C, 7, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.PERIWINKLE, "Periwinkle", "Easy. Nothing needed saying.",
            0xFFBFE3F0, 0xFF63AFD4, 0xFF2F6E92, 5, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.BUTTERCUP, "Buttercup", "Small, and it cheered you up.",
            0xFFFFEBA0, 0xFFF2C441, 0xFFB9862A, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.ANEMONE, "Anemone", "A lot at once, and it held.",
            0xFFE8C6E8, 0xFFA65CA8, 0xFF3B2440, 6, FlowerSpec.Shape.POINT),
    )

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
