package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.model.Models
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.graphic.shader.custom.DefaultShaderProgram
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class EntityRenderer : Resizable {
    val program = DefaultShaderProgram()
    val locationModel = glGetUniformLocation(program.id, "model")
    val locationView = glGetUniformLocation(program.id, "view")
    val locationProjection = glGetUniformLocation(program.id, "projection")
    val locationUseFakeLighting = glGetUniformLocation(program.id, "useFakeLighting")
    val locationSkyColor = glGetUniformLocation(program.id, "skyColor")
    val locationLightCount = glGetUniformLocation(program.id, "lightCount")
    val locationLights = mutableMapOf<Int, HashMap<String, Int>>()
    val locationMaterials = hashMapOf<String, Int>()

    init {
        for (i in 0..30) {
            locationLights[i] = hashMapOf(
                "position" to glGetUniformLocation(program.id, "lights[$i].position"),
                "ambient" to glGetUniformLocation(program.id, "lights[$i].ambient"),
                "diffuse" to glGetUniformLocation(program.id, "lights[$i].diffuse"),
                "specular" to glGetUniformLocation(program.id, "lights[$i].specular"),
                "shininess" to glGetUniformLocation(program.id, "lights[$i].shininess"),
                "constant" to glGetUniformLocation(program.id, "lights[$i].constant"),
                "linear" to glGetUniformLocation(program.id, "lights[$i].linear"),
                "quadratic" to glGetUniformLocation(program.id, "lights[$i].quadratic")
            )
        }

        locationMaterials["diffuseColor"] = glGetUniformLocation(program.id, "material.diffuseColor")
        locationMaterials["ambientColor"] = glGetUniformLocation(program.id, "material.ambientColor")
        locationMaterials["specularColor"] = glGetUniformLocation(program.id, "material.specularColor")
        locationMaterials["emissiveColor"] = glGetUniformLocation(program.id, "material.emissiveColor")
        locationMaterials["shininess"] = glGetUniformLocation(program.id, "material.shininess")
        locationMaterials["opacity"] = glGetUniformLocation(program.id, "material.opacity")
        locationMaterials["diffuseTexture"] = glGetUniformLocation(program.id, "material.diffuseTexture")
        locationMaterials["specularTexture"] = glGetUniformLocation(program.id, "material.specularTexture")
        locationMaterials["normalTexture"] = glGetUniformLocation(program.id, "material.normalTexture")
    }

    fun start() {
        program.use()
        //program.enableAttributes()
    }

    fun stop() {
        //program.disableAttributes()
        program.unuse()
    }

    /* Render */

    // TODO: 基本的にParentの子供は後から追加しない限りテクスチャは同じだから、テクスチャが同じだったらbindTextureしないようにする
    // TODO: そのためにはbindTexture()作るのと、modelEntityMapの中身を更に同じテクスチャIDでまとめる (今は同じテクスチャでも別のテクスチャIDになっちゃってる)
    fun renderEntities(entities: List<Entity>) {
        val batchMap = mutableMapOf<Model, MutableList<Entity>>()
        for (entity in entities) processEntity(entity, batchMap)

        for ((model, targets) in batchMap) {
            // モデル毎に設定するのはマテリア
            bindModel(model)
            for (target in targets) {
                // エンティティ毎に設定するのはModel行列
                prepareEntity(target)
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            }
            unbindModel()
        }
    }

    // TODO: 親のpos, rot, scaleを子にも適用 -> そもそもmodel-matrix共有しとけば？ (同じfbxのモデルならわかるけど、後から別モデルをchildren登録したらどうなる？)
    private fun processEntity(entity: Entity, batchMap: MutableMap<Model, MutableList<Entity>>) {
        // Parent model
        if (entity.model != Models.EMPTY) {
            val batch = batchMap.getOrPut(entity.model) { mutableListOf() }
            batch.add(entity)
        }

        // Children model
        for (child in entity.children) {
            processEntity(child, batchMap)
        }
    }

    private fun prepareEntity(entity: Entity) {
        UniformUtils.setUniform(
            locationModel,
            MVP.getModelMatrix(entity.position, entity.rotation, entity.scale)
        )
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vaoID)
//        program.enableAttributes()

        // Transparency and Fake lighting
        if (model.hasTransparency) glDisable(GL_CULL_FACE)
        glUniform1i(locationUseFakeLighting, if (model.useFakeLighting) 1 else 0)

        // Diffuse texture
        val material = model.material
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, material.diffuseTexture.id)
        glUniform1i(locationMaterials["diffuseTexture"]!!, 0)

        // Specular texture
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, material.specularTexture.id)
        glUniform1i(locationMaterials["specularTexture"]!!, 1)

        // Normal texture
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, material.normalTexture.id)
        glUniform1i(locationMaterials["normalTexture"]!!, 2)

        // Material
        UniformUtils.setUniform(locationMaterials["diffuseColor"]!!, material.diffuseColor)
        UniformUtils.setUniform(locationMaterials["ambientColor"]!!, material.ambientColor)
        UniformUtils.setUniform(locationMaterials["specularColor"]!!, material.specularColor)
        UniformUtils.setUniform(locationMaterials["emissiveColor"]!!, material.emissiveColor)
        UniformUtils.setUniform(locationMaterials["shininess"]!!, material.shininess)
        UniformUtils.setUniform(locationMaterials["opacity"]!!, material.opacity)
    }

    private fun unbindModel() {
//        program.disableAttributes()
        glBindVertexArray(0)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    /* Uniforms */

    fun loadCamera(camera: Camera) {
        UniformUtils.setUniform(locationView, camera.getViewMatrix())
    }

    fun loadLights(lights: List<Light>) {
        glUniform1i(locationLightCount, lights.size)

        lights.forEachIndexed { index, light ->
            val locations = locationLights[index]!!
            glUniform3f(locations.get("position")!!, light.position.x, light.position.y, light.position.z)
            glUniform3f(locations.get("ambient")!!, light.ambient.x, light.ambient.y, light.ambient.z)
            glUniform3f(locations.get("diffuse")!!, light.diffuse.x, light.diffuse.y, light.diffuse.z)
            glUniform3f(locations.get("specular")!!, light.specular.x, light.specular.y, light.specular.z)
            glUniform1f(locations.get("shininess")!!, light.shininess)
            glUniform1f(locations.get("constant")!!, light.constant)
            glUniform1f(locations.get("linear")!!, light.linear)
            glUniform1f(locations.get("quadratic")!!, light.quadratic)
        }
    }

    fun loadSkyColor(skyColor: Vector3f) {
        UniformUtils.setUniform(locationSkyColor, skyColor)
    }

    override fun resize(width: Int, height: Int) {
        UniformUtils.setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }
}
