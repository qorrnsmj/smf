package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.map.GameMap
import qorrnsmj.smf.graphic.FogSettings
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.light.SpotLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.shader.MapShaderProgram
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class MapRenderer : SceneRenderer, Resizable {
    private companion object {
        const val MAX_LOCAL_LIGHTS = 30
        const val SHADOW_TEXTURE_UNIT = 1
        const val LOCAL_SHADOW_TEXTURE_UNIT = 2
        const val POINT_SHADOW_TEXTURE_UNIT_START = 3
    }

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
    private val locationShadowStrength = glGetUniformLocation(program.id, "shadowStrength")
    private val locationLightCount = glGetUniformLocation(program.id, "lightCount")
    private val locationLocalShadowMap = glGetUniformLocation(program.id, "localShadowMap")
    private val locationLocalShadowCount = glGetUniformLocation(program.id, "localShadowCount")
    private val locationLocalShadowMatrices = glGetUniformLocation(program.id, "localShadowMatrices[0]")
    private val locationLocalShadowStrengths = glGetUniformLocation(program.id, "localShadowStrengths[0]")
    private val locationLocalLightShadowIndices = glGetUniformLocation(program.id, "localLightShadowIndices[0]")
    private val locationPointShadowCount = glGetUniformLocation(program.id, "pointShadowCount")
    private val locationPointShadowFarPlanes = glGetUniformLocation(program.id, "pointShadowFarPlanes[0]")
    private val locationPointShadowStrengths = glGetUniformLocation(program.id, "pointShadowStrengths[0]")
    private val locationPointLightShadowIndices = glGetUniformLocation(program.id, "pointLightShadowIndices[0]")
    private val locationPointShadowMaps = IntArray(MAX_POINT_LIGHT_SHADOWS) { index ->
        glGetUniformLocation(program.id, "pointShadowMaps[$index]")
    }
    private val locationSunLight = hashMapOf<String, Int>()
    private val locationLights = mutableMapOf<Int, HashMap<String, Int>>()

    init {
        locationSunLight["direction"] = glGetUniformLocation(program.id, "sunLight.direction")
        locationSunLight["color"] = glGetUniformLocation(program.id, "sunLight.color")
        locationSunLight["intensity"] = glGetUniformLocation(program.id, "sunLight.intensity")
        locationSunLight["ambientColor"] = glGetUniformLocation(program.id, "sunLight.ambientColor")
        locationSunLight["ambientIntensity"] = glGetUniformLocation(program.id, "sunLight.ambientIntensity")

        for (i in 0 until MAX_LOCAL_LIGHTS) {
            locationLights[i] = hashMapOf(
                "position" to glGetUniformLocation(program.id, "lights[$i].position"),
                "color" to glGetUniformLocation(program.id, "lights[$i].color"),
                "intensity" to glGetUniformLocation(program.id, "lights[$i].intensity"),
                "constant" to glGetUniformLocation(program.id, "lights[$i].constant"),
                "linear" to glGetUniformLocation(program.id, "lights[$i].linear"),
                "quadratic" to glGetUniformLocation(program.id, "lights[$i].quadratic"),
                "type" to glGetUniformLocation(program.id, "lights[$i].type"),
                "direction" to glGetUniformLocation(program.id, "lights[$i].direction"),
                "innerCutOff" to glGetUniformLocation(program.id, "lights[$i].innerCutOff"),
                "outerCutOff" to glGetUniformLocation(program.id, "lights[$i].outerCutOff"),
            )
        }
    }

    override fun render(scene: Scene) {
        render(scene, ShadowRenderState(false, Matrix4f(), 0))
    }

    fun render(scene: Scene, shadowState: ShadowRenderState) {
        val gameMap = scene.map ?: return

        program.use()
        loadCamera(scene.camera)
        loadSunLight(scene.sunLight)
        loadLights(scene.lights)
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
        glActiveTexture(GL_TEXTURE0 + LOCAL_SHADOW_TEXTURE_UNIT)
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
        for (i in 0 until MAX_POINT_LIGHT_SHADOWS) {
            glActiveTexture(GL_TEXTURE0 + POINT_SHADOW_TEXTURE_UNIT_START + i)
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
        }
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

    private fun loadSunLight(sunLight: DirectionalLight?) {
        val sun = sunLight ?: SunLight(intensity = 0f, ambientIntensity = 0.08f)
        UniformUtils.setUniform(locationSunLight["direction"]!!, sun.direction)
        UniformUtils.setUniform(locationSunLight["color"]!!, sun.color)
        UniformUtils.setUniform(locationSunLight["intensity"]!!, sun.intensity)
        UniformUtils.setUniform(locationSunLight["ambientColor"]!!, sun.ambientColor)
        UniformUtils.setUniform(locationSunLight["ambientIntensity"]!!, sun.ambientIntensity)
    }

    private fun loadLights(lights: List<Light>) {
        val visibleLights = lights.take(MAX_LOCAL_LIGHTS)
        UniformUtils.setUniform(locationLightCount, visibleLights.size)
        visibleLights.forEachIndexed { index, light ->
            val locations = locationLights[index]!!
            UniformUtils.setUniform(locations["position"]!!, light.position)
            UniformUtils.setUniform(locations["color"]!!, light.diffuse)
            UniformUtils.setUniform(locations["intensity"]!!, light.intensity)
            UniformUtils.setUniform(locations["constant"]!!, light.constant)
            UniformUtils.setUniform(locations["linear"]!!, light.linear)
            UniformUtils.setUniform(locations["quadratic"]!!, light.quadratic)
            if (light is SpotLight) {
                UniformUtils.setUniform(locations["type"]!!, 1)
                UniformUtils.setUniform(locations["direction"]!!, light.direction)
                UniformUtils.setUniform(locations["innerCutOff"]!!, light.innerCutOff)
                UniformUtils.setUniform(locations["outerCutOff"]!!, light.outerCutOff)
            } else {
                UniformUtils.setUniform(locations["type"]!!, 0)
                UniformUtils.setUniform(locations["direction"]!!, Vector3f(0f, -1f, 0f))
                UniformUtils.setUniform(locations["innerCutOff"]!!, 1f)
                UniformUtils.setUniform(locations["outerCutOff"]!!, 0f)
            }
        }
    }

    private fun loadShadow(shadowState: ShadowRenderState) {
        UniformUtils.setUniform(locationLightSpaceMatrix, shadowState.lightSpaceMatrix)
        UniformUtils.setUniform(locationShadowEnabled, if (shadowState.enabled) 1 else 0)
        UniformUtils.setUniform(locationShadowStrength, shadowState.strength)
        if (shadowState.enabled) {
            glActiveTexture(GL_TEXTURE0 + SHADOW_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_2D, shadowState.depthTextureId)
            glUniform1i(locationShadowMap, SHADOW_TEXTURE_UNIT)
        }

        val local = shadowState.local
        UniformUtils.setUniform(locationLocalShadowCount, local.count)
        UniformUtils.setUniform(locationLocalLightShadowIndices, local.lightShadowIndices.copyOf(MAX_LOCAL_LIGHT_SHADOWS))
        UniformUtils.setUniform(locationLocalShadowStrengths, local.strengths.padded(MAX_LOCAL_LIGHT_SHADOWS))
        UniformUtils.setUniform(locationLocalShadowMatrices, local.matrices.paddedMatrices(MAX_LOCAL_LIGHT_SHADOWS))
        if (local.enabled) {
            glActiveTexture(GL_TEXTURE0 + LOCAL_SHADOW_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_2D_ARRAY, local.depthTextureId)
            glUniform1i(locationLocalShadowMap, LOCAL_SHADOW_TEXTURE_UNIT)
        }

        val point = shadowState.point
        UniformUtils.setUniform(locationPointShadowCount, point.count)
        UniformUtils.setUniform(locationPointShadowFarPlanes, point.farPlanes.padded(MAX_POINT_LIGHT_SHADOWS))
        UniformUtils.setUniform(locationPointLightShadowIndices, point.lightShadowIndices.copyOf(MAX_LOCAL_LIGHT_SHADOWS))
        UniformUtils.setUniform(locationPointShadowStrengths, point.strengths.padded(MAX_POINT_LIGHT_SHADOWS))
        for (index in 0 until MAX_POINT_LIGHT_SHADOWS) {
            val unit = POINT_SHADOW_TEXTURE_UNIT_START + index
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(GL_TEXTURE_CUBE_MAP, point.textureIds.getOrElse(index) { 0 })
            glUniform1i(locationPointShadowMaps[index], unit)
        }
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    private fun FloatArray.padded(size: Int): FloatArray = FloatArray(size) { index -> getOrElse(index) { 0f } }

    private fun List<Matrix4f>.paddedMatrices(size: Int): List<Matrix4f> = List(size) { index -> getOrElse(index) { Matrix4f() } }
}
