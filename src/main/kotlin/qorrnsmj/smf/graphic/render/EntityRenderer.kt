package qorrnsmj.smf.graphic.render

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.FogSettings
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.ViewportShadingMode
import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.light.SpotLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.shader.EntityShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils.setUniform

class EntityRenderer : SceneRenderer, Resizable {
    private companion object {
        const val MAX_POINT_LIGHTS = 30
        const val SHADOW_TEXTURE_UNIT = 5
        const val LOCAL_SHADOW_TEXTURE_UNIT = 6
        const val POINT_SHADOW_TEXTURE_UNIT_START = 7
        val SOLID_VIEW_COLOR = Vector4f(0.52f, 0.54f, 0.56f, 1f)
    }

    val program = EntityShaderProgram()
    val locationModel = glGetUniformLocation(program.id, "model")
    val locationView = glGetUniformLocation(program.id, "view")
    val locationProjection = glGetUniformLocation(program.id, "projection")
    val locationFakeLighting = glGetUniformLocation(program.id, "fakeLighting")
    val locationGrayView = glGetUniformLocation(program.id, "grayView")
    val locationGrayColor = glGetUniformLocation(program.id, "grayColor")
    val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    val locationCameraPosition = glGetUniformLocation(program.id, "cameraPosition")
    val locationLightCount = glGetUniformLocation(program.id, "lightCount")
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
    val locationMaterials = hashMapOf<String, Int>()

