package app.harbor.domain

import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The garden seen from inside it.
 *
 * The top-down garden answers "what have I grown". This answers something the
 * plan view cannot: what it is like to be standing in it. A hundred calls read
 * as a hundred dots from above; from ground level they read as a field that
 * runs past the horizon, which is the feeling the thing is actually for.
 *
 * ## Same ground, two cameras
 *
 * This does not invent a second world. Every bloom sits at the coordinates
 * [Garden] already assigns it — the plan view's y axis is this one's z, with
 * the isometric squash undone. So a flower is in the same place in both views,
 * and switching between them is a camera move rather than a different scene.
 * That is also why nothing here needs storing: position still falls out of the
 * hash of a person's id and a flower's index.
 *
 * ## Why there is no 3D engine underneath this
 *
 * Every bloom is a billboard — a flat thing always facing the viewer — so the
 * only 3D that matters is where its base lands and how big it is. That is a
 * divide per bloom, which means it draws on the same canvas as everything else
 * in Harbor, needs no model files, and hands the artwork back as one draw call
 * per flower. When real flower art arrives it slots into that call; nothing
 * here changes.
 *
 * Pure arithmetic, no Android, no Compose, so the projection can be tested
 * exactly rather than eyeballed on a device.
 */
object Field {

    /** Ground units across an open flower. */
    const val BLOOM_SIZE = 30.0

    /** A bud is roughly a third of the flower it becomes. */
    const val BUD_SCALE = 0.34

    /** Nearer than this is behind you, or close enough to be meaningless. */
    const val NEAR = 26.0

    /** Where the ground meets the sky, as a fraction of the viewport. */
    const val HORIZON = 0.40

    /** Eye height above the ground, in the same units as the plan view. */
    const val EYE = 52.0

    private const val FOV = 1.05

    /** Beyond this many on screen the field reads as texture, not flowers. */
    private const val MAX_DRAWN = 260

    /** A bloom, placed on the ground plane. */
    data class Bloom(
        val contactId: UUID?,
        val kind: FlowerKind,
        /** Minutes the call ran, which decides how full the bloom opens. */
        val minutes: Int?,
        val x: Double,
        val z: Double,
        val seed: UInt,
        /** Index within its cluster. Stable for the life of the flower. */
        val index: Int,
    )

    /**
     * Where the viewer is standing.
     *
     * [height] is eye height; raising it tips the view towards the plan the
     * top-down garden shows, which is what makes the two modes feel like one
     * place rather than two screens.
     */
    data class Camera(
        val x: Double,
        val z: Double,
        val height: Double = EYE,
        val fov: Double = FOV,
    )

    /** One bloom, resolved to the screen. */
    data class Projected(
        val bloom: Bloom,
        val screenX: Double,
        /** Where the stem meets the ground. */
        val baseY: Double,
        /** Drawn diameter, already including how far open it is. */
        val size: Double,
        /** Distance ahead of the camera. Bigger is further. */
        val depth: Double,
        /** 0 is a closed bud, 1 is fully open. */
        val openness: Double,
        /** Fades in rather than popping at the render edge. */
        val alpha: Double,
    )

    /** One person's patch, as a cluster centre plus the flowers in it. */
    data class Cluster(
        val contactId: UUID?,
        val plot: Garden.Plot,
        /** Oldest first, so an index never changes once planted. */
        val flowers: List<Pair<FlowerKind, Int?>>,
    )

    /**
     * How far you can see, as a function of how much there is to see.
     *
     * A handful of calls should all be visible at once or the field looks
     * empty and broken. A year of them should fade into the distance instead
     * of costing a thousand draw calls. So the horizon is earned: it opens up
     * as the garden fills, the way a render distance does.
     */
    fun renderDistance(count: Int): Double =
        min(4600.0, 700.0 + count * 46.0)

    /**
     * Where the camera rests for a garden of this size.
     *
     * Few flowers: stand among them, close and low. Many: step back and lift,
     * because the point of a large field is that it is large, and you cannot
     * see that with your face in it.
     */
    fun restingHeight(count: Int): Double =
        min(190.0, EYE + count * 1.5)

