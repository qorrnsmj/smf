package qorrnsmj.smf.game.level.custom

import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import qorrnsmj.smf.SMF
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.game.entity.billboard.CloudBillboard
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.ShadowTestBlockEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.game.entity.mob.SlimeEntity
import qorrnsmj.smf.game.weather.WeatherCycle
import qorrnsmj.smf.game.weather.WeatherPresets
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skydome.Skydome
import qorrnsmj.smf.graphic.text.Font
import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.graphic.text.TextBoxElement
import qorrnsmj.smf.graphic.text.TextRun
import qorrnsmj.smf.graphic.text.TextStyle
import qorrnsmj.smf.graphic.texture.TextureLoader
import qorrnsmj.smf.graphic.texture.TexturePresets
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import java.io.File

class TestLevel : BaseLevel("test") {
    private lateinit var weatherCycle: WeatherCycle
    private lateinit var textBoxFont: Font
    private lateinit var textBoxSmallFont: Font
    private lateinit var textBoxSpeaker: SlimeEntity
    private val cloudBillboards = mutableListOf<CloudBillboard>()

    override fun load() {
        currentBranchName()?.let { glfwSetWindowTitle(SMF.window.id, it) }
        super.load()
        scene.world.lights.removeAll { it is PointLight }
        addShadowVerificationFixture()
        addTextBoxVerificationFixture()
        textBoxFont = FontLoader.loadAssetFont("Inconsolata.ttf", 22f)
        textBoxSmallFont = FontLoader.loadAssetFont("Inconsolata.ttf", 18f)

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
        scene.environment.skydome = Skydome(
            cloudBaseTexture = cloudBaseTexture,
            cloudDetailTexture = cloudDetailTexture,
            sunTexture = sunTexture,
            moonTexture = moonTexture,
            horizonColor = scene.environment.skyColor,
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
    }

    override fun update(delta: Float) {
        super.update(delta)
        cloudBillboards.forEach { cloud ->
            cloud.wrapWithin(
                minX = CLOUD_WRAP_MIN,
                maxX = CLOUD_WRAP_MAX,
                minZ = CLOUD_WRAP_MIN,
                maxZ = CLOUD_WRAP_MAX,
            )
        }
        scene.environment.skydome?.update(delta)
        weatherCycle.update(delta / FixedTimestepGame.TARGET_UPS)

        val weatherCloudColor = scene.environment.skydome?.cloudColor ?: return
        cloudBillboards.forEach { cloud ->
            val alpha = cloud.billboard.tint.w
            cloud.billboard.tint = Vector4f(
                weatherCloudColor.x,
                weatherCloudColor.y,
                weatherCloudColor.z,
                alpha,
            )
        }

        scene.world.textBoxes.clear()
        scene.world.textBoxes.add(createTextBoxTestFixture())
    }

    private fun addTextBoxVerificationFixture() {
        textBoxSpeaker = SlimeEntity(Vector3f(120f, 5f, 160f)).apply {
            displayName = "Slime Clerk"
        }
        scene.world.entities.add(textBoxSpeaker)
    }

    private fun createTextBoxTestFixture(): TextBoxElement {
        val normalStyle = TextStyle(textBoxFont, Vector3f(0.92f, 0.94f, 0.98f))
        val boldStyle = TextStyle(textBoxFont, Vector3f(1f, 0.95f, 0.68f), bold = true)
        val hintStyle = TextStyle(textBoxSmallFont, Vector3f(0.55f, 0.75f, 1f))
        return TextBoxElement(
            runs = listOf(
                TextRun("Entity displayName is linked here. ", normalStyle),
                TextRun("Bold", boldStyle),
                TextRun(" and ", normalStyle),
                TextRun("colored", TextStyle(textBoxFont, Vector3f(0.55f, 1f, 0.65f))),
                TextRun(" runs can share one text box.\n", normalStyle),
                TextRun("Font can switch per run for hints or system text.", hintStyle),
            ),
            x = 220f,
            y = 520f,
            width = 720f,
            speaker = textBoxSpeaker,
            speakerStyle = TextStyle(textBoxFont, Vector3f(1f, 0.93f, 0.72f), bold = true),
            backgroundColor = Vector4f(0.04f, 0.05f, 0.07f, 0.88f),
            borderColor = Vector4f(0.95f, 0.92f, 0.75f, 0.45f),
        )
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
        scene.world.entities += assembly
        scene.world.entities += ShadowTestBlockEntity(
            Transform(position = Vector3f(x + 1.5f, ground + 0.9f, z), scale = Vector3f(1f, 1f, 1f)),
            Vector4f(0.65f, 0.75f, 0.90f, 1f))
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
        const val CLOUD_WRAP_MIN = -160f
        const val CLOUD_WRAP_MAX = 160f
    }
}
