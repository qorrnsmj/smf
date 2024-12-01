package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11C.glBindTexture
import org.lwjgl.opengl.GL11C.glDrawElements
import org.lwjgl.opengl.GL13C.GL_TEXTURE0
import org.lwjgl.opengl.GL13C.glActiveTexture
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.game.terrain.Terrain
import qorrnsmj.smf.game.terrain.TerrainModels
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.graphic.shader.custom.TerrainShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

// TODO: 整理する
class TerrainRenderer : Resizable {
    val program = TerrainShaderProgram()
    val locationModel = glGetUniformLocation(program.id, "model")
    val locationView = glGetUniformLocation(program.id, "view")
    val locationProjection = glGetUniformLocation(program.id, "projection")
    val locationTexImage = glGetUniformLocation(program.id, "texImage")
    val locationSkyColor = glGetUniformLocation(program.id, "skyColor")

    fun start() {
        glUseProgram(program.id)
    }

    fun stop() {
        glUseProgram(0)
    }

    /* Render */

    fun renderTerrains(terrains: List<Terrain>) {
        UniformUtils.setUniform(locationModel, Matrix4f())

        bindModel(TerrainModels.TERRAIN)
        prepareTerrain()
        glDrawElements(GL_TRIANGLES, TerrainModels.TERRAIN.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        unbindModel()
    }

    private fun prepareTerrain() {
        val modelMatrix = MVP.getModelMatrix(
            position = Vector3f(-100f, 0f, -100f),
            rotation = Vector3f(0f, 0f, 0f),
            scale = Vector3f(1f, 1f, 1f)
        )
        UniformUtils.setUniform(locationModel, modelMatrix)
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vaoID)

        val material = model.material

        // Diffuse texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, material.diffuseTexture.id)
        glUniform1i(locationTexImage, 0)

        // Specular texture
        /*glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, material.specularTexture.id)
        glUniform1i(locationMaterials["specularTexture"]!!, 1)*/

        // Normal texture
        /*glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, material.normalTexture.id)
        glUniform1i(locationMaterials["normalTexture"]!!, 2)*/

        // Material
        /*UniformUtils.setUniform(locationMaterials["diffuseColor"]!!, material.diffuseColor)
        UniformUtils.setUniform(locationMaterials["ambientColor"]!!, material.ambientColor)
        UniformUtils.setUniform(locationMaterials["specularColor"]!!, material.specularColor)
        UniformUtils.setUniform(locationMaterials["emissiveColor"]!!, material.emissiveColor)
        UniformUtils.setUniform(locationMaterials["shininess"]!!, material.shininess)
        UniformUtils.setUniform(locationMaterials["opacity"]!!, material.opacity)*/
    }

    private fun unbindModel() {
        glBindVertexArray(0)

        /*glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, 0)*/
    }

    /* Uniforms */

    fun loadCamera(camera: Camera) {
        UniformUtils.setUniform(locationView, camera.getViewMatrix())
    }

    fun loadSkyColor(skyColor: Vector3f) {
        UniformUtils.setUniform(locationSkyColor, skyColor)
    }

    override fun resize(width: Int, height: Int) {
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }
}
