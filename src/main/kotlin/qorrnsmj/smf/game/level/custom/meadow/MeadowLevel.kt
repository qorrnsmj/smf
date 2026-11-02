package qorrnsmj.smf.game.level.custom.meadow

import org.lwjgl.glfw.GLFW.GLFW_KEY_G
import org.lwjgl.glfw.GLFW.GLFW_KEY_T
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import qorrnsmj.smf.SMF
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.custom.Level
import qorrnsmj.smf.graphic.scene.grass.FoliageType
import qorrnsmj.smf.graphic.scene.grass.GrassPatch
import qorrnsmj.smf.graphic.scene.light.DirectionalLight
import qorrnsmj.smf.graphic.scene.sky.Skydome
import qorrnsmj.smf.graphic.scene.terrain.MeadowTerrainGenerator
import qorrnsmj.smf.graphic.scene.terrain.Terrain
import qorrnsmj.smf.graphic.resource.texture.TextureLoader
import qorrnsmj.smf.graphic.resource.texture.TexturePresets
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld
import java.io.File

internal abstract class MeadowLevel(
    protected val style: MeadowStyle,
    private val terrainProvider: () -> Terrain,
) : Level() {
    protected lateinit var meadowTerrain: Terrain
        private set
    protected lateinit var grass: GrassPatch
        private set
    protected lateinit var flowers: GrassPatch
        private set

    private var windKeyWasDown = false
    private var timeKeyWasDown = false
    private var timePaused = false

    final override fun load() {
        val branch = currentBranchName() ?: "SMF"
        glfwSetWindowTitle(SMF.window.id, "$branch | ${style.displayName}")

        meadowTerrain = terrainProvider()
        scene.world.terrain = meadowTerrain
        scene.world.terrainHeightProvider = meadowTerrain
        player = Player()
        val spawnHeight = meadowTerrain.getHeight(0f, 0f)
        player.setFeetPosition(Vector3f(0f, spawnHeight, 0f))
        player.camera.setFront(Vector3f(0.18f, -0.08f, -1f))
        scene.world.entities += player
        scene.world.camera = player.camera

        val sunDirection = style.sunDirection.normalize()
        scene.environment.celestialLight = DirectionalLight(
            direction = if (style.night) sunDirection else sunDirection.negate(),
            color = style.lightColor,
            intensity = style.lightIntensity,
            ambientColor = style.ambientColor,
            ambientIntensity = style.ambientIntensity,
            shadowStrength = style.shadowStrength,
        )
        scene.environment.sky.color = style.horizonColor
        skySystem.sky = MeadowSkyResources.createSkydome(style)
        scene.environment.fog.apply {
            enabled = true
            color = style.fogColor
            distanceDensity = style.fogDensity
            distanceGradient = 1.35f
            heightDensity = 0.08f
            bottomY = -2f
            topY = 18f
            heightFalloff = 2.6f
            heightDistanceStart = 28f
            heightDistanceEnd = 100f
        }
        scene.sceneEffects.postEffects += style.createEffects()

        grass = GrassPatch.scatter(
            terrain = meadowTerrain,
            origin = Vector2f(meadowTerrain.position.x, meadowTerrain.position.z),
            size = Vector2f(MeadowTerrainGenerator.SIZE, MeadowTerrainGenerator.SIZE),
            spacing = 0.51f,
            seed = style.grassSeed,
        ).apply {
            windDirection = style.windDirection
            windStrength = GrassPatch.DEFAULT_WIND_STRENGTH
            windSpeed = style.windSpeed
            drawDistance = 72f
        }
        flowers = GrassPatch.scatter(
            terrain = meadowTerrain,
            origin = Vector2f(meadowTerrain.position.x, meadowTerrain.position.z),
            size = Vector2f(MeadowTerrainGenerator.SIZE, MeadowTerrainGenerator.SIZE),
            spacing = 3.4f,
            seed = style.grassSeed + 503,
            foliageType = FoliageType.FLOWERS,
            scaleRange = 0.72f..0.92f,
        ).apply {
            windDirection = style.windDirection
            windStrength = GrassPatch.DEFAULT_WIND_STRENGTH
            windSpeed = style.windSpeed
            drawDistance = 68f
        }
        scene.world.grassPatches += listOf(grass, flowers)

        populateStage()
        windKeyWasDown = glfwGetKey(SMF.window.id, GLFW_KEY_G) == GLFW_PRESS
        timeKeyWasDown = glfwGetKey(SMF.window.id, GLFW_KEY_T) == GLFW_PRESS
    }

    final override fun input(delta: Float) {
        player.handleInput(SMF.window, delta)

        val windDown = glfwGetKey(SMF.window.id, GLFW_KEY_G) == GLFW_PRESS
        if (windDown && !windKeyWasDown) {
            val strength = if (grass.windStrength > 0f) 0f else GrassPatch.DEFAULT_WIND_STRENGTH
            grass.windStrength = strength
            flowers.windStrength = strength
        }
        windKeyWasDown = windDown

        val timeDown = glfwGetKey(SMF.window.id, GLFW_KEY_T) == GLFW_PRESS
        if (timeDown && !timeKeyWasDown) {
            timePaused = !timePaused
            skySystem.paused = timePaused
        }
        timeKeyWasDown = timeDown
        inputStage(delta)
    }

    final override fun update(delta: Float) {
        PhysicsWorld.update(scene.world.entities, scene.world.terrainHeightProvider, delta)
        player.update()
        if (!timePaused) {
            val deltaSeconds = delta / FixedTimestepGame.TARGET_UPS
            grass.update(deltaSeconds)
            flowers.update(deltaSeconds)
            updateStage(deltaSeconds)
        }
    }

    final override fun unload() {
        unloadStage()
        scene.world.grassPatches.clear()
        scene.sceneEffects.postEffects.clear()
        super.unload()
    }

    protected open fun populateStage() = Unit

    protected open fun inputStage(delta: Float) = Unit

    protected open fun updateStage(deltaSeconds: Float) = Unit

    protected open fun unloadStage() = Unit

    private fun currentBranchName(): String? = runCatching {
        val projectDirectory = File(System.getProperty("user.dir"))
        val dotGit = projectDirectory.resolve(".git")
        val gitDirectory = when {
            dotGit.isDirectory -> dotGit
            dotGit.isFile -> {
                val path = dotGit.readText().trim().removePrefix("gitdir:").trim()
                File(path).let { if (it.isAbsolute) it else projectDirectory.resolve(it) }
            }
            else -> return null
        }
        val head = gitDirectory.resolve("HEAD").readText().trim()
        head.removePrefix("ref: refs/heads/").takeIf { head.startsWith("ref: refs/heads/") }
    }.getOrNull()
}

