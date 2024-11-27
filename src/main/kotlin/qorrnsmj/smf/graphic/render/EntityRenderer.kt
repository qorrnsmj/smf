package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.Scene
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.model.Model
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.graphic.shader.custom.DefaultShader
import qorrnsmj.smf.util.UniformUtils

class EntityRenderer {
    val program = DefaultShader()
    val locationModel: Int
    val locationView: Int
    val locationProjection: Int
    val locationLightCount: Int
    val locationLights: MutableMap<Int, HashMap<String, Int>>
    val locationMaterials: HashMap<String, Int>

    init {
        locationModel = glGetUniformLocation(program.id, "model")
        locationView = glGetUniformLocation(program.id, "view")
        locationProjection = glGetUniformLocation(program.id, "projection")
        locationLightCount = glGetUniformLocation(program.id, "light_count")

        locationLights = mutableMapOf()
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

        locationMaterials = hashMapOf()
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
        glUseProgram(program.id)
    }

    fun stop() {
        glUseProgram(0)
    }

    fun render(scene: Scene) {
        UniformUtils.setUniform(locationView, scene.camera.getViewMatrix())
        setLightUniforms(scene.lights)

        renderEntity(scene.entities)
    }

    /* Entity */

    // TODO: 基本的にParentの子供は後から追加しない限りテクスチャは同じだから、テクスチャが同じだったらbindTextureしないようにする
    // TODO: そのためにはbindTexture()作るのと、modelEntityMapの中身を更に同じテクスチャIDでまとめる (今は同じテクスチャでも別のテクスチャIDになっちゃってる)
    private fun renderEntity(entities: List<Entity>) {
        val batchMap = mutableMapOf<Model, MutableList<Entity>>()
        for (entity in entities) processEntity(entity, batchMap)

        for ((model, targets) in batchMap) {
            // モデル毎に設定するのはマテリアmodelEntityMap = {HashMap@1863}  size = 9ル(テクスチャ)
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
        if (entity.model.name != EntityModels.EMPTY.name) {
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

        val material = model.material
        program.enableAttributes()

        // Diffuse texture
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
        program.disableAttributes()
        glBindVertexArray(0)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    /* Light */

    private fun setLightUniforms(lights: List<Light>) {
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
}
