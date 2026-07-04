package qorrnsmj.smf.game.level

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.EntityLoader
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.skybox.SkyboxLoader
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.TerrainLoader
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.texture.TextureLoader
import qorrnsmj.smf.graphic.texture.TexturePresets
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.shape.SphereCollider
import qorrnsmj.smf.physics.component.StaticPhysics
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.min

object LevelDefinitionLoader {
    private const val SIGNED_HEIGHT_ZERO_SAMPLE = 32768f
    private const val CENTIMETERS_TO_METERS = 0.01f
    private val objectMapper = ObjectMapper()

    fun load(levelName: String): LevelDefinition {
        val resourcePath = toResourcePath(levelName)
        val root = readJson(resourcePath) ?: error("Level definition json not found: $resourcePath")

        return LevelDefinition(
            name = root.string("name") ?: levelName.removeSuffix(".json").substringAfterLast('/'),
            resourcePath = resourcePath,
            renderProfile = root.string("renderProfile"),
            entityModels = root.stringList("entityModels"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun loadInto(scene: Scene, definition: LevelDefinition) {
        val root = readJson(definition.resourcePath) ?: return

        (root["terrain"] as? Map<String, Any?>)?.let { loadTerrain(scene, it) }
        (root["environment"] as? Map<String, Any?>)?.let { loadEnvironment(scene, it) }
        loadStaticObjects(scene, root["static_objects"] as? List<Map<String, Any?>> ?: emptyList(), definition.resourcePath)
    }

    fun toResourcePath(levelName: String): String {
        val normalized = levelName.replace('\\', '/')
        return when {
            normalized.endsWith(".json") -> normalized
            normalized.startsWith("assets/level/") -> "$normalized.json"
            else -> "assets/level/$normalized.json"
        }
    }

    private fun loadTerrain(scene: Scene, terrain: Map<String, Any?>) {
        val heightmap = terrain["heightmap"] as? String
        if (heightmap.isNullOrBlank()) {
            Logger.info("Level terrain skipped: heightmap is empty")
            return
        }

        val image = readImage(heightmap)
        if (image == null) {
            Logger.warn("Level terrain heightmap not found: {}", heightmap)
            return
        }

        val mapSize = (terrain["map_size"] as? Number)?.toFloat() ?: 256f
        val heightGrid = readSignedHeightGrid(image)
        val textureMode = BlendedTexture(
            blendMap = loadSplatmapTexture(terrain),
            baseTexture = Textures.TERRAIN_GRASS,
            rTexture = Textures.TERRAIN_DIRT,
            gTexture = Textures.TERRAIN_FLOWER,
            bTexture = Textures.TERRAIN_PATH,
        )
        val loadedTerrain = TerrainLoader.loadHeightGridModel(
            sizeX = mapSize,
            sizeY = mapSize,
            heightGrid = heightGrid,
            position = Vector3f(-mapSize * 0.5f, 0f, -mapSize * 0.5f),
            textureMode = textureMode,
        )

        scene.terrain = loadedTerrain
        scene.terrainHeightProvider = loadedTerrain
        Logger.info("Level terrain loaded: heightmap={}, size={}, resolution={}", heightmap, mapSize, heightGrid.size)
    }

    private fun loadEnvironment(scene: Scene, environment: Map<String, Any?>) {
        val skyboxPath = environment["skybox"] as? String
        if (!skyboxPath.isNullOrBlank()) {
            try {
                scene.skybox = SkyboxLoader.loadSkyboxResource(toAssetPath(skyboxPath))
                Logger.info("Level skybox loaded: {}", skyboxPath)
            } catch (error: Throwable) {
                Logger.warn(error, "Level skybox could not be loaded: {}", skyboxPath)
                scene.skybox = Skyboxes.SKY1
            }
        }

        scene.skyColor = when ((environment["time_of_day"] as? String)?.lowercase()) {
            "morning" -> Vector3f(0.72f, 0.62f, 0.48f)
            "evening" -> Vector3f(0.56f, 0.32f, 0.24f)
            "night" -> Vector3f(0.05f, 0.08f, 0.14f)
            else -> Vector3f(0.55f, 0.72f, 0.98f)
        }
    }

    private fun loadStaticObjects(scene: Scene, objects: List<Map<String, Any?>>, sourcePath: String) {
        for (item in objects) {
            val model = item["model"] as? String ?: continue
            val transform = Transform(
                position = vectorFromJson(item["pos"], Vector3f()),
                rotation = Quaternion.fromEulerDegrees(vectorFromJson(item["rot"], Vector3f())),
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

        Logger.info("Level static objects loaded: {} objects from {}", objects.size, sourcePath)
    }

    private fun loadSplatmapTexture(terrain: Map<String, Any?>): qorrnsmj.smf.graphic.`object`.TextureBufferObject {
        val splatmap = (terrain["splatmaps"] as? List<*>)?.firstOrNull() as? String
        if (!splatmap.isNullOrBlank()) {
            try {
                return TextureLoader.loadTexture(toAssetPath(splatmap), TexturePresets.TERRAIN)
            } catch (error: Throwable) {
                Logger.warn(error, "Level splatmap could not be loaded: {}", splatmap)
            }
        }
        return Textures.TERRAIN_BLEND_MAP
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
            val position = objectTransform.position.add(objectTransform.rotation.rotate(localPosition))
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

    private fun readSignedHeightGrid(image: BufferedImage): Array<FloatArray> {
        val resolution = min(image.width, image.height).coerceAtLeast(2)
        val raster = image.raster
        val sampleBits = raster.sampleModel.getSampleSize(0).coerceAtLeast(1)
        val unsignedMax = if (sampleBits >= 16) 65535f else ((1 shl sampleBits) - 1).toFloat().coerceAtLeast(1f)
        return Array(resolution) { x ->
            FloatArray(resolution) { z ->
                val sourceX = ((x.toFloat() / (resolution - 1)) * (image.width - 1)).toInt().coerceIn(0, image.width - 1)
                val sourceZ = ((z.toFloat() / (resolution - 1)) * (image.height - 1)).toInt().coerceIn(0, image.height - 1)
                val sample = raster.getSample(sourceX, sourceZ, 0).toFloat()
                if (sampleBits >= 16) {
                    (sample - SIGNED_HEIGHT_ZERO_SAMPLE) * CENTIMETERS_TO_METERS
                } else {
                    (sample / unsignedMax) * 16f
                }
            }
        }
    }

    private fun readJson(path: String): Map<*, *>? {
        val filePath = Paths.get(path)
        return if (Files.isRegularFile(filePath)) {
            objectMapper.readValue(filePath.toFile(), Map::class.java)
        } else {
            ClassLoader.getSystemResourceAsStream(path)?.use { objectMapper.readValue(it, Map::class.java) }
        }
    }

    private fun readImage(path: String): BufferedImage? {
        val filePath = Paths.get(path)
        if (Files.isRegularFile(filePath)) {
            return ImageIO.read(filePath.toFile())
        }

        return ClassLoader.getSystemResourceAsStream(toAssetPath(path))?.use { ImageIO.read(it) }
    }

    private fun Vector3f.absComponents(): Vector3f {
        return Vector3f(abs(x), abs(y), abs(z))
    }

    private fun Vector3f.average(): Float {
        return (x + y + z) / 3f
    }

    private fun vectorFromJson(value: Any?, default: Vector3f): Vector3f {
        val list = value as? List<*> ?: return default
        return Vector3f(
            (list.getOrNull(0) as? Number)?.toFloat() ?: default.x,
            (list.getOrNull(1) as? Number)?.toFloat() ?: default.y,
            (list.getOrNull(2) as? Number)?.toFloat() ?: default.z,
        )
    }

    private fun toAssetPath(path: String): String {
        val normalized = path.replace('\\', '/')
        return if (normalized.startsWith("assets/")) normalized else "assets/$normalized"
    }

    private fun Map<*, *>.string(key: String): String? = this[key] as? String

    private fun Map<*, *>.stringList(key: String): List<String> {
        return (this[key] as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
    }
}
