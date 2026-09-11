package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * The field's projection, checked as arithmetic.
 *
 * Everything here is what a 3D view gets wrong in ways a screenshot hides: a
 * flower that drifts between the two views, a horizon that eats the field, a
 * bud that never opens, a render distance that quietly draws ten thousand
 * things.
 */
class FieldTest {

    private val who = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private fun clusterOf(n: Int, minutes: Int? = 12): Field.Cluster =
        Field.Cluster(
            contactId = who,
            plot = Garden.plotFor(who.toString(), 0),
            flowers = List(n) { FlowerKind.entries[it % FlowerKind.entries.size] to minutes },
        )

    private fun blooms(n: Int) = Field.layout(listOf(clusterOf(n)))

    // --- layout ------------------------------------------------------------

    @Test
    fun `a flower keeps its place when later ones are planted`() {
        val five = blooms(5)
        val twenty = blooms(20)
        repeat(5) { i ->
            assertEquals(five[i].x, twenty[i].x, 1e-9)
            assertEquals(five[i].z, twenty[i].z, 1e-9)
        }
    }

    @Test
    fun `layout is stable across calls`() {
        assertEquals(blooms(9).map { it.x to it.z }, blooms(9).map { it.x to it.z })
    }

    @Test
    fun `two people do not land on the same ground`() {
        val other = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val a = Field.layout(listOf(clusterOf(6)))
        val b = Field.layout(
            listOf(
                Field.Cluster(
                    contactId = other,
                    plot = Garden.plotFor(other.toString(), 1),
                    flowers = List(6) { FlowerKind.entries[0] to 12 },
                ),
            ),
        )
        val overlapping = a.any { p -> b.any { q -> kotlin.math.hypot(p.x - q.x, p.z - q.z) < 8 } }
        assertTrue("clusters should not sit on top of each other", !overlapping)
    }

    // --- render distance ---------------------------------------------------

    @Test
    fun `seeing further is earned by having more to see`() {
        assertTrue(Field.renderDistance(200) > Field.renderDistance(10))
    }

    @Test
    fun `render distance is bounded however big the garden gets`() {
        assertEquals(Field.renderDistance(100_000), Field.renderDistance(50_000), 1e-9)
        assertTrue(Field.renderDistance(100_000) <= 4600.0)
    }

    @Test
    fun `a big field never draws more than the cap`() {
        val many = blooms(4000)
        val cam = Field.openingCamera(many)
        assertTrue(Field.project(many, cam, 1080.0, 2000.0).size <= 260)
    }

    // --- projection --------------------------------------------------------

    @Test
    fun `nearer blooms are drawn later so they cover the far ones`() {
        val field = blooms(60)
        val out = Field.project(field, Field.openingCamera(field), 1080.0, 2000.0)
        for (i in 1 until out.size) {
            assertTrue("painter order broken at $i", out[i].depth <= out[i - 1].depth)
        }
    }

    @Test
    fun `the same distance twice as far is drawn half the size`() {
        val near = Field.Bloom(who, FlowerKind.entries[0], 12, 0.0, 300.0, 1u, 0)
        val far = near.copy(z = 600.0)
        val cam = Field.Camera(x = 0.0, z = 0.0)
        val out = Field.project(listOf(near, far), cam, 1000.0, 2000.0)
        val n = out.first { it.bloom.z == 300.0 }
        val f = out.first { it.bloom.z == 600.0 }
        // Size also carries openness, so compare the raw projection instead.
        assertEquals(2.0, (n.baseY - 2000.0 * Field.HORIZON) / (f.baseY - 2000.0 * Field.HORIZON), 1e-6)
    }

    @Test
    fun `everything sits below the horizon`() {
        val field = blooms(120)
        val height = 2000.0
        Field.project(field, Field.openingCamera(field), 1080.0, height).forEach {
            assertTrue("a bloom floated above the horizon", it.baseY > height * Field.HORIZON)
        }
    }

    @Test
    fun `what is behind you is not drawn`() {
        val ahead = Field.Bloom(who, FlowerKind.entries[0], 12, 0.0, 500.0, 1u, 0)
        val behind = ahead.copy(z = -500.0)
        val out = Field.project(listOf(ahead, behind), Field.Camera(0.0, 0.0), 1000.0, 2000.0)
        assertEquals(1, out.size)
        assertEquals(500.0, out.first().bloom.z, 1e-9)
    }

    @Test
    fun `beyond the render distance is not drawn`() {
        val far = Field.Bloom(who, FlowerKind.entries[0], 12, 0.0, 90_000.0, 1u, 0)
        assertTrue(Field.project(listOf(far), Field.Camera(0.0, 0.0), 1000.0, 2000.0).isEmpty())
    }

    // --- opening -----------------------------------------------------------