private object MeadowSkyResources {
    private val cloudBase by lazy {
        TextureLoader.loadTexture("assets/texture/sky/cloud_base.png", TexturePresets.TERRAIN)
    }
    private val cloudDetail by lazy {
        TextureLoader.loadTexture("assets/texture/sky/cloud_detail.png", TexturePresets.TERRAIN)
    }
    private val sun by lazy {
        TextureLoader.loadTexture("assets/texture/sky/sun.png", TexturePresets.SKYBOX)
    }
    private val moon by lazy {
        TextureLoader.loadTexture("assets/texture/sky/moon.png", TexturePresets.SKYBOX)
    }

    fun createSkydome(style: MeadowStyle) = Skydome(
        cloudBaseTexture = cloudBase,
        cloudDetailTexture = cloudDetail,
        sunTexture = sun,
        moonTexture = moon,
        sunDirection = style.sunDirection,
        sunColor = if (style.night) Vector3f(0.25f, 0.22f, 0.34f) else Vector3f(1f, 0.90f, 0.68f),
        sunAngularSize = 0.13f,
        sunHorizonGlowStrength = if (style.night) 0.10f else 0.48f,
        moonColor = Vector3f(0.78f, 0.88f, 1f),
        moonAngularSize = 0.09f,
        animationSpeed = if (style.night) 0.006f else 0.010f,
        cloudScale = 1.72f,
        cloudCoverage = if (style.night) 0.43f else 0.36f,
        cloudSoftness = 0.27f,
        horizonColor = style.horizonColor,
        zenithColor = style.zenithColor,
        cloudColor = style.cloudColor,
    )
}
