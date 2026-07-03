package qorrnsmj.smf.game.level

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.skybox.SkyboxLoader
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.TerrainLoader
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.texture.TextureLoader
import qorrnsmj.smf.graphic.texture.TexturePresets
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Vector3f
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.math.min

object EditorMapLevelLoader {
    private const val SIGNED_HEIGHT_ZERO_SAMPLE = 32768f
    private const val CENTIMETERS_TO_METERS = 0.01f
    private val objectMapper = ObjectMapper()

    @Suppress("UNCHECKED_CAST")
    fun loadInto(scene: Scene, path: String) {
        val root = readJson(path) ?: return

        val terrain = root["terrain"] as? Map<String, Any?>
        if (terrain != null) {
            loadTerrain(scene, terrain)
        }

        val environment = root["environment"] as? Map<String, Any?>
        if (environment != null) {
            loadEnvironment(scene, environment)
        }
    }

    private fun loadTerrain(scene: Scene, terrain: Map<String, Any?>) {
        val heightmap = terrain["heightmap"] as? String
        if (heightmap.isNullOrBlank()) {
            Logger.info("Editor map terrain skipped: heightmap is empty")
            return
        }

        val image = readImage(heightmap)
        if (image == null) {
            Logger.warn("Editor map terrain heightmap not found: {}", heightmap)
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
        Logger.info(
            "Editor map terrain loaded: heightmap={}, size={}, resolution={}",
            heightmap,
            mapSize,
            heightGrid.size,
        )
    }

    private fun loadEnvironment(scene: Scene, environment: Map<String, Any?>) {
        val skyboxPath = environment["skybox"] as? String
        if (!skyboxPath.isNullOrBlank()) {
            try {
                scene.skybox = SkyboxLoader.loadSkyboxResource(toAssetPath(skyboxPath))
                Logger.info("Editor map skybox loaded: {}", skyboxPath)
            } catch (error: Throwable) {
                Logger.warn(error, "Editor map skybox could not be loaded: {}", skyboxPath)
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

    private fun loadSplatmapTexture(terrain: Map<String, Any?>): qorrnsmj.smf.graphic.`object`.TextureBufferObject {
        val splatmap = (terrain["splatmaps"] as? List<*>)?.firstOrNull() as? String
        if (!splatmap.isNullOrBlank()) {
            try {
                return TextureLoader.loadTexture(toAssetPath(splatmap), TexturePresets.TERRAIN)
            } catch (error: Throwable) {
                Logger.warn(error, "Editor map splatmap could not be loaded: {}", splatmap)
            }
        }
        return Textures.TERRAIN_BLEND_MAP
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
            val stream = ClassLoader.getSystemResourceAsStream(path)
            if (stream == null) {
                Logger.warn("Editor map json not found: {}", path)
                null
            } else {
                stream.use { objectMapper.readValue(it, Map::class.java) }
            }
        }
    }

    private fun readImage(path: String): BufferedImage? {
        val filePath = Paths.get(path)
        if (Files.isRegularFile(filePath)) {
            return ImageIO.read(filePath.toFile())
        }

        return ClassLoader.getSystemResourceAsStream(toAssetPath(path))?.use { ImageIO.read(it) }
    }

    private fun toAssetPath(path: String): String {
        val normalized = path.replace('\\', '/')
        return if (normalized.startsWith("assets/")) normalized else "assets/$normalized"
    }
}
