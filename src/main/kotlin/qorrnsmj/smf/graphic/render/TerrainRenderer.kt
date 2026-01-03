package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.terrain.*
import qorrnsmj.smf.game.terrain.component.BlendedTexture
import qorrnsmj.smf.game.terrain.component.SingleTexture
import qorrnsmj.smf.game.terrain.component.TerrainTextureMode
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.graphic.render.shader.TerrainShaderProgram
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
    val locationUseSingleTexture = glGetUniformLocation(program.id, "useSingleTexture")
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
        for (terrain in terrains) {
            bindTextures(terrain.model.material.textureMode)
            prepareTerrain(terrain)
            glDrawElements(GL_TRIANGLES, terrain.model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            unbindTextures()
        }
    }

    private fun prepareTerrain(terrain: Terrain) {
        val mesh = terrain.model.mesh
        glBindVertexArray(mesh.vao)

        val modelMatrix = MVP.getModelMatrix(
            position = terrain.position,
            rotation = terrain.rotation,
            scale = terrain.scale,
        )
        UniformUtils.setUniform(locationModel, modelMatrix)
    }

    private fun bindTextures(mode: TerrainTextureMode) {
        when (mode) {
            is SingleTexture -> {
                // Single texture mode: only bind one texture
                glUniform1i(locationUseSingleTexture, 1)

                glActiveTexture(GL_TEXTURE0)
                glBindTexture(GL_TEXTURE_2D, mode.baseTexture.id)
                glUniform1i(locationTexGrass, 0)
                // No need to bind other textures
            }
            is BlendedTexture -> {
                // Blended texture mode: bind blend map and 4 textures
                glUniform1i(locationUseSingleTexture, 0)

                glActiveTexture(GL_TEXTURE0)
                glBindTexture(GL_TEXTURE_2D, mode.blendMap.id)
                glUniform1i(locationBlendMap, 0)

                glActiveTexture(GL_TEXTURE1)
                glBindTexture(GL_TEXTURE_2D, mode.baseTexture.id)
                glUniform1i(locationTexGrass, 1)

                glActiveTexture(GL_TEXTURE2)
                glBindTexture(GL_TEXTURE_2D, mode.gTexture.id)
                glUniform1i(locationTexFlower, 2)

                glActiveTexture(GL_TEXTURE3)
                glBindTexture(GL_TEXTURE_2D, mode.rTexture.id)
                glUniform1i(locationTexDirt, 3)

                glActiveTexture(GL_TEXTURE4)
                glBindTexture(GL_TEXTURE_2D, mode.bTexture.id)
                glUniform1i(locationTexPath, 4)
            }
        }
    }

    private fun unbindTextures() {
        glBindVertexArray(0)

        // Unbind only active texture units
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
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
