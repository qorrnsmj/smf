package qorrnsmj.smf.graphic.skydome

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.SceneRenderer
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.settings.ViewportShadingSettings
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SkydomeRenderer : SceneRenderer, Resizable {
    private val program = SkydomeShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationTime = glGetUniformLocation(program.id, "time")
    private val locationCloudScale = glGetUniformLocation(program.id, "cloudScale")
    private val locationCloudCoverage = glGetUniformLocation(program.id, "cloudCoverage")
    private val locationCloudSoftness = glGetUniformLocation(program.id, "cloudSoftness")
    private val locationHorizonColor = glGetUniformLocation(program.id, "horizonColor")
    private val locationZenithColor = glGetUniformLocation(program.id, "zenithColor")
    private val locationCloudColor = glGetUniformLocation(program.id, "cloudColor")
    private val locationUseCloudTextures = glGetUniformLocation(program.id, "useCloudTextures")
    private val locationCloudBaseTexture = glGetUniformLocation(program.id, "cloudBaseTexture")
    private val locationCloudDetailTexture = glGetUniformLocation(program.id, "cloudDetailTexture")
    private val locationUseSunTexture = glGetUniformLocation(program.id, "useSunTexture")
    private val locationSunTexture = glGetUniformLocation(program.id, "sunTexture")
    private val locationSunDirection = glGetUniformLocation(program.id, "sunDirection")
    private val locationSunColor = glGetUniformLocation(program.id, "sunColor")
    private val locationSunAngularSize = glGetUniformLocation(program.id, "sunAngularSize")
    private val locationSunHorizonGlowStrength = glGetUniformLocation(program.id, "sunHorizonGlowStrength")
    private val locationUseMoonTexture = glGetUniformLocation(program.id, "useMoonTexture")
    private val locationMoonTexture = glGetUniformLocation(program.id, "moonTexture")
    private val locationMoonColor = glGetUniformLocation(program.id, "moonColor")
    private val locationMoonAngularSize = glGetUniformLocation(program.id, "moonAngularSize")
    private val mesh = createDomeMesh()

    override fun render(scene: Scene) {
        val skydome = scene.environment.skydome ?: return
        val visible = skydome.enabled && shouldRenderSky(scene)
        val clearColor = if (visible) skydome.horizonColor else EDITOR_SKY_HIDDEN_COLOR
        glClearColor(clearColor.x, clearColor.y, clearColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        if (!visible) return

        start()
        loadCamera(scene.world.camera)
        loadSkydome(skydome)
        renderMesh()
        stop()
    }

    private fun shouldRenderSky(scene: Scene): Boolean =
        scene.renderSettings.viewportShadingMode == ViewportShadingSettings.RENDERED &&
            scene.environment.skyVisible

    private fun start() {
        program.use()
        glDepthMask(false)
        glDisable(GL_CULL_FACE)
        glDepthFunc(GL_LEQUAL)
    }

    private fun stop() {
        glActiveTexture(GL_TEXTURE0)
        glDepthMask(true)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glDepthFunc(GL_LESS)
    }

    private fun loadCamera(camera: Camera) {
        val view = camera.getViewMatrix()
        view.m03 = 0f
        view.m13 = 0f
        view.m23 = 0f
        UniformUtils.setUniform(locationModel, Matrix4f())
        UniformUtils.setUniform(locationView, view)
    }

    private fun loadSkydome(skydome: Skydome) {
        UniformUtils.setUniform(locationTime, skydome.time)
        UniformUtils.setUniform(locationCloudScale, skydome.cloudScale)
        UniformUtils.setUniform(locationCloudCoverage, skydome.cloudCoverage)
        UniformUtils.setUniform(locationCloudSoftness, skydome.cloudSoftness)
        UniformUtils.setUniform(locationHorizonColor, skydome.horizonColor)
        UniformUtils.setUniform(locationZenithColor, skydome.zenithColor)
        UniformUtils.setUniform(locationCloudColor, skydome.cloudColor)
        UniformUtils.setUniform(locationSunDirection, skydome.sunDirection)
        UniformUtils.setUniform(locationSunColor, skydome.sunColor)
        UniformUtils.setUniform(locationSunAngularSize, skydome.sunAngularSize)
        UniformUtils.setUniform(locationSunHorizonGlowStrength, skydome.sunHorizonGlowStrength)
        UniformUtils.setUniform(locationMoonColor, skydome.moonColor)
        UniformUtils.setUniform(locationMoonAngularSize, skydome.moonAngularSize)

        val hasSunTexture = skydome.sunTexture != null
        UniformUtils.setUniform(locationUseSunTexture, if (hasSunTexture) 1 else 0)
        if (hasSunTexture) {
            glActiveTexture(GL_TEXTURE2)
            skydome.sunTexture?.bind()
            UniformUtils.setUniform(locationSunTexture, 2)
        }

        val hasMoonTexture = skydome.moonTexture != null
        UniformUtils.setUniform(locationUseMoonTexture, if (hasMoonTexture) 1 else 0)
        if (hasMoonTexture) {
            glActiveTexture(GL_TEXTURE3)
            skydome.moonTexture?.bind()
            UniformUtils.setUniform(locationMoonTexture, 3)
        }

        val hasCloudTextures = skydome.cloudBaseTexture != null && skydome.cloudDetailTexture != null
        UniformUtils.setUniform(locationUseCloudTextures, if (hasCloudTextures) 1 else 0)
        if (!hasCloudTextures) return

        glActiveTexture(GL_TEXTURE0)
        skydome.cloudBaseTexture?.bind()
        UniformUtils.setUniform(locationCloudBaseTexture, 0)
        glActiveTexture(GL_TEXTURE1)
        skydome.cloudDetailTexture?.bind()
        UniformUtils.setUniform(locationCloudDetailTexture, 1)
    }

    private fun renderMesh() {
        glBindVertexArray(mesh.vao)
        glEnableVertexAttribArray(0)
        glDrawElements(GL_TRIANGLES, mesh.indexCount, GL_UNSIGNED_INT, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    private data class DomeMesh(val vao: Int, val indexCount: Int)

    private fun createDomeMesh(rings: Int = 24, segments: Int = 64): DomeMesh {
        val vertices = ArrayList<Float>((rings + 1) * (segments + 1) * 3)
        val indices = ArrayList<Int>(rings * segments * 6)

        for (ring in 0..rings) {
            val theta = ring / rings.toFloat() * (PI.toFloat() * 0.5f)
            val y = sin(theta)
            val radius = cos(theta)
            for (segment in 0..segments) {
                val phi = segment / segments.toFloat() * PI.toFloat() * 2f
                vertices.add(cos(phi) * radius)
                vertices.add(y)
                vertices.add(sin(phi) * radius)
            }
        }

        for (ring in 0 until rings) {
            for (segment in 0 until segments) {
                val rowStart = ring * (segments + 1)
                val nextRowStart = (ring + 1) * (segments + 1)
                val topLeft = rowStart + segment
                val topRight = topLeft + 1
                val bottomLeft = nextRowStart + segment
                val bottomRight = bottomLeft + 1
                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)
                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }

        val vao = glGenVertexArrays()
        val vbo = glGenBuffers()
        val ebo = glGenBuffers()
        glBindVertexArray(vao)

        val vertexBuffer = MemoryUtil.memAllocFloat(vertices.size)
        vertexBuffer.put(vertices.toFloatArray()).flip()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)

        val indexBuffer = MemoryUtil.memAllocInt(indices.size)
        indexBuffer.put(indices.toIntArray()).flip()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glBindVertexArray(0)
        MemoryUtil.memFree(vertexBuffer)
        MemoryUtil.memFree(indexBuffer)

        return DomeMesh(vao, indices.size)
    }

    private companion object {
        val EDITOR_SKY_HIDDEN_COLOR = Vector3f(0.08f, 0.09f, 0.1f)
    }
}
