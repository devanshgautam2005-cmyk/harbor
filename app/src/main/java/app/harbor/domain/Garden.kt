package app.harbor.domain

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The garden's geometry.
 *
 * Ported from `lib/harbor/garden.ts`. Every shape is derived from a hash of
 * the person's id, which is the whole trick: a plot keeps the same organic
 * outline and a flower keeps the same spot for as long as it exists, without
 * any of it being stored. Adding a flower never moves the others.
 *
 * Pure arithmetic, no Android, no Compose — so the layout can be tested
 * exactly, rather than eyeballed on a device.
 */
object Garden {

    private const val TILE_W = 178.0
    private const val TILE_H = 104.0

    /** Ground is drawn in plan view and squashed, which is what reads as isometric. */
    const val GROUND_SQUASH = 0.56

    const val MIN_ZOOM = 0.42
    const val MAX_ZOOM = 2.8

    /** Above this every flower is drawn; below it, one per plot. */
    const val DETAIL_ZOOM = 1.12

    /** The sky owns the top of the frame and the hint the bottom. */
    const val SKY_BAND = 92.0
    private const val HINT_BAND = 42.0

    data class Plot(
        val id: String,
        val x: Double,
        val y: Double,
        val radius: Double,
        val seed: UInt,
        /** Painter's order: further back is drawn first. */
        val depth: Int,
    )

    data class Camera(val x: Double, val y: Double, val k: Double)

    data class Bounds(val x: Double, val y: Double, val width: Double, val height: Double)

    data class Spot(val x: Double, val y: Double)

    /** FNV-1a. Matches the prototype's `hashOf` exactly, including overflow. */
    fun hashOf(seed: String): UInt {
        var h = 2166136261u
        for (c in seed) {
            h = h xor c.code.toUInt()
            // Kotlin's Int multiply wraps at 32 bits, which is what Math.imul
            // does in JavaScript. That correspondence is the reason the two
            // implementations agree.
            h = (h.toInt() * 16777619).toUInt()
        }
        return h
    }

    /** Deterministic noise in [0, 1). Matches the prototype's `rand`. */
    fun rand(h: UInt, i: Int): Double {
        val x = sin(h.toDouble() * 0.0001 + i * 12.9898) * 43758.5453
        return x - floor(x)
    }

    /**
     * Square-shell walk: index 0 sits at the middle, later plots ring outward
     * without ever overlapping.
     */
    fun plotCell(i: Int): Pair<Int, Int> {
        val n = floor(sqrt(i.toDouble())).toInt()
        val k = i - n * n
        return if (k < n) n to k else (2 * n - k) to n
    }

    fun plotFor(personId: String, index: Int): Plot {
        val (gx, gy) = plotCell(index)
        val seed = hashOf(personId)
        val jx = (rand(seed, 1) - 0.5) * 26
        val jy = (rand(seed, 2) - 0.5) * 18
        return Plot(
            id = personId,
            x = (gx - gy) * (TILE_W / 2) + jx,
            y = (gx + gy) * (TILE_H / 2) + jy,
            radius = 54 + rand(seed, 3) * 13,
            seed = seed,
            depth = gx + gy,
        )
    }

    /**
     * The outline of a plot, as points around a noisy circle.
     *
     * The caller turns these into a closed Catmull-Rom loop. Returned as
     * points rather than a path so the drawing layer owns its own path type.
     */
    fun blobPoints(seed: UInt, radius: Double, points: Int = 11): List<Spot> =
        (0 until points).map { i ->
            val angle = (i.toDouble() / points) * 2 * Math.PI + (rand(seed, i + 10) - 0.5) * 0.22
            val r = radius * (0.78 + rand(seed, i + 40) * 0.4)
            Spot(cos(angle) * r, sin(angle) * r)
        }

    /**
     * Where a flower sits inside its plot.
     *
     * Golden-angle placement, so the position depends only on the flower's
     * index — planting one never moves the others.
     */
    fun flowerSpot(seed: UInt, index: Int, radius: Double): Spot {
        val r = min(radius * 0.78, radius * 0.76 * sqrt((index + 0.6) / 13.0))
        val angle = index * 2.399963 + rand(seed, index + 70) * 0.55
        return Spot(
            x = cos(angle) * r + (rand(seed, index + 120) - 0.5) * 9,
            y = (sin(angle) * r + (rand(seed, index + 180) - 0.5) * 7) * GROUND_SQUASH,
        )
    }

    fun sceneBounds(plots: List<Plot>): Bounds {
        if (plots.isEmpty()) return Bounds(-160.0, -110.0, 320.0, 220.0)
        val pad = 30.0
        val minX = plots.minOf { it.x - it.radius } - pad
        val maxX = plots.maxOf { it.x + it.radius } + pad
        // Room above for the tallest flower, and below for the name tag.
        val minY = plots.minOf { it.y - it.radius * GROUND_SQUASH - 52 } - pad
        val maxY = plots.maxOf { it.y + it.radius * GROUND_SQUASH + 36 } + pad
        return Bounds(minX, minY, maxX - minX, maxY - minY)
    }

    fun clampZoom(k: Double): Double = min(MAX_ZOOM, max(MIN_ZOOM, k))

    /**
     * The camera that shows the whole garden at once.
     *
     * @param skyBand room reserved at the top. The prototype keeps [SKY_BAND]
     *   clear for its sky wheel; a caller that has not drawn one should pass 0
     *   rather than leave the garden sitting in an empty gap.
     */
    fun fitCamera(
        plots: List<Plot>,
        width: Double,
        height: Double,
        skyBand: Double = SKY_BAND,
    ): Camera {
        if (width <= 0 || height <= 0) return Camera(0.0, 0.0, 1.0)
        val b = sceneBounds(plots)
        val usable = max(130.0, height - skyBand - HINT_BAND)
        val k = clampZoom(min(width / b.width, usable / b.height) * 0.96)
        return Camera(
            x = width / 2 - (b.x + b.width / 2) * k,
            y = skyBand + usable / 2 - (b.y + b.height / 2) * k,
            k = k,
        )
    }

    /** Zoom about a point, so the ground under the finger stays under it. */
    fun zoomAt(camera: Camera, px: Double, py: Double, factor: Double): Camera {
        val k = clampZoom(camera.k * factor)
        val ratio = k / camera.k
        return Camera(
            x = px - (px - camera.x) * ratio,
            y = py - (py - camera.y) * ratio,
            k = k,
        )
    }

    fun focusCamera(plot: Plot, width: Double, height: Double, k: Double = 1.9): Camera =
        Camera(x = width / 2 - plot.x * k, y = height / 2 - plot.y * k, k = k)
}
