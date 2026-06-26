package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.map.GameMap
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.render.shader.MapShaderProgram
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class MapRenderer : SceneRenderer, Resizable {
    private companion object {
        const val FOG_DENSITY = 0.00045f
        const val FOG_GRADIENT = 1.5f
    }

    private val program = MapShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationMapTexture = glGetUniformLocation(program.id, "mapTexture")
    private val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    private val locationCameraPosition = glGetUniformLocation(program.id, "cameraPosition")
    private val locationFogDensity = glGetUniformLocation(program.id, "fogDensity")
    private val locationFogGradient = glGetUniformLocation(program.id, "fogGradient")
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
        loadFog(FOG_DENSITY, FOG_GRADIENT)
        loadShadow(shadowState)
        renderMap(gameMap)
    }

    private fun renderMap(gameMap: GameMap) {
        UniformUtils.setUniform(
            locationModel,
            MVP.getModelMatrix(
                position = Vector3f(),
                rotation = Vector3f(),
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

    private fun loadFog(density: Float, gradient: Float) {
        UniformUtils.setUniform(locationFogDensity, density)
        UniformUtils.setUniform(locationFogGradient, gradient)
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
