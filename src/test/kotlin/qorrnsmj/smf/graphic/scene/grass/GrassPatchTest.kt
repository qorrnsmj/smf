package qorrnsmj.smf.graphic.scene.grass
import qorrnsmj.smf.graphic.scene.terrain.Terrain

import qorrnsmj.smf.graphic.scene.terrain.HeightProvider
import qorrnsmj.smf.math.Vector2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GrassPatchTest {
    private val slope = object : HeightProvider {
        override fun getHeight(worldX: Float, worldZ: Float) = worldX * 0.2f + worldZ * 0.3f
    }

    @Test
    fun scatterStaysInBoundsAndFollowsTerrain() {
        val patch = GrassPatch.scatter(slope, Vector2f(-3f, 2f), Vector2f(4.2f, 2.1f), 1f)
        assertEquals(15, patch.instances.size)
        for (instance in patch.instances) {
            val p = instance.position
            assertTrue(p.x >= -3f && p.x < 1.2f)
            assertTrue(p.z >= 2f && p.z < 4.1f)
            assertEquals(slope.getHeight(p.x, p.z) - 0.02f, p.y, 0.00001f)
            assertTrue(instance.scale in 0.45f..0.8f)
        }
        assertTrue(patch.instances.map { it.phase }.distinct().size > 1)
    }

    @Test
    fun seededScatterIsRepeatable() {
        fun scatter(seed: Int) = GrassPatch.scatter(slope, Vector2f(), Vector2f(2f, 2f), seed = seed)
        val first = scatter(42)
        val repeated = scatter(42)
        first.instances.zip(repeated.instances).forEach { (a, b) ->
            assertEquals(a.position.x, b.position.x)
            assertEquals(a.position.y, b.position.y)
            assertEquals(a.position.z, b.position.z)
            assertEquals(a.phase, b.phase)
            assertEquals(a.rotation, b.rotation)
        }
        assertTrue(first.instances.first().position.x != scatter(43).instances.first().position.x)
    }

    @Test
    fun rejectsInvalidOrExcessiveScatterAndUpdatesInSeconds() {
        for (spacing in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, 0.00001f)) {
            assertFailsWith<IllegalArgumentException> {
                GrassPatch.scatter(slope, Vector2f(), Vector2f(10f, 10f), spacing)
            }
        }
        val patch = GrassPatch(emptyList())
        patch.update(0.25f)
        patch.update(0.75f)
        assertEquals(1f, patch.timeSeconds)
        assertFailsWith<IllegalArgumentException> { patch.update(-1f) }
    }

    @Test
    fun scattersLowPolyFlowersWithTheirOwnScale() {
        val flowers = GrassPatch.scatter(
            terrain = slope,
            origin = Vector2f(),
            size = Vector2f(8f, 8f),
            spacing = 4f,
            seed = 8,
            foliageType = FoliageType.FLOWERS,
            scaleRange = 0.7f..0.9f,
        )
        assertEquals(FoliageType.FLOWERS, flowers.foliageType)
        assertEquals(4, flowers.instances.size)
        assertTrue(flowers.instances.all { it.scale in 0.7f..0.9f })
    }
}
