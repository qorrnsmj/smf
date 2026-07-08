package qorrnsmj.smf.graphic.skybox

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.settings.ViewportShadingSettings
import qorrnsmj.smf.graphic.SceneRenderer
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.UniformUtils
import qorrnsmj.smf.util.Resizable

class SkyboxRenderer : SceneRenderer, Resizable {
    // TODO: locationはProgramクラスの中にしまえない？
    private val program = SkyboxShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationTexImage = glGetUniformLocation(program.id, "texImage")
    private val locationSkyColor = glGetUniformLocation(program.id, "skyColor")

    override fun render(scene: Scene) {
        start(scene)
        if (!shouldRenderSkybox(scene)) {
            stop()
            return
        }
        loadCamera(scene.world.camera)
        loadSkyColor(scene.environment.skyColor)
        renderSkybox(scene.environment.skybox)
        stop()
    }

    private fun start(scene: Scene) {
        val skyColor = if (shouldRenderSkybox(scene)) scene.environment.skyColor else EDITOR_SKY_HIDDEN_COLOR
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        program.use()

        glDepthMask(false)
        glDisable(GL_CULL_FACE)
        glDepthFunc(GL_LEQUAL)
    }

    private fun stop() {
        glDepthMask(true)
        glEnable(GL_CULL_FACE)
        glDepthFunc(GL_LESS)
    }

    private fun shouldRenderSkybox(scene: Scene): Boolean {
        return scene.renderSettings.viewportShadingMode == ViewportShadingSettings.RENDERED &&
            scene.environment.skyVisible &&
            scene.environment.skyboxEnabled
    }

    private fun renderSkybox(skybox: Skybox) {
        val model = skybox.model

        // identity model
        UniformUtils.setUniform(locationModel, Matrix4f())

        // bind cubemap to unit 0
        val tex = model.material.baseColorTexture
        glActiveTexture(GL_TEXTURE0)
        tex.bind()
        UniformUtils.setUniform(locationTexImage, 0)

        // draw
        glBindVertexArray(model.mesh.vao)
        glEnableVertexAttribArray(0)
        glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    private fun loadSkyColor(skyColor: Vector3f) {
        UniformUtils.setUniform(locationSkyColor, skyColor)
    }

    private fun loadCamera(camera: Camera) {
        val view = camera.getViewMatrix()
        // clear translation components of your Matrix4f (project's Matrix4f stores trans in m03/m13/m23)
        view.m03 = 0f
        view.m13 = 0f
        view.m23 = 0f
        UniformUtils.setUniform(locationView, view)
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    private companion object {
        val EDITOR_SKY_HIDDEN_COLOR = Vector3f(0.08f, 0.09f, 0.1f)
    }
}
