package qorrnsmj.smf.graphic.scene.grass

import qorrnsmj.smf.graphic.renderer.GrassRenderer
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.graphic.resource.model.Material
import qorrnsmj.smf.graphic.resource.model.Mesh
import qorrnsmj.smf.graphic.resource.model.Model
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.SceneEnvironment
import qorrnsmj.smf.graphic.scene.SkySettings
import qorrnsmj.smf.graphic.scene.light.DirectionalLight
import qorrnsmj.smf.graphic.scene.light.PointLight
import qorrnsmj.smf.graphic.scene.shadow.ShadowRenderState
import qorrnsmj.smf.graphic.scene.sky.Skybox
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Matrix4f
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrassGpuTest {
    @Test
    fun instancedTexturedFoliageAnimatesShadowsAndReleasesResources() {
        assumeTrue(System.getenv("SMF_RUN_GL_TESTS") == "1", "Requires a local OpenGL driver")
        check(glfwInit())
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        val window = glfwCreateWindow(128, 128, "Grass regression", 0, 0)
        check(window != 0L)
        try {
            glfwMakeContextCurrent(window)
            GL.createCapabilities()
            verifyGrass()
        } finally {
            GL.setCapabilities(null)
            glfwDestroyWindow(window)
            glfwTerminate()
        }
    }

    private fun verifyGrass() {
        val dummy = TextureBufferObject()
        val material = Material(baseColorTexture = dummy, metallicRoughnessTexture = dummy,
            normalTexture = dummy, occlusionTexture = dummy, emissiveTexture = dummy)
        val scene = Scene(environment = SceneEnvironment(
            sky = SkySettings(skybox = Skybox(Model(Mesh(), material))),
        ))
        scene.world.camera.position = Vector3f(0f, 0.5f, 2.5f)
        val grass = GrassPatch(listOf(
            GrassInstance(Vector3f(-0.6f, 0f, 0f), 1f, 0f, 0f, 1f),
            GrassInstance(Vector3f(0.6f, 0f, 0f), 1f, 0.4f, 2f, 0.9f),
        ))
        scene.world.grassPatches += grass
        var shadowState: ShadowRenderState? = null
        val renderer = GrassRenderer { shadowState }
        renderer.resize(128, 128)
        glViewport(0, 0, 128, 128)
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glEnable(GL_BLEND)
        fun render(): FloatArray {
            glDepthMask(true)
            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glDepthMask(false)
            renderer.render(scene)
            assertEquals(GL_NO_ERROR, glGetError(), "Grass rendering must not produce OpenGL errors")
            assertTrue(glIsEnabled(GL_CULL_FACE), "Restore culling")
            assertTrue(glIsEnabled(GL_BLEND), "Restore blending")
            assertFalse(glGetBoolean(GL_DEPTH_WRITEMASK), "Restore depth mask")
            return FloatArray(128 * 128 * 4).also {
                glReadPixels(0, 0, 128, 128, GL_RGBA, GL_FLOAT, it)
            }
        }
        try {
            grass.windStrength = 0f
            val still = render()
            val covered = (0 until 128 * 128).filter { still[it * 4 + 1] > 0.05f }
            assertTrue(covered.size in 100..8000, "Alpha-cut grass cards must render with open space between blades")
            assertTrue(covered.count { it % 128 < 64 } > 50, "First instance must render")
            assertTrue(covered.count { it % 128 >= 64 } > 50, "Second instance must render")
            grass.update(0.7f)
            assertContentEquals(still, render(), "Zero wind must remain still as time advances")

            scene.environment.celestialLight = DirectionalLight(
                intensity = 0f,
                ambientColor = Vector3f(1f, 1f, 1f),
                ambientIntensity = 0.03f,
            )
            val dimmed = render()
            scene.world.lights += PointLight(
                position = Vector3f(0f, 1.5f, 1f),
                diffuse = Vector3f(1f, 0.8f, 0.55f),
                intensity = 5f,
                linear = 0.05f,
                quadratic = 0.01f,
            )
            val locallyLit = render()
            assertTrue(
                locallyLit.filterIndexed { index, _ -> index % 4 == 1 }.sum() >
                    dimmed.filterIndexed { index, _ -> index % 4 == 1 }.sum() * 2f,
                "Point lights must visibly illuminate grass",
            )
            scene.world.lights.clear()
            scene.environment.celestialLight = null

            val shadowTexture = createShadowTexture()
            try {
                shadowState = ShadowRenderState(
                    enabled = true,
                    lightSpaceMatrix = Matrix4f(),
                    depthTextureId = shadowTexture,
                    strength = 1f,
                    receiverMinBias = 0f,
                )
                val shadowed = render()
                assertTrue(shadowed.filterIndexed { index, _ -> index % 4 == 1 }.sum() <
                    still.filterIndexed { index, _ -> index % 4 == 1 }.sum() * 0.1f,
                    "Directional shadow maps must visibly darken grass")
            } finally {
                shadowState = null
                glDeleteTextures(shadowTexture)
            }

            grass.windStrength = 0.45f
            val windFirst = render()
            grass.update(0.7f)
            val windSecond = render()
            assertTrue(windFirst.indices.count { kotlin.math.abs(windFirst[it] - windSecond[it]) > 0.05f } > 50,
                "Wind must visibly change the rendered image")
            savePreview(windSecond)

            grass.drawDistance = 1f
            val distant = render()
            assertTrue(distant.all { it == 0f }, "Grass outside draw distance must disappear")

            scene.world.grassPatches.clear()
            render()
            val flowers = GrassPatch(
                listOf(GrassInstance(Vector3f(0f, 0f, 0f), 1f, 0f, 0f, 1f)),
                FoliageType.FLOWERS,
            )
            flowers.windStrength = 0f
            scene.world.grassPatches += flowers
            val flowerPixels = render()
            assertTrue((0 until 128 * 128).any { index ->
                flowerPixels[index * 4 + 2] > flowerPixels[index * 4] * 1.25f &&
                    flowerPixels[index * 4 + 2] > 0.2f
            }, "Low-poly flower geometry must include visible blue petals")

            scene.world.grassPatches.clear()
            render()
            scene.world.grassPatches += grass
            grass.drawDistance = 60f
            assertTrue(render().any { it > 0.05f }, "Grass must reload after a level switch")
        } finally {
            renderer.clear()
            dummy.delete()
        }
    }

    private fun createShadowTexture(): Int = glGenTextures().also { texture ->
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, 1, 1, 0, GL_RED, GL_FLOAT, floatArrayOf(0f))
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun savePreview(pixels: FloatArray) {
        val image = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val offset = (y * 128 + x) * 4
                fun channel(index: Int) = (pixels[offset + index].coerceIn(0f, 1f) * 255).toInt()
                val color = (channel(3) shl 24) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
                image.setRGB(x, 127 - y, color)
            }
        }
        val file = File("build/grass-verification.png")
        file.parentFile.mkdirs()
        ImageIO.write(image, "png", file)
    }
}
