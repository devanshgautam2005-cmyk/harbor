package app.harbor.domain

import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The garden as a place you can be in.
 *
 * Ported from the `fieldtrial.html` prototype, which is the design authority
 * for this screen the way `harvest-pulse` is for the rest of the app. The
 * terrain it stands on is in [Terrain]; this is everything above ground —
 * whose patch is where, what each cell of the field is, and the camera.
 *
 * ## One camera, not two views
 *
 * There is no 2D mode and 3D mode. [tiltFor] blends a flat overhead
 * projection into a perspective one as you zoom in, so the plan view *is* the
 * field seen from far enough away, and everything in between is a real
 * position on that dial. Pulling back is how you get the map; leaning in is
 * how you get the landscape. The readout calls the three states plan,
 * tipping and landscape.
 *
 * That is why this replaced a Field/Top toggle: a toggle asks the user to
 * classify what they want before they can look, and the honest answer is
 * usually "somewhere between".
 *
 * ## Why flowers appear and disappear
 *
 * A planted cell is a coloured dot until it is drawn larger than
 * [FLOWER_AT] pixels, at which point it opens into an actual flower. That is
 * level of detail doing the work of an animation: walking in opens the buds
 * around you because they got big, not because anything is keyframed.
 *
 * Pure arithmetic, no Android, so the projection can be tested exactly.
 */
object Field {

    // --- camera constants, from the prototype -----------------------------

    /** Relative zoom where the overhead view starts tipping into perspective. */
    const val TILT_FROM = 2.1

    /** Relative zoom by which the tip is complete. */
    const val TILT_TO = 4.2

    /** How far in you can go, as a multiple of the overview zoom. */
    const val MAX_REL = 30.0

    private const val EYE = 300.0
    private const val SET_BACK = 2.4
    private const val HORIZON = 0.14
    private const val ELEVATION = 170.0

    /** Drawn radius at which a planted dot becomes a flower. */
    // Lowered from 5.5. A planted cell under this draws as a plain circle,
    // and at the zoom people actually open the garden at, that meant a patch
    // of somebody's flowers was a patch of dots -- the one place in the app
    // where the flowers were promised and not delivered. Below about four
    // pixels a petal is thinner than a pixel and there is genuinely nothing
    // to show, so this is as far down as it is worth going.
    const val FLOWER_AT = 4.2

    /** Cell radius in pixels is this times its size, times the projected scale. */
    const val DOT_SCALE = 3.3

    // --- paint palette ----------------------------------------------------
    //
    // Colours are bucketed so the whole field draws in about a dozen fills
    // rather than one per cell. The index a cell carries is its bucket.

    // Sage rather than grass.
    //
    // These used to be saturated yellow-greens, which made the whole field a
    // wall of colour and left a bloom nothing to be brighter than. The
    // specimen sheet holds colour back everywhere except the flower, and the
    // field is the largest surface in the app to apply that to: the ground is
    // quiet so that a patch of somebody's flowers reads from across the
    // valley.
    val VEG = listOf(0xFFB8C4A4, 0xFFA3B18C, 0xFF8D9C75, 0xFF77875E, 0xFF5F6E4A)
    val WATER = listOf(0xFFC3D3D9, 0xFFA8BEC8)

    /**
     * Sparse ground, drawn faintly.
     *
     * The prototype reaches for `CIRCLE_PAINT.length - 1` here, which is
     * evaluated after the patch colours have been appended — so its bare
     * ground silently takes the last person's petal colour. Harmless in a
     * mock with four fixed people; in Harbor the ground would change colour
     * when a contact is added. This is the constant that was meant.
     */
    const val BARE = 0xFFCBC7C0

    /** Where per-patch colours start in the palette. Two each: deep, then petal. */
    const val PATCH_PAINT_FROM = 8

    private const val BARE_PAINT = 7

    enum class Kind { DOT, CROSS, SQUARE, FLOWER }