    fun restingBack(count: Int): Double =
        min(760.0, 180.0 + count * 7.0)

    /**
     * Lay the blooms out on the ground plane.
     *
     * Reuses [Garden.flowerSpot] so a flower is in the same place here as in
     * the plan view; the squash that makes the plan look isometric is undone
     * to recover the true ground position.
     */
    fun layout(clusters: List<Cluster>): List<Bloom> = clusters.flatMap { cluster ->
        cluster.flowers.mapIndexed { index, (kind, minutes) ->
            val spot = Garden.flowerSpot(cluster.plot.seed, index, cluster.plot.radius)
            Bloom(
                contactId = cluster.contactId,
                kind = kind,
                minutes = minutes,
                x = cluster.plot.x + spot.x,
                // Both terms are plan coordinates, squashed to read as
                // isometric. Undoing that once recovers true ground spacing;
                // undoing it twice, as an earlier version did, stretched every
                // cluster into a long corridor in z.
                z = (cluster.plot.y + spot.y) / Garden.GROUND_SQUASH,
                seed = cluster.plot.seed,
                index = index,
            )
        }
    }

    /** The centre of everything planted, so a camera can be aimed at it. */
    fun centre(blooms: List<Bloom>): Pair<Double, Double> {
        if (blooms.isEmpty()) return 0.0 to 0.0
        return blooms.sumOf { it.x } / blooms.size to blooms.sumOf { it.z } / blooms.size
    }

    /** The camera a garden of this size opens at: behind the field, looking in. */
    fun openingCamera(blooms: List<Bloom>): Camera {
        val (cx, cz) = centre(blooms)
        return Camera(
            x = cx,
            z = cz - restingBack(blooms.size),
            height = restingHeight(blooms.size),
        )
    }

    /**
     * Project the world onto the screen.
     *
     * Returns only what is actually visible, ordered far to near so a painter
     * can draw them in sequence and get occlusion for free. Capped at
     * [MAX_DRAWN]: past that the far ones contribute a pixel each, and the
     * cost is real on the mid-range phones this study runs on.
     */
    fun project(
        blooms: List<Bloom>,
        camera: Camera,
        width: Double,
        height: Double,
    ): List<Projected> {
        if (width <= 0 || height <= 0) return emptyList()

        val focal = (width / 2) / tan(camera.fov / 2)
        val horizon = height * HORIZON
        val far = renderDistance(blooms.size)
        val centreX = width / 2

        val visible = ArrayList<Projected>(min(blooms.size, MAX_DRAWN))

        for (bloom in blooms) {
            val dz = bloom.z - camera.z
            if (dz <= NEAR || dz > far) continue

            val scale = focal / dz
            val screenX = centreX + (bloom.x - camera.x) * scale

            // Generous margin: a bloom whose centre is off screen can still
            // have petals on it.
            if (screenX < -width * 0.4 || screenX > width * 1.4) continue

            val baseY = horizon + camera.height * scale
            val openness = opennessOf(dz, screenX - centreX, width, far)
            val fullness = fullnessOf(bloom.minutes)
            val diameter =
                BLOOM_SIZE * fullness * (BUD_SCALE + (1 - BUD_SCALE) * openness) * scale

            visible += Projected(
                bloom = bloom,
                screenX = screenX,
                baseY = baseY,
                size = diameter,
                depth = dz,
                openness = openness,
                alpha = fadeOf(dz, far),
            )
        }

        visible.sortByDescending { it.depth }
        return if (visible.size <= MAX_DRAWN) visible else visible.takeLast(MAX_DRAWN)
    }

