package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.map.GameMap
import qorrnsmj.smf.graphic.FogSettings
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.render.shader.MapShaderProgram
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class MapRenderer : SceneRenderer, Resizable {
    private val program = MapShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationMapTexture = glGetUniformLocation(program.id, "mapTexture")
    private val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    private val locationCameraPosition = glGetUniformLocation(program.id, "cameraPosition")
    private val locationFogEnabled = glGetUniformLocation(program.id, "fog.enabled")
    private val locationFogColor = glGetUniformLocation(program.id, "fog.color")
    private val locationFogDistanceDensity = glGetUniformLocation(program.id, "fog.distanceDensity")
    private val locationFogDistanceGradient = glGetUniformLocation(program.id, "fog.distanceGradient")
    private val locationFogHeightDensity = glGetUniformLocation(program.id, "fog.heightDensity")
    private val locationFogBottomY = glGetUniformLocation(program.id, "fog.bottomY")
    private val locationFogTopY = glGetUniformLocation(program.id, "fog.topY")
    private val locationFogHeightFalloff = glGetUniformLocation(program.id, "fog.heightFalloff")
    private val locationLightSpaceMatrix = glGetUniformLocation(program.id, "lightSpaceMatrix")
    private val locationShadowMap = glGetUniformLocation(program.id, "shadowMap")
    private val locationShadowEnabled = glGetUniformLocation(program.id, "shadowEnabled")

    override fun render(scene: Scene) {
        render(scene, ShadowRenderState(false, qorrnsmj.smf.math.Matrix4f(), 0))
    }

    fun render(scene: Scene, shadowState: ShadowRenderState) {
        val gameMap = scene.map ?: return

        program.use()
        loadCamera(scene.camera)
        loadSkyColor(scene.skyColor)
        loadFog(scene.fog)
        loadShadow(shadowState)
        renderMap(gameMap)
    }

    private fun renderMap(gameMap: GameMap) {
        UniformUtils.setUniform(
            locationModel,
            MVP.getModelMatrix(
                position = Vector3f(),
                rotation = Quaternion.identity(),
                scale = Vector3f(1f, 1f, 1f),
            )
        )

        glActiveTexture(GL_TEXTURE0)
        glUniform1i(locationMapTexture, 0)

        for ((textureName, mesh) in gameMap.meshesByTexture) {
            glBindTexture(GL_TEXTURE_2D, textureId(textureName))
            glBindVertexArray(mesh.vao)
            glDrawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0)
        }

        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun textureId(textureName: String): Int {
        return when (textureName.lowercase()) {
            "dirt" -> Textures.TERRAIN_DIRT.id
            "path" -> Textures.TERRAIN_PATH.id
            "flower" -> Textures.TERRAIN_FLOWER.id
            "grass" -> Textures.TERRAIN_GRASS.id
            else -> Textures.TERRAIN_GRASS.id
        }
    }

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

    private fun loadShadow(shadowState: ShadowRenderState) {
        UniformUtils.setUniform(locationLightSpaceMatrix, shadowState.lightSpaceMatrix)
        UniformUtils.setUniform(locationShadowEnabled, if (shadowState.enabled) 1 else 0)
        if (shadowState.enabled) {
            glActiveTexture(GL_TEXTURE1)
            glBindTexture(GL_TEXTURE_2D, shadowState.depthTextureId)
            glUniform1i(locationShadowMap, 1)
        }
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }
}