    @Test
    fun `distance closes a bloom and approaching opens it`() {
        val width = 1000.0
        val far = Field.renderDistance(50)
        val close = Field.opennessOf(60.0, 0.0, width, far)
        val distant = Field.opennessOf(1400.0, 0.0, width, far)
        assertTrue("close should be open, was $close", close > 0.8)
        assertTrue("distant should be a bud, was $distant", distant < 0.15)
    }

    @Test
    fun `the one you are looking at opens more than its neighbours`() {
        val width = 1000.0
        val far = Field.renderDistance(50)
        val centre = Field.opennessOf(120.0, 0.0, width, far)
        val edge = Field.opennessOf(120.0, 460.0, width, far)
        assertTrue("centre $centre should beat edge $edge", centre > edge)
    }

    @Test
    fun `openness stays inside its range`() {
        val far = Field.renderDistance(80)
        listOf(-9000.0, 0.0, 30.0, 700.0, 5000.0).forEach { d ->
            listOf(-4000.0, 0.0, 4000.0).forEach { o ->
                val v = Field.opennessOf(d, o, 1000.0, far)
                assertTrue("openness $v out of range", v in 0.0..1.0)
            }
        }
    }

    @Test
    fun `nothing pops in at the far edge`() {
        val far = 2000.0
        assertEquals(1.0, Field.fadeOf(10.0, far), 1e-9)
        assertEquals(0.0, Field.fadeOf(far, far), 1e-9)
        assertTrue(Field.fadeOf(far * 0.92, far) in 0.0..1.0)
    }

    @Test
    fun `a longer call opens a fuller bloom, within bounds`() {
        assertTrue(Field.fullnessOf(45) > Field.fullnessOf(5))
        assertEquals(Field.fullnessOf(600), Field.fullnessOf(60), 1e-9)
        assertEquals(1.0, Field.fullnessOf(null), 1e-9)
    }

    // --- focus and touch ---------------------------------------------------

    @Test
    fun `choosing a bloom focuses that one and not a nearer neighbour`() {
        val field = blooms(40)
        val target = field[7]
        val out = Field.project(field, Field.facing(target, field.size), 1080.0, 2000.0)
        val focus = Field.focused(out, chosen = target)
        assertNotNull("the chosen bloom should be in focus", focus)
        assertEquals(target.index, focus!!.bloom.index)
    }

    @Test
    fun `a neighbour really can be nearer, which is why choosing exists`() {
        // Not a wish: golden-angle placement genuinely puts other flowers
        // between you and the one you walked to. Emergent focus picks that
        // neighbour, and this records it rather than pretending otherwise.
        val field = blooms(40)
        val target = field[7]
        val out = Field.project(field, Field.facing(target, field.size), 1080.0, 2000.0)
        val emergent = Field.focused(out)
        assertNotNull(emergent)
        assertTrue(
            "emergent focus should be the most open thing on screen",
            out.none { it.openness > emergent!!.openness },
        )
    }

    @Test
    fun `a chosen bloom that walked out of view falls back to what is open`() {
        val field = blooms(40)
        val offscreen = Field.Bloom(who, FlowerKind.entries[0], 12, 99_000.0, 99_000.0, 1u, 999)
        val out = Field.project(field, Field.openingCamera(field), 1080.0, 2000.0)
        // Should not throw, and should not return the absent bloom.
        val focus = Field.focused(out, chosen = offscreen)
        assertTrue(focus == null || focus.bloom.index != 999)
    }

    @Test
    fun `an empty field focuses nothing`() {
        assertNull(Field.focused(emptyList()))
    }

    @Test
    fun `a tap finds the bloom under it and a miss finds nothing`() {
        val field = blooms(30)
        val out = Field.project(field, Field.openingCamera(field), 1080.0, 2000.0)
        val target = out.last()
        val centreY = target.baseY - target.size * 0.5
        assertEquals(
            target.bloom.index,
            Field.hit(out, target.screenX, centreY)?.bloom?.index,
        )
        assertNull(Field.hit(out, -5000.0, -5000.0))
    }

    // --- camera ------------------------------------------------------------

    @Test
    fun `a bigger garden is viewed from further back and higher up`() {
        val small = Field.openingCamera(blooms(4))
        val large = Field.openingCamera(blooms(300))
        assertTrue(large.height > small.height)
    }

    @Test
    fun `the camera cannot wander out of the world`() {
        val field = blooms(30)
        val lost = Field.Camera(x = 1e9, z = 1e9, height = 1e9)
        val back = Field.clamp(lost, field)
        assertTrue(back.x < 1e8)
        assertTrue(back.z < 1e8)
        assertTrue(back.height <= 420.0)
    }

    @Test
    fun `an empty field still projects and still has a camera`() {
        val none = Field.layout(emptyList())
        assertTrue(Field.project(none, Field.openingCamera(none), 1080.0, 2000.0).isEmpty())
    }

    @Test
    fun `a zero sized viewport draws nothing rather than dividing by it`() {
        val field = blooms(10)
        assertTrue(Field.project(field, Field.openingCamera(field), 0.0, 0.0).isEmpty())
    }
}
