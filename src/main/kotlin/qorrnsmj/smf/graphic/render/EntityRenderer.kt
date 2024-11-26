package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.Models
import qorrnsmj.smf.game.entity.component.Model
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.graphic.shader.custom.DefaultShader
import qorrnsmj.smf.util.UniformUtils

class EntityRenderer {
    val program = DefaultShader()
    val locModel: Int
    val locView: Int
    val locProjection: Int
    val locLightCount: Int
    val locLights: MutableMap<Int, HashMap<String, Int>>
    val locMaterials: HashMap<String, Int>

    init {
        locModel = glGetUniformLocation(program.id, "model")
        locView = glGetUniformLocation(program.id, "view")
        locProjection = glGetUniformLocation(program.id, "projection")
        locLightCount = glGetUniformLocation(program.id, "light_count")

        locLights = mutableMapOf()
        for (i in 0..30) {
            locLights[i] = hashMapOf(
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

        locMaterials = hashMapOf()
        locMaterials["diffuseColor"] = glGetUniformLocation(program.id, "material.diffuseColor")
        locMaterials["ambientColor"] = glGetUniformLocation(program.id, "material.ambientColor")
        locMaterials["specularColor"] = glGetUniformLocation(program.id, "material.specularColor")
        locMaterials["emissiveColor"] = glGetUniformLocation(program.id, "material.emissiveColor")
        locMaterials["shininess"] = glGetUniformLocation(program.id, "material.shininess")
        locMaterials["opacity"] = glGetUniformLocation(program.id, "material.opacity")
        locMaterials["diffuseTexture"] = glGetUniformLocation(program.id, "material.diffuseTexture")
        locMaterials["specularTexture"] = glGetUniformLocation(program.id, "material.specularTexture")
        locMaterials["normalTexture"] = glGetUniformLocation(program.id, "material.normalTexture")
    }

    fun start() {
        glUseProgram(program.id)
    }

    fun stop() {
        glUseProgram(0)
    }

    fun render(scene: Scene) {
        UniformUtils.setUniform(locView, scene.camera.getViewMatrix())
        setLightUniforms(scene.lights)

        renderEntity(scene.entities)
    }

    /* Light */

    private fun setLightUniforms(lights: List<Light>) {
        glUniform1i(locLightCount, lights.size)

        lights.forEachIndexed { index, light ->
            val locations = locLights[index]!!
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
        if (entity.model.name != Models.EMPTY.name) {
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
            locModel,
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
        glUniform1i(locMaterials["diffuseTexture"]!!, 0)

        // Specular texture
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, material.specularTexture.id)
        glUniform1i(locMaterials["specularTexture"]!!, 1)

        // Normal texture
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, material.normalTexture.id)
        glUniform1i(locMaterials["normalTexture"]!!, 2)

        // Material
        UniformUtils.setUniform(locMaterials["diffuseColor"]!!, material.diffuseColor)
        UniformUtils.setUniform(locMaterials["ambientColor"]!!, material.ambientColor)
        UniformUtils.setUniform(locMaterials["specularColor"]!!, material.specularColor)
        UniformUtils.setUniform(locMaterials["emissiveColor"]!!, material.emissiveColor)
        UniformUtils.setUniform(locMaterials["shininess"]!!, material.shininess)
        UniformUtils.setUniform(locMaterials["opacity"]!!, material.opacity)
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
}
