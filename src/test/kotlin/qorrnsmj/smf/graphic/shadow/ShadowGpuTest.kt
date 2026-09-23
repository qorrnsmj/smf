package qorrnsmj.smf.graphic.shadow

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.entity.ModelRenderer
import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.graphic.resource.model.Material
import qorrnsmj.smf.graphic.resource.model.Mesh
import qorrnsmj.smf.graphic.resource.model.Model
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.util.UniformUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShadowGpuTest {
    @Test
    fun shadowDepthAndCrossObjectReceiving() {
        assumeTrue(System.getenv("SMF_RUN_GL_TESTS") == "1", "Requires a local OpenGL driver")
        check(glfwInit())
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        val window = glfwCreateWindow(32, 32, "Shadow regression", 0, 0)
        check(window != 0L)
        try {
            glfwMakeContextCurrent(window)
            GL.createCapabilities()
            checkDepthPass()
            checkMultipleMaterialShadowSampling()
        } finally {
            GL.setCapabilities(null)
            glfwDestroyWindow(window)
            glfwTerminate()
        }
    }

    private fun checkMultipleMaterialShadowSampling() {
        val white = TextureBufferObject()
        val normal = TextureBufferObject()
        val depth = glGenTextures()
        val vao = glGenVertexArrays()
        val vertices = glGenBuffers()
        val indices = glGenBuffers()
        fun texture(texture: TextureBufferObject, color: FloatArray) {
            glActiveTexture(GL_TEXTURE0)
            texture.bind()
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 1, 1, 0, GL_RGBA, GL_FLOAT, color)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        }
        texture(white, floatArrayOf(1f, 1f, 1f, 1f))
        texture(normal, floatArrayOf(0.5f, 0.5f, 1f, 1f))
        val material = Material(baseColorTexture = white, metallicRoughnessTexture = white,
            normalTexture = normal, occlusionTexture = white, emissiveTexture = white, doubleSided = true)
        EntityModels.EMPTY = Model(Mesh(), material)
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vertices)
        glBufferData(GL_ARRAY_BUFFER, floatArrayOf(
            -0.4f, -0.8f, 0f, 0.4f, -0.8f, 0f, 0.4f, 0.8f, 0f, -0.4f, 0.8f, 0f,
        ), GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L)
        glEnableVertexAttribArray(0)
        glVertexAttrib2f(1, 0.5f, 0.5f)
        glVertexAttrib3f(2, 0f, 0f, 1f)
        glVertexAttrib4f(3, 1f, 0f, 0f, 1f)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indices)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, intArrayOf(0, 1, 2, 0, 2, 3), GL_STATIC_DRAW)
        glBindVertexArray(0)
        val scene = Scene(environment = qorrnsmj.smf.graphic.scene.SkyEnvironment(
            skybox = qorrnsmj.smf.graphic.skybox.Skybox(EntityModels.EMPTY)))
        scene.world.camera.position = Vector3f(0f, 0f, 2f)
        scene.environment.fog.enabled = false
        scene.environment.celestialLight = DirectionalLight(direction = Vector3f(0f, 0f, -1f),
            intensity = 0f, ambientColor = Vector3f(1f, 1f, 1f), ambientIntensity = 0.6f)
        for (x in listOf(-0.6f, 0.6f)) {
            scene.world.entities += object : Entity(Transform(position = Vector3f(x, 0f, 0f)),
                Model(Mesh(vao, 6), material.copy())) {}
        }
        val shadowRenderer = ShadowRenderer()
        val renderer = ModelRenderer(shadowRenderer)
        renderer.resize(32, 32)
        // Supply a controlled map to exercise the production material batching and receiving shader.
        val render = ModelRenderer::class.java.getDeclaredMethod("render", Scene::class.java, ShadowRenderState::class.java)
        render.isAccessible = true
        fun renderPixels(state: ShadowRenderState): FloatArray {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glViewport(0, 0, 32, 32)
            glClearColor(0f, 0f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            render.invoke(renderer, scene, state)
            assertEquals(GL_NO_ERROR, glGetError(), "Model rendering must not produce OpenGL errors")
            return listOf(11, 20).map { x ->
                val pixel = FloatArray(4)
                glReadPixels(x, 16, 1, 1, GL_RGBA, GL_FLOAT, pixel)
                pixel[0]
            }.toFloatArray()
        }
        fun draw(mapDepth: Float): FloatArray {
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, depth)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 1, 1, 0, GL_DEPTH_COMPONENT, GL_FLOAT, floatArrayOf(mapDepth))
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            return renderPixels(ShadowRenderState(true, Matrix4f(), depth, 1f))
        }
        try {
            val lit = draw(1f)
            assertTrue(lit[0] > 0.5f, "First material must be lit: ${lit.toList()}")
            assertEquals(lit[0], lit[1], 0.01f, "Shadow binding must survive material switches")
            val shadowed = draw(0f)
            assertTrue(shadowed[0] < lit[0] * 0.7f, "First material must receive a shadow")
            assertTrue(shadowed[1] < lit[1] * 0.7f, "Second material must receive a shadow")

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, depth)
            val edgeDepth = FloatArray(8 * 8) { if (it % 8 < 4) 0f else 1f }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 8, 8, 0, GL_DEPTH_COMPONENT, GL_FLOAT, edgeDepth)
            val edgeSamples = (0..64).map { step ->
                val matrix = Matrix4f().apply { m03 = 0.3125f + step / 128f }
                renderPixels(ShadowRenderState(true, matrix, depth, 1f))[0]
            }
            val largestStep = edgeSamples.zipWithNext { a, b -> kotlin.math.abs(a - b) }.max()
            assertTrue(edgeSamples.last() - edgeSamples.first() > 0.1f, "The sweep must cross a visible shadow edge")
            assertTrue(largestStep < 0.025f, "A subtexel movement must not jump between PCF bins: $largestStep")

            val light = scene.environment.celestialLight!!
            light.direction = Vector3f(0f, -1f, -1f)
            light.shadowStrength = 1f
            fun generatedMapPixels(): FloatArray {
                light.intensity = 1f
                shadowRenderer.render(scene)
                light.intensity = 0f
                return renderPixels(shadowRenderer.currentState)
            }
            val baseline = generatedMapPixels()
            assertTrue(baseline.all { it > 0.5f }, "Unoccluded model surfaces must stay lit: ${baseline.toList()}")

            val caster = object : Entity(Transform(position = Vector3f(0f, 0f, 1f),
                scale = Vector3f(5f, 5f, 1f)), Model(Mesh(vao, 6), material)) {}
            // The first receiver and caster share a parent; the second receiver is independent.
            val receiver = scene.world.entities.removeAt(0)
            val assembly = object : Entity() {}
            assembly.addChild(receiver)
            assembly.addChild(caster)
            scene.world.entities += assembly
            light.intensity = 1f
            shadowRenderer.render(scene)
            assembly.removeChild(caster)
            light.intensity = 0f
            val entityShadow = renderPixels(shadowRenderer.currentState)
            assertTrue(entityShadow.all { it < 0.4f }, "Entity must shadow both sibling and independent receivers: ${entityShadow.toList()}")

            caster.localTransform = caster.localTransform.copy(position = Vector3f(0f, 0f, 0.05f))
            assembly.addChild(caster)
            light.intensity = 1f
            shadowRenderer.render(scene)
            assembly.removeChild(caster)
            light.intensity = 0f
            val contactShadow = renderPixels(shadowRenderer.currentState)
            assertTrue(contactShadow.all { it < 0.4f }, "Bias must preserve shadows only 5 cm from a receiver: ${contactShadow.toList()}")

            scene.world.terrain = qorrnsmj.smf.graphic.terrain.TerrainLoader.loadHeightGridModel(
                sizeX = 4f, sizeY = 4f, heightGrid = Array(2) { FloatArray(2) },
                position = Vector3f(-2f, 1f, 0.5f),
                textureMode = qorrnsmj.smf.graphic.terrain.component.SingleTexture(white))
            val terrainShadow = generatedMapPixels()
            assertTrue(terrainShadow.all { it < 0.4f }, "Terrain must cast onto both model materials: ${terrainShadow.toList()}")
        } finally {
            renderer.program.delete()
            glDeleteBuffers(vertices)
            glDeleteBuffers(indices)
            glDeleteVertexArrays(vao)
            glDeleteTextures(depth)
            white.delete()
            normal.delete()
        }
    }

    private fun checkDepthPass() {
        val program = ShadowShaderProgram()
        val framebuffer = glGenFramebuffers()
        val depth = glGenTextures()
        val vao = glGenVertexArrays()
        val vertices = glGenBuffers()
        try {
            assertEquals(GL_TRUE, glGetProgrami(program.id, GL_LINK_STATUS), glGetProgramInfoLog(program.id))
            glBindTexture(GL_TEXTURE_2D, depth)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 32, 32, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0)
            glDrawBuffer(GL_NONE)
            glReadBuffer(GL_NONE)
            assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER))
            glViewport(0, 0, 32, 32)
            glEnable(GL_DEPTH_TEST)
            glDepthFunc(GL_LESS)
            glDepthMask(true)
            glDisable(GL_CULL_FACE)
            glClearDepth(1.0)
            glClear(GL_DEPTH_BUFFER_BIT)
            program.use()
            UniformUtils.setUniform(glGetUniformLocation(program.id, "model"), Matrix4f())
            UniformUtils.setUniform(glGetUniformLocation(program.id, "lightSpaceMatrix"), Matrix4f())
            glUniform1i(glGetUniformLocation(program.id, "pointShadowPass"), 0)
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vertices)
            glEnableVertexAttribArray(0)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L)

            fun draw(z: Float) {
                glBufferData(GL_ARRAY_BUFFER, floatArrayOf(-1f, -1f, z, 3f, -1f, z, -1f, 3f, z), GL_STATIC_DRAW)
                glDrawArrays(GL_TRIANGLES, 0, 3)
            }
            fun readDepth(): Float {
                val result = FloatArray(1)
                glReadPixels(16, 16, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, result)
                assertEquals(GL_NO_ERROR, glGetError())
                return result[0]
            }

            draw(0.5f)
            val receiver = readDepth()
            assertEquals(0.75f, receiver, 0.00001f, "Directional receiver depth must not be undefined")
            draw(-0.5f)
            val caster = readDepth()
            assertEquals(0.25f, caster, 0.00001f, "A nearer entity must replace the terrain depth")
            draw(0.5f)
            assertEquals(caster, readDepth(), "A farther surface must not overwrite the caster")
            assertTrue(receiver - 0.001f > caster, "The receiver must classify as shadowed")

            glClear(GL_DEPTH_BUFFER_BIT)
            glUniform1i(glGetUniformLocation(program.id, "pointShadowPass"), 1)
            glUniform3f(glGetUniformLocation(program.id, "pointLightPosition"), 0f, 0f, 0f)
            glUniform1f(glGetUniformLocation(program.id, "pointShadowFarPlane"), 2f)
            draw(0.5f)
            val expectedPointDepth = kotlin.math.sqrt(0.5f * 0.5f + 2f * 0.03125f * 0.03125f) / 2f
            assertEquals(expectedPointDepth, readDepth(), 0.00001f, "Point shadows must retain radial depth")
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glBindVertexArray(0)
            glDeleteBuffers(vertices)
            glDeleteVertexArrays(vao)
            glDeleteTextures(depth)
            glDeleteFramebuffers(framebuffer)
            program.delete()
        }
    }
}
