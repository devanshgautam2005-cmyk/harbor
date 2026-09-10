package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The garden's geometry, checked against the prototype's own JavaScript.
 *
 * The expected values below were produced by running the actual functions in
 * `lib/harbor/garden.ts` under node, not by reading the code and reasoning
 * about it. That matters: the whole design depends on a plot keeping its
 * outline and a flower keeping its spot, and both are derived from a hash
 * whose behaviour depends on 32-bit integer overflow. An implementation that
 * is *nearly* right produces a garden that quietly rearranges itself.
 */
class GardenTest {

    // --- the hash, where an off-by-one-bit ruins everything ---------------

    @Test
    fun the_hash_matches_the_prototype() {
        assertEquals(3869146504u, Garden.hashOf("mom"))
        assertEquals(3880724150u, Garden.hashOf("dad"))
    }

    @Test
    fun the_hash_is_stable_across_calls() {
        assertEquals(Garden.hashOf("aanya"), Garden.hashOf("aanya"))
    }

    @Test
    fun different_people_hash_differently() {
        assertTrue(Garden.hashOf("mom") != Garden.hashOf("dad"))
    }

    @Test
    fun the_noise_function_matches_the_prototype() {
        assertEquals(0.2722277706, Garden.rand(Garden.hashOf("mom"), 1), 1e-9)
    }

    // --- plot layout ------------------------------------------------------

    @Test
    fun plots_ring_outward_without_overlapping() {
        // The square-shell walk: 0 in the middle, then rings.
        val expected = listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1, 2 to 0, 2 to 1)
        assertEquals(expected, (0..5).map { Garden.plotCell(it) })
    }

    @Test
    fun a_plot_lands_where_the_prototype_puts_it() {
        val plot = Garden.plotFor("mom", 0)
        assertEquals(-5.922078, plot.x, 1e-6)
        assertEquals(-6.964024, plot.y, 1e-6)
        assertEquals(59.529395, plot.radius, 1e-6)
        assertEquals(0, plot.depth)
    }

    @Test
    fun a_persons_plot_never_moves() {
        // Same person, same index, same everything — this is what makes the
        // garden feel like a place rather than a chart.
        repeat(3) {
            val plot = Garden.plotFor("dad", 2)
            assertEquals(Garden.plotFor("dad", 2), plot)
        }
    }

    @Test
    fun plot_outlines_are_organic_but_never_a_circle() {
        val seed = Garden.hashOf("mom")
        val points = Garden.blobPoints(seed, 60.0)
        assertEquals(11, points.size)

        val radii = points.map { kotlin.math.hypot(it.x, it.y) }
        assertTrue("outline should vary", radii.max() - radii.min() > 1.0)
        // Still recognisably a plot of roughly the requested size.
        assertTrue(radii.all { it in 30.0..90.0 })
    }

    // --- flowers ----------------------------------------------------------

    @Test
    fun a_flower_sits_where_the_prototype_puts_it() {
        val spot = Garden.flowerSpot(Garden.hashOf("mom"), 3, 60.0)
        assertEquals(-0.704266, spot.x, 1e-6)
        assertEquals(13.663787, spot.y, 1e-6)
    }

    @Test
    fun planting_a_flower_never_moves_the_others() {
        // Golden-angle placement depends only on the index, which is the
        // reason a garden can grow without rearranging itself.
        val seed = Garden.hashOf("mom")
        val before = (0 until 5).map { Garden.flowerSpot(seed, it, 60.0) }
        val after = (0 until 6).map { Garden.flowerSpot(seed, it, 60.0) }
        assertEquals(before, after.take(5))
    }

    @Test
    fun flowers_stay_inside_their_plot() {
        val seed = Garden.hashOf("mom")
        val radius = 60.0
        for (i in 0 until 40) {
            val spot = Garden.flowerSpot(seed, i, radius)
            // y is squashed by the isometric projection, so undo it before
            // measuring the distance from the middle of the plot.
            val distance = kotlin.math.hypot(spot.x, spot.y / Garden.GROUND_SQUASH)
            assertTrue("flower $i escaped its plot at $distance", distance < radius * 1.1)
        }
    }

    // --- camera -----------------------------------------------------------

    @Test
    fun the_fit_camera_centres_a_single_plot() {
        val plots = listOf(Garden.plotFor("mom", 0))
        val camera = Garden.fitCamera(plots, 1080.0, 2000.0)
        val bounds = Garden.sceneBounds(plots)

        val centreX = bounds.x + bounds.width / 2
        assertEquals(1080.0 / 2, centreX * camera.k + camera.x, 0.5)
    }

    @Test
    fun zoom_is_clamped_at_both_ends() {
        assertEquals(Garden.MIN_ZOOM, Garden.clampZoom(0.01), 1e-9)
        assertEquals(Garden.MAX_ZOOM, Garden.clampZoom(99.0), 1e-9)
    }

    @Test
    fun zooming_keeps_the_ground_under_the_finger() {
        val camera = Garden.Camera(x = 40.0, y = 90.0, k = 1.0)
        val zoomed = Garden.zoomAt(camera, px = 300.0, py = 500.0, factor = 1.6)

        // The world point under (300, 500) before and after must be the same.
        val beforeX = (300.0 - camera.x) / camera.k
        val afterX = (300.0 - zoomed.x) / zoomed.k
        assertEquals(beforeX, afterX, 1e-9)

        val beforeY = (500.0 - camera.y) / camera.k
        val afterY = (500.0 - zoomed.y) / zoomed.k
        assertEquals(beforeY, afterY, 1e-9)
    }

    @Test
    fun an_empty_garden_still_has_a_frame_to_show() {
        val bounds = Garden.sceneBounds(emptyList())
        assertTrue(bounds.width > 0 && bounds.height > 0)
        val camera = Garden.fitCamera(emptyList(), 1080.0, 2000.0)
        assertTrue(camera.k >= Garden.MIN_ZOOM)
    }

    @Test
    fun a_zero_sized_screen_does_not_produce_a_broken_camera() {
        val camera = Garden.fitCamera(listOf(Garden.plotFor("mom", 0)), 0.0, 0.0)
        assertEquals(1.0, camera.k, 1e-9)
    }
}
