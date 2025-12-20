package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.graphic.render.shader.SkyboxShaderProgram
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.UniformUtils
import qorrnsmj.smf.util.impl.Cleanable
import qorrnsmj.smf.util.impl.Resizable

class SkyboxRenderer : Resizable, Cleanable {
    // TODO: locationはProgramクラスの中にしまえない？
    private val program = SkyboxShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationTexImage = glGetUniformLocation(program.id, "texImage")
    private val locationSkyColor = glGetUniformLocation(program.id, "skyColor")

    fun start() {
        program.use()

        glDepthMask(false)
        glDisable(GL_CULL_FACE)
        glDepthFunc(GL_LEQUAL)
    }

    fun stop() {
        glDepthMask(true)
        glEnable(GL_CULL_FACE)
        glDepthFunc(GL_LESS)
    }

    fun renderSkybox(model: Model) {
        // identity model
        UniformUtils.setUniform(locationModel, Matrix4f())

        // bind cubemap to unit 0
        val tex = model.material.diffuseTexture
        glActiveTexture(GL_TEXTURE0)
        tex.bind()
        UniformUtils.setUniform(locationTexImage, 0)

        // sky color fallback
        UniformUtils.setUniform(locationSkyColor, Vector3f(0.5f, 0.7f, 1.0f))

        // draw
        glBindVertexArray(model.mesh.vaoID)
        glEnableVertexAttribArray(0)
        glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    fun loadCamera(camera: Camera) {
        val view = camera.getViewMatrix()
        // clear translation components of your Matrix4f (project's Matrix4f stores trans in m03/m13/m23)
        view.m03 = 0f
        view.m13 = 0f
        view.m23 = 0f
        UniformUtils.setUniform(locationView, view)
    }

    override fun resize(width: Int, height: Int) {
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    override fun cleanup() {
        glDeleteProgram(program.id)

        Logger.info("SkyboxRenderer cleaned up!")
    }
}
