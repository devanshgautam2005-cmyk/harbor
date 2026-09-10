package app.harbor.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * The flower library, and how a call becomes one.
 *
 * Ported from `lib/harbor/model.ts` in the prototype, which is the authority
 * on these values. The colours and petal counts are design decisions, not
 * arbitrary constants — keep them in step with that file.
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
            0xFFFBFCF6, 0xFFE7EBDA, 0xFFF0BD3E, 9, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.MARIGOLD, "Marigold", "Warm, a little loud, full of news.",
            0xFFF5B14A, 0xFFDE8F2E, 0xFF8B5A1C, 11, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.COSMOS, "Cosmos", "Light and drifting. No agenda.",
            0xFFF1C3D4, 0xFFDB9BB4, 0xFFF0BD3E, 7, FlowerSpec.Shape.ROUND),
        FlowerSpec(FlowerKind.POPPY, "Poppy", "Something honest got said.",
            0xFFE2705A, 0xFFC4523E, 0xFF3B2A22, 5, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.TULIP, "Tulip", "Short, and enough.",
            0xFFE0879F, 0xFFC4667F, 0xFFC4667F, 3, FlowerSpec.Shape.CUP),
        FlowerSpec(FlowerKind.BLUEBELL, "Bluebell", "Quiet. Mostly listening.",
            0xFF8FA6D6, 0xFF6D85BC, 0xFF5A6FA5, 5, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.ASTER, "Aster", "Tangled, then untangled.",
            0xFFB79CD8, 0xFF9A7CC0, 0xFFF0BD3E, 13, FlowerSpec.Shape.POINT),
        FlowerSpec(FlowerKind.SUNFLOWER, "Sunflower", "The long, good kind.",
            0xFFF0C93E, 0xFFD6A81F, 0xFF6B4A22, 14, FlowerSpec.Shape.POINT),
    )

    fun spec(kind: FlowerKind?): FlowerSpec =
        LIBRARY.firstOrNull { it.kind == kind } ?: LIBRARY.first()

    /**
     * What to offer after a call: the flower that matches how it felt, then a
     * few others.
     *
     * The feeling's own flower comes first because it is the honest default,
     * but the rest are there so nobody feels sorted into a box by a question
     * they answered in two seconds.
     */
    fun suggestions(feeling: Feeling, count: Int = 4): List<FlowerKind> {
        val primary = feeling.flower
        return (listOf(primary) + LIBRARY.map { it.kind }.filter { it != primary })
            .take(count)
    }

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
