package qorrnsmj.smf.game.level.custom.test

import qorrnsmj.smf.game.level.custom.BaseLevel

import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.GLFW_KEY_G
import org.lwjgl.glfw.GLFW.GLFW_KEY_T
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import qorrnsmj.smf.SMF
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.game.entity.billboard.CloudBillboard
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.LightEntity
import qorrnsmj.smf.game.entity.custom.test.ShadowTestBlockEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.game.weather.WeatherCycle
import qorrnsmj.smf.game.weather.WeatherPresets
import qorrnsmj.smf.debug.DebugBox
import qorrnsmj.smf.debug.DebugCapsule
import qorrnsmj.smf.debug.DebugLine
import qorrnsmj.smf.debug.DebugSphere
import qorrnsmj.smf.debug.DebugVisual
import qorrnsmj.smf.graphic.scene.light.PointLight
import qorrnsmj.smf.graphic.scene.grass.GrassPatch
import qorrnsmj.smf.graphic.scene.sky.Skydome
import qorrnsmj.smf.graphic.resource.texture.TextureLoader
import qorrnsmj.smf.graphic.resource.texture.TexturePresets
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class TestLevel : BaseLevel("test") {
    private lateinit var weatherCycle: WeatherCycle
    private val cloudBillboards = mutableListOf<CloudBillboard>()
    private lateinit var grass: GrassPatch
    private var windKeyWasDown = false
    private var timeKeyWasDown = false
    private var timePaused = false

    override fun load() {
        val branch = currentBranchName() ?: "SMF"
        glfwSetWindowTitle(SMF.window.id, "$branch | Test Level load #${LOAD_COUNT.incrementAndGet()}")
        super.load()
        scene.world.lights.removeAll { it is PointLight }
        addShadowVerificationFixture()
        addDebugVisualVerificationFixture()

        val cloudBaseTexture = TextureLoader.loadTexture(
            "assets/texture/sky/cloud_base.png",
            TexturePresets.TERRAIN,
        )
        val cloudDetailTexture = TextureLoader.loadTexture(
            "assets/texture/sky/cloud_detail.png",
            TexturePresets.TERRAIN,
        )
        val sunTexture = TextureLoader.loadTexture(
            "assets/texture/sky/sun.png",
            TexturePresets.SKYBOX,
        )
        val moonTexture = TextureLoader.loadTexture(
            "assets/texture/sky/moon.png",
            TexturePresets.SKYBOX,
        )
        skySystem.sky = Skydome(
            cloudBaseTexture = cloudBaseTexture,
            cloudDetailTexture = cloudDetailTexture,
            sunTexture = sunTexture,
            moonTexture = moonTexture,
            horizonColor = scene.environment.sky.color,
            zenithColor = Vector3f(0.12f, 0.35f, 0.78f),
            cloudCoverage = 0.38f,
            cloudSoftness = 0.28f,
            cloudScale = 1.65f,
        )

        weatherCycle = WeatherCycle(
            environment = scene.environment,
            presets = listOf(
                WeatherPresets.MORNING,
                WeatherPresets.NOON,
                WeatherPresets.EVENING,
                WeatherPresets.NIGHT,
            ),
            phaseDurationSeconds = 6f,
        )

        cloudBillboards.clear()
        cloudBillboards.addAll(
            listOf(
                CloudBillboard(
                    position = Vector3f(18f, 52f, -18f),
                    size = Vector2f(58f, 20f),
                    texture = cloudBaseTexture,
                    tint = Vector4f(1f, 1f, 1f, 0.30f),
                    velocity = Vector3f(0.006f, 0f, 0.002f),
                ),
                CloudBillboard(
                    position = Vector3f(48f, 62f, -42f),
                    size = Vector2f(72f, 24f),
                    texture = cloudBaseTexture,
                    tint = Vector4f(1f, 1f, 1f, 0.26f),
                    velocity = Vector3f(0.004f, 0f, -0.001f),
                ),
                CloudBillboard(
                    position = Vector3f(82f, 45f, -5f),
                    size = Vector2f(48f, 17f),
                    texture = cloudDetailTexture,
                    tint = Vector4f(1f, 1f, 1f, 0.22f),
                    velocity = Vector3f(0.008f, 0f, 0.001f),
                ),
            )
        )
        scene.world.entities.addAll(cloudBillboards)
        addGrassVerificationFixture()
    }

    override fun input(delta: Float) {
        super.input(delta)
        val windKeyDown = glfwGetKey(SMF.window.id, GLFW_KEY_G) == GLFW_PRESS
        if (windKeyDown && !windKeyWasDown) {
            grass.windStrength = if (grass.windStrength > 0f) 0f else GrassPatch.DEFAULT_WIND_STRENGTH
        }
        windKeyWasDown = windKeyDown
        val timeKeyDown = glfwGetKey(SMF.window.id, GLFW_KEY_T) == GLFW_PRESS
        if (timeKeyDown && !timeKeyWasDown) {
            timePaused = !timePaused
            skySystem.paused = timePaused
        }
        timeKeyWasDown = timeKeyDown
    }

    override fun update(delta: Float) {
        super.update(delta)
        if (!timePaused) grass.update(delta / FixedTimestepGame.TARGET_UPS)
        cloudBillboards.forEach { cloud ->
            cloud.wrapWithin(
                minX = CLOUD_WRAP_MIN,
                maxX = CLOUD_WRAP_MAX,
                minZ = CLOUD_WRAP_MIN,
                maxZ = CLOUD_WRAP_MAX,
            )
        }
        if (!timePaused) weatherCycle.update(delta / FixedTimestepGame.TARGET_UPS)

        val weatherCloudColor = skySystem.sky?.cloudColor ?: return
        cloudBillboards.forEach { cloud ->
            val alpha = cloud.billboard.tint.w
            cloud.billboard.tint = Vector4f(
                weatherCloudColor.x,
                weatherCloudColor.y,
                weatherCloudColor.z,
                alpha,
            )
        }

    }

    override fun unload() {
        scene.world.grassPatches.clear()
        SMF.renderer.grassRenderer.clear()
        SMF.renderer.debugRenderer.clearOverlayPrimitives()
        SMF.renderer.debugRenderer.setEnabled(DebugVisual.COLLIDERS, false)
        super.unload()
    }

    private fun addGrassVerificationFixture() {
        val terrain = requireNotNull(scene.world.terrain)
        val camera = scene.world.camera.position
        val x = camera.x + 20f
        val z = camera.z
        val size = Vector2f(minOf(72f, terrain.model.mesh.size.x), minOf(72f, terrain.model.mesh.size.y))
        val origin = Vector2f(
            (x - size.x / 2f).coerceIn(terrain.position.x, terrain.position.x + terrain.model.mesh.size.x - size.x),
            (z - size.y * 0.8f).coerceIn(terrain.position.z, terrain.position.z + terrain.model.mesh.size.y - size.y),
        )
        grass = GrassPatch.scatter(
            terrain = terrain,
            origin = origin,
            size = size,
            spacing = 0.9f,
            seed = 17,
        )
        scene.world.grassPatches += grass
        player.setFeetPosition(Vector3f(x, terrain.getHeight(x, z), z))
        player.camera.setFront(Vector3f(0f, -0.18f, -1f))
    }

    private fun addShadowVerificationFixture() {
        val camera = scene.world.camera.position
        val x = camera.x
        val z = camera.z - 9f
        val ground = scene.world.terrain?.getHeight(x, z) ?: 0f
        val assembly = ObjectEntity(Transform(position = Vector3f(x, ground, z)))
        assembly.addChild(ShadowTestBlockEntity(
            Transform(position = Vector3f(0f, 0.2f, 0f), scale = Vector3f(8f, 0.4f, 6f)),
            Vector4f(0.75f, 0.75f, 0.75f, 1f)))
        assembly.addChild(ShadowTestBlockEntity(
            Transform(position = Vector3f(-2f, 1.9f, 0f), scale = Vector3f(0.8f, 3f, 0.8f))))
        assembly.addChild(ShadowTestBlockEntity(
            Transform(position = Vector3f(0f, 3.6f, 0f), scale = Vector3f(5f, 0.4f, 1.4f))))
        assembly.addChild(LightEntity(
            transform = Transform(position = Vector3f(0f, 4.2f, 0f)),
            light = PointLight(
                diffuse = Vector3f(1f, 0.52f, 0.18f),
                specular = Vector3f(1f, 0.72f, 0.42f),
                intensity = 18f,
                linear = 0.12f,
                quadratic = 0.035f,
                castsShadow = true,
                shadowStrength = 0.62f,
            ),
            displayName = "Lantern Light",
        ))
        scene.world.entities += assembly
        scene.world.entities += ShadowTestBlockEntity(
            Transform(position = Vector3f(x + 1.5f, ground + 0.9f, z), scale = Vector3f(1f, 1f, 1f)),
            Vector4f(0.65f, 0.75f, 0.90f, 1f))
    }

    private fun addDebugVisualVerificationFixture() {
        val camera = scene.world.camera.position
        val z = camera.z - 6f
        val ground = scene.world.terrain?.getHeight(camera.x, z) ?: 0f
        val origin = Vector3f(camera.x, ground + 0.05f, z)

        SMF.renderer.debugRenderer.setOverlayPrimitives(
            listOf(
                DebugBox(
                    origin.add(Vector3f(-2f, 1f, 0f)),
                    Vector3f(1.5f, 2f, 1.5f),
                    Vector3f(0f, 30f, 0f),
                    Vector4f(0.2f, 0.65f, 1f, 1f),
                ),
                DebugSphere(
                    origin.add(Vector3f(0f, 1f, 0f)),
                    1f,
                    Vector4f(0.2f, 1f, 0.55f, 1f),
                ),
                DebugCapsule(
                    origin.add(Vector3f(2f, 0f, 0f)),
                    0.55f,
                    2f,
                    Vector4f(1f, 0.75f, 0.2f, 1f),
                ),
                DebugLine(origin, origin.add(Vector3f(0f, 3f, 0f)), Vector4f(1f, 0.25f, 0.25f, 1f)),
            )
        )
    }

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

    private companion object {
        val LOAD_COUNT = AtomicInteger()
        const val CLOUD_WRAP_MIN = -160f
        const val CLOUD_WRAP_MAX = 160f
    }
}
