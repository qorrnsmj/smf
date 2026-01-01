package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.terrain.Terrain
import qorrnsmj.smf.game.terrain.TerrainModel
import qorrnsmj.smf.game.terrain.TerrainModels
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.graphic.render.shader.TerrainShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.impl.Resizable
import qorrnsmj.smf.util.UniformUtils
import qorrnsmj.smf.util.impl.Cleanable

// TODO: 整理する
class TerrainRenderer : Resizable, Cleanable {
    // TODO: locationはシェーダークラスに書く
    val program = TerrainShaderProgram()
    val locationModel = glGetUniformLocation(program.id, "model")
    val locationView = glGetUniformLocation(program.id, "view")
    val locationProjection = glGetUniformLocation(program.id, "projection")
    val locationBlendMap = glGetUniformLocation(program.id, "blendMap")
    val locationTexGrass = glGetUniformLocation(program.id, "texGrass")
    val locationTexFlower = glGetUniformLocation(program.id, "texFlower")
    val locationTexDirt = glGetUniformLocation(program.id, "texDirt")
    val locationTexPath = glGetUniformLocation(program.id, "texPath")
    val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    val locationFogDensity = glGetUniformLocation(program.id, "fogDensity")
    val locationFogGradient = glGetUniformLocation(program.id, "fogGradient")

    fun start() {
        program.use()
    }

    fun stop() {
    }

    /* Render */

    fun renderTerrains(terrains: List<Terrain>) {
        UniformUtils.setUniform(locationModel, Matrix4f())

        // TODO
        for (terrain in terrains) {
            bindModel(TerrainModels.TERRAIN)
            prepareTerrain()
            glDrawElements(GL_TRIANGLES, TerrainModels.TERRAIN.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            unbindModel()
        }
    }

    private fun prepareTerrain() {
        val modelMatrix = MVP.getModelMatrix(
            position = Vector3f(-100f, 0f, -100f),
            rotation = Vector3f(0f, 0f, 0f),
            scale = Vector3f(1f, 1f, 1f)
        )
        UniformUtils.setUniform(locationModel, modelMatrix)
    }

    private fun bindModel(model: TerrainModel) {
        glBindVertexArray(model.mesh.vao)

        val material = model.material

        // BlendMap texture (unit 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, material.blendMap.id)
        glUniform1i(locationBlendMap, 0)

        // Grass texture (unit 1)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, material.grassTexture.id)
        glUniform1i(locationTexGrass, 1)

        // Flower texture (unit 2)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, material.flowerTexture.id)
        glUniform1i(locationTexFlower, 2)

        // Dirt texture (unit 3)
        glActiveTexture(GL_TEXTURE3)
        glBindTexture(GL_TEXTURE_2D, material.dirtTexture.id)
        glUniform1i(locationTexDirt, 3)

        // Path texture (unit 4)
        glActiveTexture(GL_TEXTURE4)
        glBindTexture(GL_TEXTURE_2D, material.pathTexture.id)
        glUniform1i(locationTexPath, 4)
    }

    private fun unbindModel() {
        glBindVertexArray(0)

        // Unbind all texture units
        for (i in 0..4) {
            glActiveTexture(GL_TEXTURE0 + i)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
    }

    /* Uniforms */

    fun loadCamera(camera: Camera) {
        UniformUtils.setUniform(locationView, camera.getViewMatrix())
    }

    fun loadSkyColor(skyColor: Vector3f) {
        UniformUtils.setUniform(locationSkyColor, skyColor)
    }

    fun loadFog(density: Float, gradient: Float) {
        UniformUtils.setUniform(locationFogDensity, density)
        UniformUtils.setUniform(locationFogGradient, gradient)
    }

    override fun resize(width: Int, height: Int) {
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    override fun cleanup() {
        glDeleteProgram(program.id)

        Logger.info("TerrainRenderer cleaned up!")
    }
}
