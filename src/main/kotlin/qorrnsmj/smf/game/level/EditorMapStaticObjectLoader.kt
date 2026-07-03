package qorrnsmj.smf.game.level

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.EntityLoader
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.shape.SphereCollider
import qorrnsmj.smf.physics.component.StaticPhysics
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object EditorMapStaticObjectLoader {
    private val objectMapper = ObjectMapper()

    @Suppress("UNCHECKED_CAST")
    fun loadInto(scene: Scene, path: String) {
        val root = readJson(path) ?: return
        val objects = root["static_objects"] as? List<Map<String, Any?>> ?: emptyList()

        for (item in objects) {
            val model = item["model"] as? String ?: continue
            val transform = Transform(
                position = vectorFromJson(item["pos"], Vector3f()),
                rotation = vectorFromJson(item["rot"], Vector3f()),
                scale = vectorFromJson(item["scale"], Vector3f(1f, 1f, 1f)),
            )
            val entity = ObjectEntity(transform = transform, model = EntityModels.EMPTY)
            val models = loadModels(model)

            for ((name, meshModel) in models) {
                if (name.startsWith("collision_", ignoreCase = true)) continue
                entity.addChild(ObjectEntity(transform = Transform(), model = meshModel))
            }

            scene.entities.add(entity)
            addCollisionEntities(scene, transform, item["collisions"])
        }

        Logger.info("Editor map static objects loaded: {} objects from {}", objects.size, path)
    }

    private fun readJson(path: String): Map<*, *>? {
        val filePath = Paths.get(path)
        return if (Files.isRegularFile(filePath)) {
            objectMapper.readValue(filePath.toFile(), Map::class.java)
        } else {
            val stream = ClassLoader.getSystemResourceAsStream(path)
            if (stream == null) {
                Logger.warn("Editor map json not found: {}", path)
                null
            } else {
                stream.use { objectMapper.readValue(it, Map::class.java) }
            }
        }
    }

    private fun loadModels(modelPath: String): Map<String, qorrnsmj.smf.graphic.`object`.Model> {
        val path = Paths.get(modelPath)
        return if (Files.isRegularFile(path)) {
            EntityLoader.loadModelFromFile(path)
        } else {
            val resourcePath = when {
                modelPath.startsWith("assets/") -> modelPath
                modelPath.startsWith("model/") -> "assets/$modelPath"
                else -> "assets/model/$modelPath"
            }
            EntityLoader.loadModelFromResource(resourcePath)
        }
    }

    private fun addCollisionEntities(scene: Scene, objectTransform: Transform, value: Any?) {
        val collisions = value as? List<*> ?: return
        val objectScale = objectTransform.scale.absComponents()
        for (item in collisions) {
            val collision = item as? Map<*, *> ?: continue
            val localPosition = vectorFromJson(collision["pos"], Vector3f()).multiply(objectScale)
            val position = objectTransform.position.add(rotateEuler(localPosition, objectTransform.rotation))
            val shape = collision["shape"] as? String
            val collider = when {
                shape.equals("sphere", ignoreCase = true) -> {
                    val radius = ((collision["radius"] as? Number)?.toFloat() ?: 1f) * objectScale.average()
                    SphereCollider(radius)
                }

                else -> {
                    val size = vectorFromJson(collision["size"], Vector3f(1f, 1f, 1f)).multiply(objectScale)
                    BoxCollider(size.x, size.y, size.z)
                }
            }

            scene.entities.add(
                ObjectEntity(
                    transform = Transform(position = position),
                    model = EntityModels.EMPTY,
                    physicsComponent = StaticPhysics(collider),
                )
            )
        }
    }

    private fun Vector3f.absComponents(): Vector3f {
        return Vector3f(abs(x), abs(y), abs(z))
    }

    private fun Vector3f.average(): Float {
        return (x + y + z) / 3f
    }

    private fun rotateEuler(value: Vector3f, rotationDegrees: Vector3f): Vector3f {
        val rx = Math.toRadians(rotationDegrees.x.toDouble()).toFloat()
        val ry = Math.toRadians(rotationDegrees.y.toDouble()).toFloat()
        val rz = Math.toRadians(rotationDegrees.z.toDouble()).toFloat()

        val cx = cos(rx)
        val sx = sin(rx)
        val cy = cos(ry)
        val sy = sin(ry)
        val cz = cos(rz)
        val sz = sin(rz)

        val y1 = value.y * cx - value.z * sx
        val z1 = value.y * sx + value.z * cx
        val x2 = value.x * cy + z1 * sy
        val z2 = -value.x * sy + z1 * cy
        val x3 = x2 * cz - y1 * sz
        val y3 = x2 * sz + y1 * cz
        return Vector3f(x3, y3, z2)
    }

    private fun vectorFromJson(value: Any?, default: Vector3f): Vector3f {
        val list = value as? List<*> ?: return default
        return Vector3f(
            (list.getOrNull(0) as? Number)?.toFloat() ?: default.x,
            (list.getOrNull(1) as? Number)?.toFloat() ?: default.y,
            (list.getOrNull(2) as? Number)?.toFloat() ?: default.z,
        )
    }
}