    /** One person's planted ground. */
    data class Patch(
        val contactId: UUID?,
        val label: String,
        val calls: Int,
        val x: Double,
        val y: Double,
        val radius: Double,
        val ring: List<Garden.Spot>,
        /** The flower this patch is planted with, which gives it its colour. */
        val flower: FlowerKind,
    )

    /**
     * One cell of the field.
     *
     * There are tens of thousands of these, so it holds numbers and indices
     * rather than objects, and the drawing layer never allocates per cell.
     */
    data class Cell(
        val x: Double,
        val y: Double,
        val z: Double,
        val kind: Kind,
        val size: Double,
        val paint: Int,
        /** Index into the patch list, or -1 for open country. */
        val patch: Int,
        /** Stable per-cell randomness, used for petal spin and colour choice. */
        val tone: Double,
    )

    data class Camera(val x: Double, val y: Double, val zoom: Double)

    /** What the camera works out once per frame, rather than once per cell. */
    data class Lens(
        val tilt: Double,
        val focal: Double,
        val eye: Double,
        val back: Double,
        val camZ: Double,
    )

    /** Somewhere to put a projected point without allocating in the hot loop. */
    class Point {
        @JvmField var x: Double = 0.0
        @JvmField var y: Double = 0.0
        @JvmField var s: Double = 1.0
    }

    /** The zoom at which the whole island just fills the frame. */
    fun overviewZoom(width: Double, height: Double): Double =
        if (width <= 0 || height <= 0) 0.2
        else max(width / Terrain.FIELD_W, height / Terrain.FIELD_H)

    fun clampZoom(zoom: Double, base: Double): Double =
        min(base * MAX_REL, max(base, zoom))

    /** 0 is flat overhead, 1 is full perspective. Everything between is real. */
    fun tiltFor(zoom: Double, base: Double): Double =
        Terrain.smooth((zoom / base - TILT_FROM) / (TILT_TO - TILT_FROM))

    /**
     * The eye for this camera.
     *
     * [Lens.camZ] is sampled under what you are looking at rather than under
     * the viewer, so the framing does not lurch every time the ground beneath
     * you changes height.
     */
    fun buildLens(camera: Camera, base: Double, height: Double): Lens {
        val rel = max(camera.zoom / base, 0.6)
        val eye = max(34.0, EYE * TILT_TO / rel)
        return Lens(
            tilt = tiltFor(camera.zoom, base),
            focal = height * 0.92,
            eye = eye,
            back = eye * SET_BACK,
            camZ = Terrain.heightAt(camera.x, camera.y) * ELEVATION,
        )
    }

    /**
     * World to screen.
     *
     * Projects flat and in perspective, then mixes the two by [Lens.tilt].
     * Mixing the *results* rather than switching between them is what makes
     * the tip continuous — there is no frame where the world jumps.
     */
    fun project(
        wx: Double,
        wy: Double,
        wz: Double,
        camera: Camera,
        lens: Lens,
        width: Double,
        height: Double,
        out: Point,
    ): Point {
        val flatX = width / 2 + (wx - camera.x) * camera.zoom
        val flatY = height * 0.5 + (wy - camera.y) * camera.zoom
        if (lens.tilt <= 0.002) {
            out.x = flatX
            out.y = flatY
            out.s = camera.zoom
            return out
        }
        // Depth. The eye sits at camera.y + back and looks toward decreasing
        // y, so +y runs *toward* the viewer and the far edge of the field is
        // its low-y edge. Easy to get backwards; the floor stops anything at
        // or behind the eye from projecting to infinity.
        val d = max(lens.eye * 0.3, camera.y + lens.back - wy)
        val tx = width / 2 + lens.focal * (wx - camera.x) / d
        val ty = height * HORIZON + lens.focal * (lens.eye + lens.camZ - wz * ELEVATION) / d
        val ts = lens.focal / d
        out.x = flatX + (tx - flatX) * lens.tilt
        out.y = flatY + (ty - flatY) * lens.tilt
        out.s = camera.zoom + (ts - camera.zoom) * lens.tilt
        return out
    }

