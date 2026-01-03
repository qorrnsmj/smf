package qorrnsmj.smf.graphic.render

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.model.component.Model
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.graphic.render.shader.EntityShaderProgram
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.impl.Resizable
import qorrnsmj.smf.util.UniformUtils.setUniform
import qorrnsmj.smf.util.impl.Cleanable

class EntityRenderer : Resizable, Cleanable {
    // TODO: locationはシェーダークラスに書く?
    val program = EntityShaderProgram()
    val locationModel = glGetUniformLocation(program.id, "model")
    val locationView = glGetUniformLocation(program.id, "view")
    val locationProjection = glGetUniformLocation(program.id, "projection")
    val locationFakeLighting = glGetUniformLocation(program.id, "fakeLighting")
    val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    val locationCameraPosition = glGetUniformLocation(program.id, "cameraPosition")
    val locationLightCount = glGetUniformLocation(program.id, "lightCount")
    val locationFogDensity = glGetUniformLocation(program.id, "fogDensity")
    val locationFogGradient = glGetUniformLocation(program.id, "fogGradient")
    val locationLights = mutableMapOf<Int, HashMap<String, Int>>()
    val locationMaterials = hashMapOf<String, Int>()

    init {
        for (i in 0..30) {
            locationLights[i] = hashMapOf(
                "position" to glGetUniformLocation(program.id, "lights[$i].position"),
                "color" to glGetUniformLocation(program.id, "lights[$i].color"),
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

    fun start() {
        program.use()
    }

    fun stop() {
    }

    /* Render */

    // TODO: テクスチャキャッシング - 同じテクスチャは再bind しない
    fun renderEntity(camera: Camera, entities: List<Entity>) {
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
            if (model.material.alphaMode == AlphaMode.BLEND)
                continue

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

        // sort by distance from camera
        data class DrawItem(val model: Model, val entity: Entity, val distanceSq: Float)
        val blendItems = mutableListOf<DrawItem>()
        for ((model, targets) in batchMap) {
            if (model.material.alphaMode != AlphaMode.BLEND) continue

            for (entity in targets) {
                // TODO: エンティティとの距離はUtilクラスに
                val dx = entity.position.x - camera.position.x
                val dy = entity.position.y - camera.position.y
                val dz = entity.position.z - camera.position.z
                val distSq = dx * dx + dy * dy + dz * dz

                blendItems.add(DrawItem(model, entity, distSq))
            }
        }
        blendItems.sortByDescending { it.distanceSq }

        // render
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

    // TODO: 親のpos, rot, scaleを子にも適用 -> そもそもmodel-matrix共有しとけば？ (同じfbxのモデルならわかるけど、後から別モデルをchildren登録したらどうなる？)
    private fun processEntity(entity: Entity, batchMap: MutableMap<Model, MutableList<Entity>>) {
        // parent model
        if (entity.model != EntityModels.EMPTY) {
            val batch = batchMap.getOrPut(entity.model) { mutableListOf() }
            batch.add(entity)
        }

        // children model
        for (child in entity.children) {
            processEntity(child, batchMap)
        }
    }

    private fun prepareEntity(entity: Entity) {
        setUniform(locationModel, MVP.getModelMatrix(entity.position, entity.rotation, entity.scale))
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vao)
        setUniform(locationFakeLighting, if (model.fakeLighting) 1 else 0)
        val m = model.material

        // factors
        setUniform(locationMaterials["baseColorFactor"]!!, m.baseColorFactor)
        setUniform(locationMaterials["emissiveFactor"]!!, m.emissiveFactor)
        setUniform(locationMaterials["metallicFactor"]!!, m.metallicFactor)
        setUniform(locationMaterials["roughnessFactor"]!!, m.roughnessFactor)

        // textures
        setUniform(locationMaterials["baseColorTexture"]!!, m.baseColorTexture.id, 0)
        setUniform(locationMaterials["metallicRoughnessTexture"]!!, m.metallicRoughnessTexture.id, 1)
        setUniform(locationMaterials["normalTexture"]!!, m.normalTexture.id, 2)
        setUniform(locationMaterials["occlusionTexture"]!!, m.occlusionTexture.id, 3)
        setUniform(locationMaterials["emissiveTexture"]!!, m.emissiveTexture.id, 4)

        // texture params
        setUniform(locationMaterials["normalScale"]!!, m.normalScale)
        setUniform(locationMaterials["occlusionStrength"]!!, m.occlusionStrength)

        // render states
        setUniform(locationMaterials["alphaMode"]!!, m.alphaMode.ordinal)
        setUniform(locationMaterials["alphaCutoff"]!!, m.alphaCutoff)
        if (m.doubleSided) glDisable(GL_CULL_FACE)
    }

    private fun unbindModel() {
        glBindVertexArray(0)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        for (i in 0..4) {
            glActiveTexture(GL_TEXTURE0 + i)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
    }

    /* Uniforms */

    fun loadCamera(camera: Camera) {
        setUniform(locationView, camera.getViewMatrix())
        setUniform(locationCameraPosition, camera.position)
    }

    fun loadLights(lights: List<Light>) {
        // TODO: dynamic light count
        glUniform1i(locationLightCount, lights.size)

        lights.forEachIndexed { index, light ->
            val locations = locationLights[index]!!
            setUniform(locations.get("position")!!, light.position)
            setUniform(locations.get("color")!!, Vector3f(1.0f, 1.0f, 1.0f))
            // TODO: light color
        }
    }

    fun loadSkyColor(skyColor: Vector3f) {
        setUniform(locationSkyColor, skyColor)
    }

    fun loadFog(density: Float, gradient: Float) {
        setUniform(locationFogDensity, density)
        setUniform(locationFogGradient, gradient)
    }

    override fun resize(width: Int, height: Int) {
        setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    override fun cleanup() {
        glDeleteProgram(program.id)
        Logger.info("EntityRenderer cleaned up!")
    }
}

