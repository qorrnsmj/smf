package qorrnsmj.smf.graphic.scene.terrain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MeadowTerrainGeneratorTest {
    @Test
    fun generatedMeadowIsRepeatableAndCentered() {
        val first = MeadowTerrainGenerator.generateHeightGrid(33, 42)
        val repeated = MeadowTerrainGenerator.generateHeightGrid(33, 42)
        first.zip(repeated).forEach { (a, b) -> assertContentEquals(a, b) }
        assertEquals(0f, first[16][16], 0.00001f)
        assertNotEquals(first[0][0], MeadowTerrainGenerator.generateHeightGrid(33, 43)[0][0])
    }

    @Test
    fun generatedMeadowHasVisibleRollingVariationWithoutSharpSteps() {
        val grid = MeadowTerrainGenerator.generateHeightGrid(seed = 2_048)
        val values = grid.flatMap { it.asIterable() }
        assertTrue(values.max() - values.min() in 2.5f..8f)

        var largestNeighborStep = 0f
        for (x in grid.indices) {
            for (z in grid[x].indices) {
                if (x > 0) largestNeighborStep = maxOf(largestNeighborStep, abs(grid[x][z] - grid[x - 1][z]))
                if (z > 0) largestNeighborStep = maxOf(largestNeighborStep, abs(grid[x][z] - grid[x][z - 1]))
            }
        }
        assertTrue(largestNeighborStep < 0.45f)
    }
}