    // --- patches ----------------------------------------------------------

    /** Where the first few patches are aimed, before [Terrain.settle] adjusts. */
    private val SPOTS = listOf(
        0.40 to 0.66,
        0.68 to 0.74,
        0.55 to 0.42,
        0.82 to 0.55,
    )

    /**
     * A place to aim patch [index] at.
     *
     * The prototype has four people and four hand-placed spots. Harbor does
     * not know how many people there will be, so past the fourth this walks a
     * golden-angle spiral out from the middle — which never repeats and never
     * clusters, and stays deterministic.
     */
    fun spotFor(index: Int): Pair<Double, Double> {
        SPOTS.getOrNull(index)?.let { return it }
        val n = index - SPOTS.size
        val angle = n * 2.399963
        val radius = 0.16 + 0.055 * kotlin.math.sqrt(n + 1.0)
        return (0.5 + kotlin.math.cos(angle) * radius) to
            (0.56 + kotlin.math.sin(angle) * radius * 0.8)
    }

    /**
     * Lay out one patch per person.
     *
     * Radius follows the prototype, then grows with how much has been planted
     * — a patch of one call should not cover the same ground as a patch of
     * twenty. It reaches the prototype's size at six calls, which is where
     * the two agree exactly.
     */
    fun patches(people: List<Person>): List<Patch> = people.mapIndexed { i, person ->
        val (fx, fy) = spotFor(i)
        val at = Terrain.settle(fx * Terrain.FIELD_W, fy * Terrain.FIELD_H)
        val seed = 4000 + i * 37
        val full = 165 + Terrain.hash2(i, 3, seed) * 60
        val growth = 0.72 + 0.28 * min(1.0, person.calls / 6.0)
        val radius = full * growth
        Patch(
            contactId = person.contactId,
            label = person.label,
            calls = person.calls,
            x = at.x,
            y = at.y,
            radius = radius,
            ring = Terrain.blobRing(at.x, at.y, radius, seed),
            flower = person.flower,
        )
    }

    /** A person, as the field needs them. */
    data class Person(
        val contactId: UUID?,
        val label: String,
        val calls: Int,
        /** What this patch is planted with — the kind chosen most often here. */
        val flower: FlowerKind,
    )

    /**
     * Where a garden with nothing in it opens.
     *
     * Not the overview. An empty island seen from above is a map of nothing —
     * it reads as a screen that failed to load. Standing on good ground
     * instead, close enough to see the grass, it reads as somewhere with room
     * in it, which is the honest description of a garden nobody has planted
     * yet.
     *
     * [Terrain.settle] is what keeps this out of the river.
     */
    fun emptyStart(): Garden.Spot =
        Terrain.settle(Terrain.FIELD_W * 0.52, Terrain.FIELD_H * 0.60)

    /** How far in an empty garden stands. Past [TILT_TO], so it is landscape. */
    const val EMPTY_ZOOM = 6.5

    /** Which patch contains a point, or -1. */
    fun patchAt(patches: List<Patch>, px: Double, py: Double): Int {
        for (i in patches.indices) {
            val p = patches[i]
            if (abs(px - p.x) > p.radius * 1.5 || abs(py - p.y) > p.radius * 1.5) continue
            if (Terrain.inRing(p.ring, px, py)) return i
        }
        return -1
    }

    /** The full palette for a given set of patches: shared colours, then two each. */
    fun palette(patches: List<Patch>): LongArray {
        val out = LongArray(PATCH_PAINT_FROM + patches.size * 2)
        VEG.forEachIndexed { i, c -> out[i] = c }
        WATER.forEachIndexed { i, c -> out[VEG.size + i] = c }
        out[BARE_PAINT] = BARE
        patches.forEachIndexed { i, p ->
            val spec = Flowers.spec(p.flower)
            out[PATCH_PAINT_FROM + i * 2] = spec.petalDeep
            out[PATCH_PAINT_FROM + i * 2 + 1] = spec.petal
        }
        return out
    }