    init {
        locationSunLight["direction"] = glGetUniformLocation(program.id, "sunLight.direction")
        locationSunLight["color"] = glGetUniformLocation(program.id, "sunLight.color")
        locationSunLight["intensity"] = glGetUniformLocation(program.id, "sunLight.intensity")
        locationSunLight["ambientColor"] = glGetUniformLocation(program.id, "sunLight.ambientColor")
        locationSunLight["ambientIntensity"] = glGetUniformLocation(program.id, "sunLight.ambientIntensity")

        for (i in 0 until MAX_POINT_LIGHTS) {
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

        locationMaterials["baseColorFactor"] = glGetUniformLocation(program.id, "material.baseColorFactor")
        locationMaterials["emissiveFactor"] = glGetUniformLocation(program.id, "material.emissiveFactor")
        locationMaterials["metallicFactor"] = glGetUniformLocation(program.id, "material.metallicFactor")
        locationMaterials["roughnessFactor"] = glGetUniformLocation(program.id, "material.roughnessFactor")
        locationMaterials["baseColorTexture"] = glGetUniformLocation(program.id, "material.baseColorTexture")
        locationMaterials["metallicRoughnessTexture"] = glGetUniformLocation(program.id, "material.metallicRoughnessTexture")
        locationMaterials["normalTexture"] = glGetUniformLocation(program.id, "material.normalTexture")
        locationMaterials["occlusionTexture"] = glGetUniformLocation(program.id, "material.occlusionTexture")
        locationMaterials["emissiveTexture"] = glGetUniformLocation(program.id, "material.emissiveTexture")
        locationMaterials["normalScale"] = glGetUniformLocation(program.id, "material.normalScale")
        locationMaterials["occlusionStrength"] = glGetUniformLocation(program.id, "material.occlusionStrength")
        locationMaterials["alphaMode"] = glGetUniformLocation(program.id, "material.alphaMode")
        locationMaterials["alphaCutoff"] = glGetUniformLocation(program.id, "material.alphaCutoff")
        locationMaterials["doubleSided"] = glGetUniformLocation(program.id, "material.doubleSided")
    }

    override fun render(scene: Scene) {
        render(scene, ShadowRenderState(false, Matrix4f(), 0))
    }

    fun render(scene: Scene, shadowState: ShadowRenderState) {
        start()
        loadCamera(scene.camera)
        loadSunLight(scene.sunLight)
        loadLights(scene.lights)
        loadSkyColor(scene.skyColor)
        loadFog(scene.fog)
        loadShadow(shadowState)
        loadViewportShading(scene.viewportShadingMode)
        renderEntities(scene.camera, scene.entities)
        stop()
    }

    private fun start() {
        program.use()
    }

    private fun stop() {
    }

    private fun renderEntities(camera: Camera, entities: List<Entity>) {
        val batchMap = mutableMapOf<Model, MutableList<Entity>>()
        for (entity in entities) processEntity(entity, batchMap)

        renderOpaqueAndMask(batchMap)
        renderBlend(camera, batchMap)

        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    private fun renderOpaqueAndMask(batchMap: Map<Model, MutableList<Entity>>) {
        glDepthMask(true)
        glDisable(GL_BLEND)

        for ((model, targets) in batchMap) {
            if (model.material.alphaMode == AlphaMode.BLEND) continue

            bindModel(model)
            for (target in targets) {
                prepareEntity(target)
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, model.mesh.vertexType, 0)
            }
            unbindModel()
        }
    }

    private fun renderBlend(camera: Camera, batchMap: Map<Model, MutableList<Entity>>) {
        glDepthMask(false)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        data class DrawItem(val model: Model, val entity: Entity, val distanceSq: Float)
        val blendItems = mutableListOf<DrawItem>()
        for ((model, targets) in batchMap) {
            if (model.material.alphaMode != AlphaMode.BLEND) continue
            for (entity in targets) {
                val worldPos = entity.worldTransform.position
                val dx = worldPos.x - camera.position.x
                val dy = worldPos.y - camera.position.y
                val dz = worldPos.z - camera.position.z
                blendItems.add(DrawItem(model, entity, dx * dx + dy * dy + dz * dz))
            }
        }
        blendItems.sortByDescending { it.distanceSq }

        var boundModel: Model? = null
        for (item in blendItems) {
            if (boundModel != item.model) {
                unbindModel()
                bindModel(item.model)
                boundModel = item.model
            }
            prepareEntity(item.entity)
            glDrawElements(GL_TRIANGLES, item.model.mesh.vertexCount, item.model.mesh.vertexType, 0)
        }
        unbindModel()
    }

    private fun processEntity(entity: Entity, batchMap: MutableMap<Model, MutableList<Entity>>) {
        if (entity.model != EntityModels.EMPTY) {
            batchMap.getOrPut(entity.model) { mutableListOf() }.add(entity)
        }
        for (child in entity.children) {
            processEntity(child, batchMap)
        }
    }

    private fun prepareEntity(entity: Entity) {
        val world = entity.worldTransform
        setUniform(locationModel, MVP.getModelMatrix(world.position, world.rotation, world.scale))
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vao)
        setUniform(locationFakeLighting, if (model.fakeLighting) 1 else 0)
        val material = model.material

        setUniform(locationMaterials["baseColorFactor"]!!, material.baseColorFactor)
        setUniform(locationMaterials["emissiveFactor"]!!, material.emissiveFactor)
        setUniform(locationMaterials["metallicFactor"]!!, material.metallicFactor)
        setUniform(locationMaterials["roughnessFactor"]!!, material.roughnessFactor)
        setUniform(locationMaterials["baseColorTexture"]!!, material.baseColorTexture.id, 0)
        setUniform(locationMaterials["metallicRoughnessTexture"]!!, material.metallicRoughnessTexture.id, 1)
        setUniform(locationMaterials["normalTexture"]!!, material.normalTexture.id, 2)
        setUniform(locationMaterials["occlusionTexture"]!!, material.occlusionTexture.id, 3)
        setUniform(locationMaterials["emissiveTexture"]!!, material.emissiveTexture.id, 4)
        setUniform(locationMaterials["normalScale"]!!, material.normalScale)
        setUniform(locationMaterials["occlusionStrength"]!!, material.occlusionStrength)
        setUniform(locationMaterials["alphaMode"]!!, material.alphaMode.ordinal)
        setUniform(locationMaterials["alphaCutoff"]!!, material.alphaCutoff)
        if (material.doubleSided) glDisable(GL_CULL_FACE)
    }

    private fun unbindModel() {
        glBindVertexArray(0)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
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
        setUniform(locationView, camera.getViewMatrix())
        setUniform(locationCameraPosition, camera.position)
    }

    private fun loadLights(lights: List<Light>) {
        val visibleLights = lights.take(MAX_POINT_LIGHTS)
        glUniform1i(locationLightCount, visibleLights.size)

        visibleLights.forEachIndexed { index, light ->
            val locations = locationLights[index]!!
            setUniform(locations["position"]!!, light.position)
            setUniform(locations["color"]!!, light.diffuse)
            setUniform(locations["intensity"]!!, light.intensity)
            setUniform(locations["constant"]!!, light.constant)
            setUniform(locations["linear"]!!, light.linear)
            setUniform(locations["quadratic"]!!, light.quadratic)
            if (light is SpotLight) {
                setUniform(locations["type"]!!, 1)
                setUniform(locations["direction"]!!, light.direction)
                setUniform(locations["innerCutOff"]!!, light.innerCutOff)
                setUniform(locations["outerCutOff"]!!, light.outerCutOff)
            } else {
                setUniform(locations["type"]!!, 0)
                setUniform(locations["direction"]!!, Vector3f(0f, -1f, 0f))
                setUniform(locations["innerCutOff"]!!, 1f)
                setUniform(locations["outerCutOff"]!!, 0f)
            }
        }
    }

