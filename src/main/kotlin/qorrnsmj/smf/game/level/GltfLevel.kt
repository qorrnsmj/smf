package qorrnsmj.smf.game.level

import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.component.StaticPhysics

data class GltfLevel(
    val resourcePath: String,
    val terrain: GltfTerrain?,
    val staticMeshes: List<ObjectEntity>,
    val entitySpawns: List<GltfEntity>,
    val areaTriggers: List<GltfTrigger>,
) {
    companion object {
        fun createTemporaryTestLevel(): GltfLevel {
            val terrainTriangles = listOf(
                GltfTerrainTriangle(
                    Vector3f(-1000f, 0f, -1000f),
                    Vector3f(1000f, 0f, -1000f),
                    Vector3f(-1000f, 0f, 1000f),
                ),
                GltfTerrainTriangle(
                    Vector3f(1000f, 0f, -1000f),
                    Vector3f(1000f, 180f, 1000f),
                    Vector3f(-1000f, 0f, 1000f),
                ),
            )
            val terrain = GltfTerrain(
                entity = ObjectEntity(model = EntityModels.EMPTY),
                triangles = terrainTriangles,
            )
            val testBlock = ObjectEntity(
                transform = Transform(position = Vector3f(350f, 50f, 350f)),
                model = EntityModels.EMPTY,
                physicsComponent = StaticPhysics(
                    collider = BoxCollider(width = 200f, height = 100f, depth = 200f),
                ),
            )

            return GltfLevel(
                resourcePath = "temporary://gltf-level-test",
                terrain = terrain,
                staticMeshes = listOf(testBlock),
                entitySpawns = listOf(
                    GltfEntity(
                        name = "Temp_PlayerSpawn",
                        type = "player",
                        spawnPoint = true,
                        transform = Transform(position = Vector3f(0f, 20f, 0f)),
                        properties = emptyMap(),
                    ),
                ),
                areaTriggers = listOf(
                    GltfTrigger(
                        name = "Temp_Trigger",
                        eventName = "temporary_test_trigger",
                        center = Vector3f(250f, 50f, 250f),
                        halfExtents = Vector3f(120f, 80f, 120f),
                        properties = emptyMap(),
                    ),
                ),
            )
        }
    }
}

data class GltfTerrain(
    val entity: ObjectEntity,
    private val triangles: List<GltfTerrainTriangle>,
) : HeightProvider {
    override fun getHeight(worldX: Float, worldZ: Float): Float {
        var bestHeight: Float? = null

        for (triangle in triangles) {
            val height = triangle.getHeightAt(worldX, worldZ) ?: continue
            if (bestHeight == null || height > bestHeight) {
                bestHeight = height
            }
        }

        return bestHeight ?: 0f
    }
}

data class GltfTerrainTriangle(
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

data class GltfEntity(
    val name: String,
    val type: String,
    val spawnPoint: Boolean,
    val transform: Transform,
    val properties: Map<String, String>,
)

data class GltfTrigger(
    val name: String,
    val eventName: String,
    val center: Vector3f,
    val halfExtents: Vector3f,
    val properties: Map<String, String>,
)

enum class GltfColliderType {
    CONVEX_HULL,
    AABB,
    NONE,
}
