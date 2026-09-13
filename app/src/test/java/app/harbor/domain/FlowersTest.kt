package app.harbor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The library, against the enum it is supposed to cover.
 *
 * [Flowers.spec] falls back to the first entry rather than throwing, which is
 * the right behaviour for a renderer — a missing spec should not crash
 * somebody's garden — but it means a kind added to [FlowerKind] and forgotten
 * in the library fails silently. It would draw daisies for a flower the user
 * picked on purpose, in the picker, in their patch and in the field, and
 * nothing on any of those screens would look broken enough to notice.
 *
 * So the coverage is asserted here instead.
 */
class FlowersTest {

    @Test
    fun `every kind has a spec`() {
        val missing = FlowerKind.entries.filter { kind ->
            Flowers.LIBRARY.none { it.kind == kind }
        }
        assertTrue("no spec in Flowers.LIBRARY for: $missing", missing.isEmpty())
    }

    @Test
    fun `no kind has two specs`() {
        val duplicated = Flowers.LIBRARY
            .groupingBy { it.kind }.eachCount()
            .filterValues { it > 1 }
        assertTrue("more than one spec for: ${duplicated.keys}", duplicated.isEmpty())
    }

    @Test
    fun `the library is the enum, in order`() {
        // The shelf is FlowerKind.entries and the specs are looked up from it,
        // so a library in a different order is not wrong — but it is confusing
        // to read next to the enum, and this keeps the two files in step.
        assertEquals(FlowerKind.entries.toList(), Flowers.LIBRARY.map { it.kind })
    }

    @Test
    fun `every flower is named and means something`() {
        Flowers.LIBRARY.forEach {
            assertTrue("${it.kind} has no name", it.name.isNotBlank())
            assertTrue("${it.kind} has no note", it.note.isNotBlank())
        }
    }

    @Test
    fun `every flower has petals that can be drawn`() {
        // FlowerMark divides a full turn by this, so zero is a divide by zero
        // and one is not a flower. The sheet's blooms run four to eight.
        Flowers.LIBRARY.forEach {
            assertTrue("${it.kind} has ${it.petals} petals", it.petals in 3..12)
        }
    }

    @Test
    fun `every flower is opaque`() {
        // Colours are packed as 0xAARRGGBB longs. A spec written without the
        // alpha byte comes out fully transparent and draws nothing at all,
        // which on a field of thousands of cells is very easy to miss.
        Flowers.LIBRARY.forEach {
            listOf("petal" to it.petal, "petalDeep" to it.petalDeep, "heart" to it.heart)
                .forEach { (which, colour) ->
                    val alpha = (colour shr 24) and 0xFF
                    assertEquals("${it.kind} $which is not opaque", 0xFFL, alpha)
                }
        }
    }

    // --- how a call becomes flowers -----------------------------------------

    @Test
    fun `a minute is a flower`() {
        assertEquals(12, Flowers.flowerCount(12))
    }

    @Test
    fun `a call with no length recorded still counts for one`() {
        assertEquals(1, Flowers.flowerCount(null))
        assertEquals(1, Flowers.flowerCount(0))
    }

    @Test
    fun `a mis-tapped marathon cannot flood a patch`() {
        assertEquals(180, Flowers.flowerCount(9000))
    }

    @Test
    fun `a short call is still a whole bloom`() {
        assertTrue(Flowers.bloomScale(2) >= 0.68)
    }

    @Test
    fun `an hour does not dwarf ten minutes`() {
        assertTrue(Flowers.bloomScale(60) <= 1.45)
        assertTrue(Flowers.bloomScale(60) < Flowers.bloomScale(10) * 2)
    }
}