    /** How opaque a bucket draws. Sparse ground recedes; planted ground does not. */
    fun alphaFor(paint: Int): Float = when {
        paint == BARE_PAINT -> 0.45f
        paint >= PATCH_PAINT_FROM -> 0.95f
        else -> 0.9f
    }

    // --- the cells --------------------------------------------------------

    /**
     * Build the whole field, once.
     *
     * Tens of thousands of cells, each decided by the terrain under it: water
     * where it is low, rock where it is high and steep, groves where growth
     * clumps, tilled rows where the ground is worked, and flowers wherever
     * somebody's patch covers it. Everything is a pure function of position,
     * so this is rebuilt rather than stored, and always comes out the same.
     */
    fun cells(patches: List<Patch>): List<Cell> {
        val out = ArrayList<Cell>(Terrain.COLS * Terrain.ROWS / 2)
        for (row in 0 until Terrain.ROWS) {
            for (col in 0 until Terrain.COLS) {
                val jx = (Terrain.hash2(col, row, Terrain.SEED + 5) - 0.5) * Terrain.CELL * 0.5
                val jy = (Terrain.hash2(col, row, Terrain.SEED + 6) - 0.5) * Terrain.CELL * 0.5
                val x = col * Terrain.CELL + jx
                val y = row * Terrain.CELL + jy
                if (Terrain.landAt(x, y) < 0.2) continue

                val z = Terrain.heightAt(x, y)
                val m = Terrain.moistureAt(x, y)
                val grove = Terrain.grovesAt(x, y)
                val till = Terrain.tilledAt(x, y)
                val slope = abs(z - Terrain.heightAt(x + Terrain.CELL, y)) +
                    abs(z - Terrain.heightAt(x, y + Terrain.CELL))
                val chance = Terrain.hash2(col, row, Terrain.SEED + 7)
                val patch = patchAt(patches, x, y)

                var kind = Kind.DOT
                var size = 0.1
                var paint = BARE_PAINT

                if (patch >= 0 && z > 0.3) {
                    // Inside somebody's patch the ground is planted: denser,
                    // larger, and in their flower's colour.
                    kind = Kind.FLOWER
                    size = 0.72 + chance * 0.9 + grove * 0.5
                    paint = PATCH_PAINT_FROM + patch * 2 + (if (chance > 0.55) 1 else 0)
                } else if (z < 0.3) {
                    size = 0.5 + (0.3 - z) * 2.4
                    paint = 5 + (if (chance > 0.5) 1 else 0)
                } else if (z < 0.35) {
                    size = 0.3
                    paint = 5
                } else if (z > 0.74 && slope > 0.026) {
                    kind = Kind.SQUARE
                    size = 0.34 + slope * 3
                } else if (grove > 0.5) {
                    size = 0.6 + (grove - 0.5) * 3.4 + m * 0.6
                    paint = min(4.0, floor(z * 3.6 + chance * 1.4)).toInt()
                } else if (m > 0.44) {
                    size = 0.26 + (m - 0.44) * 2.4
                    paint = min(4.0, floor(z * 3.2 + chance)).toInt()
                } else if (till > 0.55 && till < 0.67 && z < 0.7) {
                    kind = Kind.CROSS
                    size = 0.4
                } else if (chance > 0.987 && z > 0.36) {
                    kind = Kind.SQUARE
                    size = 0.3
                } else {
                    size = 0.13 + m * 0.2
                    paint = BARE_PAINT
                }

                out += Cell(x, y, z, kind, min(size, 2.4), paint, patch, chance)
            }
        }
        return out
    }
}
