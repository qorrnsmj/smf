package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.light.SpotLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.shader.TerrainShaderProgram
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.terrain.component.SingleTexture
import qorrnsmj.smf.graphic.terrain.component.TerrainTextureMode
import qorrnsmj.smf.graphic.FogSettings
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class TerrainRenderer : SceneRenderer, Resizable {
    private companion object {
        const val MAX_LOCAL_LIGHTS = 30
        const val SHADOW_TEXTURE_UNIT = 5
        const val LOCAL_SHADOW_TEXTURE_UNIT = 6
        const val POINT_SHADOW_TEXTURE_UNIT_START = 7
    }

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
    val locationLightSpaceMatrix = glGetUniformLocation(program.id, "lightSpaceMatrix")
    val locationShadowMap = glGetUniformLocation(program.id, "shadowMap")
    val locationShadowEnabled = glGetUniformLocation(program.id, "shadowEnabled")
    val locationShadowStrength = glGetUniformLocation(program.id, "shadowStrength")
    val locationLightCount = glGetUniformLocation(program.id, "lightCount")
    val locationLocalShadowMap = glGetUniformLocation(program.id, "localShadowMap")
    val locationLocalShadowCount = glGetUniformLocation(program.id, "localShadowCount")
    val locationLocalShadowMatrices = glGetUniformLocation(program.id, "localShadowMatrices[0]")
    val locationLocalShadowStrengths = glGetUniformLocation(program.id, "localShadowStrengths[0]")
    val locationLocalLightShadowIndices = glGetUniformLocation(program.id, "localLightShadowIndices[0]")
    val locationPointShadowCount = glGetUniformLocation(program.id, "pointShadowCount")
    val locationPointShadowFarPlanes = glGetUniformLocation(program.id, "pointShadowFarPlanes[0]")
    val locationPointShadowStrengths = glGetUniformLocation(program.id, "pointShadowStrengths[0]")
    val locationPointLightShadowIndices = glGetUniformLocation(program.id, "pointLightShadowIndices[0]")
    val locationPointShadowMaps = IntArray(MAX_POINT_LIGHT_SHADOWS) { index ->
        glGetUniformLocation(program.id, "pointShadowMaps[$index]")
    }
    val locationSunLight = hashMapOf<String, Int>()
    val locationLights = mutableMapOf<Int, HashMap<String, Int>>()

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
        val terrain = scene.terrain ?: return

        start()
        loadCamera(scene.camera)
        loadSunLight(scene.sunLight)
        loadLights(scene.lights)
        loadSkyColor(scene.skyColor)
        loadFog(scene.fog)
        loadShadow(shadowState)
        renderTerrains(terrain)
        stop()
    }

    private fun start() {
        program.use()
    }

    private fun stop() {
    }

    private fun renderTerrains(terrain: Terrain) {
        bindTextures(terrain.model.material.textureMode)
        prepareTerrain(terrain)
        glDrawElements(GL_TRIANGLES, terrain.model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        unbindTextures()
    }

    private fun prepareTerrain(terrain: Terrain) {
        val mesh = terrain.model.mesh
        glBindVertexArray(mesh.vao)
        val modelMatrix = MVP.getModelMatrix(terrain.position, Quaternion.identity(), Vector3f(1f, 1f, 1f))
        UniformUtils.setUniform(locationModel, modelMatrix)
    }

    private fun bindTextures(mode: TerrainTextureMode) {
        when (mode) {
            is SingleTexture -> {
                glUniform1i(locationUseSingleTexture, 1)
                glActiveTexture(GL_TEXTURE0)
                glBindTexture(GL_TEXTURE_2D, mode.baseTexture.id)
                glUniform1i(locationTexGrass, 0)
            }
            is BlendedTexture -> {
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
        for (i in 0 until POINT_SHADOW_TEXTURE_UNIT_START) {
            glActiveTexture(GL_TEXTURE0 + i)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
        glActiveTexture(GL_TEXTURE0 + LOCAL_SHADOW_TEXTURE_UNIT)
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
        for (i in 0 until MAX_POINT_LIGHT_SHADOWS) {
            glActiveTexture(GL_TEXTURE0 + POINT_SHADOW_TEXTURE_UNIT_START + i)
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
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
