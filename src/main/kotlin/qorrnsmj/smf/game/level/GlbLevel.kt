package qorrnsmj.smf.game.level

import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.component.StaticPhysics

data class GlbLevel(
    val resourcePath: String,
    val terrain: GlbTerrain?,
    val staticMeshes: List<ObjectEntity>,
    val entitySpawns: List<GlbEntity>,
    val areaTriggers: List<GlbTrigger>,
) {
    companion object {
        fun createTemporaryTestLevel(): GlbLevel {
            val terrainTriangles = listOf(
                GlbTerrainTriangle(
                    Vector3f(-10f, 0f, -10f),
                    Vector3f(10f, 0f, -10f),
                    Vector3f(-10f, 0f, 10f),
                ),
                GlbTerrainTriangle(
                    Vector3f(10f, 0f, -10f),
                    Vector3f(10f, 1.8f, 10f),
                    Vector3f(-10f, 0f, 10f),
                ),
            )
            val terrain = GlbTerrain(
                entity = ObjectEntity(model = EntityModels.EMPTY),
                triangles = terrainTriangles,
            )
            val testBlock = ObjectEntity(
                transform = Transform(position = Vector3f(3.5f, 0.5f, 3.5f)),
                model = EntityModels.EMPTY,
                physicsComponent = StaticPhysics(
                    collider = BoxCollider(width = 2f, height = 1f, depth = 2f),
                ),
            )

            return GlbLevel(
                resourcePath = "temporary://glb-level-test",
                terrain = terrain,
                staticMeshes = listOf(testBlock),
                entitySpawns = listOf(
                    GlbEntity(
                        name = "Temp_PlayerSpawn",
                        type = "player",
                        spawnPoint = true,
                        transform = Transform(position = Vector3f(0f, 0.2f, 0f)),
                        properties = emptyMap(),
                    ),
                ),
                areaTriggers = listOf(
                    GlbTrigger(
                        name = "Temp_Trigger",
                        eventName = "temporary_test_trigger",
                        center = Vector3f(2.5f, 0.5f, 2.5f),
                        halfExtents = Vector3f(1.2f, 0.8f, 1.2f),
                        properties = emptyMap(),
                    ),
                ),
            )
        }
    }
}

data class GlbTerrain(
    val entity: ObjectEntity,
    private val triangles: List<GlbTerrainTriangle>,
) : HeightProvider {
    override fun getHeight(worldX: Float, worldZ: Float): Float {
        var bestHeight: Float? = null

        for (triangle in triangles) {
            val height = triangle.getHeightAt(worldX, worldZ) ?: continue
            if (bestHeight == null || height > bestHeight) {
                bestHeight = height
            }
        }

        return bestHeight ?: Float.NEGATIVE_INFINITY
    }
}

data class GlbTerrainTriangle(
    val a: Vector3f,
    val b: Vector3f,
    val c: Vector3f,
) {
    fun getHeightAt(worldX: Float, worldZ: Float): Float? {
        val denominator = (b.z - c.z) * (a.x - c.x) + (c.x - b.x) * (a.z - c.z)
        if (kotlin.math.abs(denominator) <= 0.000001f) return null

        val w1 = ((b.z - c.z) * (worldX - c.x) + (c.x - b.x) * (worldZ - c.z)) / denominator
        val w2 = ((c.z - a.z) * (worldX - c.x) + (a.x - c.x) * (worldZ - c.z)) / denominator
        val w3 = 1f - w1 - w2
        val epsilon = -0.0001f

        if (w1 < epsilon || w2 < epsilon || w3 < epsilon) return null

        return a.y * w1 + b.y * w2 + c.y * w3
    }
}

data class GlbEntity(
    val name: String,
    val type: String,
    val spawnPoint: Boolean,
    val transform: Transform,
    val properties: Map<String, String>,
)

data class GlbTrigger(
    val name: String,
    val eventName: String,
    val center: Vector3f,
    val halfExtents: Vector3f,
    val properties: Map<String, String>,
)

enum class GlbColliderType {
    CONVEX_HULL,
    AABB,
    NONE,
}