    /**
     * How open a bloom is: near and central opens, far and peripheral stays a
     * bud.
     *
     * Two terms, because distance alone gives you a wall of open flowers as
     * soon as you walk in. The centre term is what makes the thing you are
     * looking at the thing that opens — one flower at a time, the way an
     * attention-following interface behaves, rather than the whole front row
     * blooming at once.
     */
    fun opennessOf(depth: Double, offsetFromCentre: Double, width: Double, far: Double): Double {
        val nearBand = NEAR * 2
        val openBy = min(far * 0.45, nearBand + 620.0)
        val proximity = 1.0 - smoothstep(nearBand, openBy, depth)
        val reach = max(1.0, width * 0.42)
        val central = 1.0 - smoothstep(0.0, reach, abs(offsetFromCentre))
        return (proximity * (0.35 + 0.65 * central)).coerceIn(0.0, 1.0)
    }

    /** A longer call opens a fuller bloom, bounded so a short one is still whole. */
    fun fullnessOf(minutes: Int?): Double {
        if (minutes == null) return 1.0
        return (0.78 + 0.42 * sqrt(min(minutes, 60) / 60.0)).coerceIn(0.78, 1.2)
    }

    /** The last tenth of the render distance fades, so nothing pops into being. */
    fun fadeOf(depth: Double, far: Double): Double =
        (1.0 - smoothstep(far * 0.82, far, depth)).coerceIn(0.0, 1.0)

    /**
     * The bloom the viewer is attending to.
     *
     * Two ways to be that bloom, and the order matters.
     *
     * A [chosen] one wins outright. Openness alone cannot carry this: flowers
     * are placed on a golden angle, so walking up to one regularly leaves a
     * neighbour between you and it, and that neighbour is nearer and opens
     * wider. Inferring focus there meant tapping a flower and watching a
     * different flower open, which reads as the tap having missed.
     *
     * Otherwise it is emergent — whatever is most open, which by construction
     * is near the middle and close to hand. That is the right behaviour while
     * wandering, where nothing has been chosen.
     *
     * Null in an empty field, or when everything is still a distant bud.
     */
    fun focused(projected: List<Projected>, chosen: Bloom? = null): Projected? {
        if (chosen != null) {
            val pick = projected.firstOrNull { same(it.bloom, chosen) }
            if (pick != null) return pick
        }
        return projected.filter { it.openness > 0.45 }.maxByOrNull { it.openness }
    }

    /**
     * Identity that survives a relayout.
     *
     * Position is derived, not stored, so comparing coordinates would break
     * the moment anything upstream rounded differently. A bloom is which
     * person's patch it is in and where in that patch it sits.
     */
    fun same(a: Bloom, b: Bloom): Boolean =
        a.contactId == b.contactId && a.index == b.index

    /** Which bloom a tap landed on, nearest first so the front one wins. */
    fun hit(projected: List<Projected>, x: Double, y: Double): Projected? =
        projected.lastOrNull { p ->
            val radius = max(22.0, p.size * 0.62)
            val centreY = p.baseY - p.size * 0.5
            abs(x - p.screenX) <= radius && abs(y - centreY) <= radius
        }

    /**
     * A camera standing in front of one bloom, close enough to read it.
     *
     * Stops short rather than arriving on top of it: [NEAR] culls anything
     * closer, so walking all the way in would make the flower vanish.
     */
    fun facing(bloom: Bloom, count: Int): Camera = Camera(
        x = bloom.x,
        z = bloom.z - NEAR * 4.2,
        height = min(EYE, restingHeight(count)),
    )

    /** Keeps a walking camera inside the field it is walking in. */
    fun clamp(camera: Camera, blooms: List<Bloom>): Camera {
        if (blooms.isEmpty()) return camera
        val margin = 900.0
        val minX = blooms.minOf { it.x } - margin
        val maxX = blooms.maxOf { it.x } + margin
        val minZ = blooms.minOf { it.z } - renderDistance(blooms.size)
        val maxZ = blooms.maxOf { it.z } + margin
        return camera.copy(
            x = camera.x.coerceIn(minX, maxX),
            z = camera.z.coerceIn(minZ, maxZ),
            height = camera.height.coerceIn(18.0, 420.0),
        )
    }

    /** Hermite ease. 0 below [from], 1 above [to]. */
    fun smoothstep(from: Double, to: Double, at: Double): Double {
        if (to <= from) return if (at >= to) 1.0 else 0.0
        val t = ((at - from) / (to - from)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }
}