    private fun loadSunLight(sunLight: DirectionalLight?) {
        val sun = sunLight ?: SunLight(intensity = 0f, ambientIntensity = 0.08f)
        setUniform(locationSunLight["direction"]!!, sun.direction)
        setUniform(locationSunLight["color"]!!, sun.color)
        setUniform(locationSunLight["intensity"]!!, sun.intensity)
        setUniform(locationSunLight["ambientColor"]!!, sun.ambientColor)
        setUniform(locationSunLight["ambientIntensity"]!!, sun.ambientIntensity)
    }

    private fun loadSkyColor(skyColor: Vector3f) {
        setUniform(locationSkyColor, skyColor)
    }

    private fun loadViewportShading(mode: ViewportShadingMode) {
        val grayView = mode == ViewportShadingMode.SOLID || mode == ViewportShadingMode.WIRE
        setUniform(locationGrayView, if (grayView) 1 else 0)
        setUniform(locationGrayColor, SOLID_VIEW_COLOR)
    }

    private fun loadFog(fog: FogSettings) {
        setUniform(locationFogEnabled, if (fog.enabled) 1 else 0)
        setUniform(locationFogColor, fog.color)
        setUniform(locationFogDistanceDensity, fog.distanceDensity)
        setUniform(locationFogDistanceGradient, fog.distanceGradient)
        setUniform(locationFogHeightDensity, fog.heightDensity)
        setUniform(locationFogBottomY, fog.bottomY)
        setUniform(locationFogTopY, fog.topY)
        setUniform(locationFogHeightFalloff, fog.heightFalloff)
    }

    private fun loadShadow(shadowState: ShadowRenderState) {
        setUniform(locationLightSpaceMatrix, shadowState.lightSpaceMatrix)
        setUniform(locationShadowEnabled, if (shadowState.enabled) 1 else 0)
        setUniform(locationShadowStrength, shadowState.strength)
        if (shadowState.enabled) {
            setUniform(locationShadowMap, shadowState.depthTextureId, SHADOW_TEXTURE_UNIT)
        }

        val local = shadowState.local
        setUniform(locationLocalShadowCount, local.count)
        setUniform(locationLocalLightShadowIndices, local.lightShadowIndices.copyOf(MAX_LOCAL_LIGHT_SHADOWS))
        setUniform(locationLocalShadowStrengths, local.strengths.padded(MAX_LOCAL_LIGHT_SHADOWS))
        setUniform(locationLocalShadowMatrices, local.matrices.paddedMatrices(MAX_LOCAL_LIGHT_SHADOWS))
        if (local.enabled) {
            glActiveTexture(GL_TEXTURE0 + LOCAL_SHADOW_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_2D_ARRAY, local.depthTextureId)
            glUniform1i(locationLocalShadowMap, LOCAL_SHADOW_TEXTURE_UNIT)
        }

        val point = shadowState.point
        setUniform(locationPointShadowCount, point.count)
        setUniform(locationPointShadowFarPlanes, point.farPlanes.padded(MAX_POINT_LIGHT_SHADOWS))
        setUniform(locationPointLightShadowIndices, point.lightShadowIndices.copyOf(MAX_LOCAL_LIGHT_SHADOWS))
        setUniform(locationPointShadowStrengths, point.strengths.padded(MAX_POINT_LIGHT_SHADOWS))
        for (index in 0 until MAX_POINT_LIGHT_SHADOWS) {
            val unit = POINT_SHADOW_TEXTURE_UNIT_START + index
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(GL_TEXTURE_CUBE_MAP, point.textureIds.getOrElse(index) { 0 })
            glUniform1i(locationPointShadowMaps[index], unit)
        }
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    private fun FloatArray.padded(size: Int): FloatArray = FloatArray(size) { index -> getOrElse(index) { 0f } }

    private fun List<Matrix4f>.paddedMatrices(size: Int): List<Matrix4f> = List(size) { index -> getOrElse(index) { Matrix4f() } }
}
