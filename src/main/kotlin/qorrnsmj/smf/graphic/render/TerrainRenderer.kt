package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.terrain.*
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.terrain.component.SingleTexture
import qorrnsmj.smf.graphic.terrain.component.TerrainTextureMode
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.graphic.FogSettings
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.render.shader.TerrainShaderProgram
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

// TODO: 整理する
class TerrainRenderer : SceneRenderer, Resizable {
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
    val locationCameraPosition = glGetUniformLocation(program.id, "cameraPosition")
    val locationFogEnabled = glGetUniformLocation(program.id, "fog.enabled")
    val locationFogColor = glGetUniformLocation(program.id, "fog.color")
    val locationFogDistanceDensity = glGetUniformLocation(program.id, "fog.distanceDensity")
    val locationFogDistanceGradient = glGetUniformLocation(program.id, "fog.distanceGradient")
    val locationFogHeightDensity = glGetUniformLocation(program.id, "fog.heightDensity")
    val locationFogBottomY = glGetUniformLocation(program.id, "fog.bottomY")
    val locationFogTopY = glGetUniformLocation(program.id, "fog.topY")
    val locationFogHeightFalloff = glGetUniformLocation(program.id, "fog.heightFalloff")

    override fun render(scene: Scene) {
        val terrain = scene.terrain ?: return

        start()
        loadCamera(scene.camera)
        loadSkyColor(scene.skyColor)
        loadFog(scene.fog)
        renderTerrains(terrain)
        stop()
    }

    private fun start() {
        program.use()
    }

    private fun stop() {
    }

    /* Render */

    private fun renderTerrains(terrain: Terrain) {
        bindTextures(terrain.model.material.textureMode)
        prepareTerrain(terrain)
        glDrawElements(GL_TRIANGLES, terrain.model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        unbindTextures()
    }

    private fun prepareTerrain(terrain: Terrain) {
        val mesh = terrain.model.mesh
        glBindVertexArray(mesh.vao)

        val modelMatrix = MVP.getModelMatrix(
            position = terrain.position,
            rotation = Vector3f(0f, 0f, 0f),
            scale = Vector3f(1f, 1f, 1f)
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

    private fun loadCamera(camera: Camera) {
        UniformUtils.setUniform(locationView, camera.getViewMatrix())
        UniformUtils.setUniform(locationCameraPosition, camera.position)
    }

    private fun loadSkyColor(skyColor: Vector3f) {
        UniformUtils.setUniform(locationSkyColor, skyColor)
    }

    private fun loadFog(fog: FogSettings) {
        UniformUtils.setUniform(locationFogEnabled, if (fog.enabled) 1 else 0)
        UniformUtils.setUniform(locationFogColor, fog.color)
        UniformUtils.setUniform(locationFogDistanceDensity, fog.distanceDensity)
        UniformUtils.setUniform(locationFogDistanceGradient, fog.distanceGradient)
        UniformUtils.setUniform(locationFogHeightDensity, fog.heightDensity)
        UniformUtils.setUniform(locationFogBottomY, fog.bottomY)
        UniformUtils.setUniform(locationFogTopY, fog.topY)
        UniformUtils.setUniform(locationFogHeightFalloff, fog.heightFalloff)
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }
}
