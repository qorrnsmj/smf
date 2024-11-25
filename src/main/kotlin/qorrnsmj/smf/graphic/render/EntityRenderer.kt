package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.component.Model
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.graphic.shader.custom.DefaultShader
import qorrnsmj.smf.graphic.shader.custom.DefaultShader.Uniform.*
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.util.UniformUtils

class EntityRenderer {
    val program = DefaultShader
    // TODO: Modelsの同じモデルをまとめる (複数をあつめたモデルのクラスを新しく作る？)
    private val modelEntitiesMap = HashMap<Model, MutableList<Entity>>()

    init {
        Logger.info("EntityRenderer initializing...")
        glUseProgram(program.id)

        UniformUtils.setUniform(MODEL.location, Matrix4f())
        UniformUtils.setUniform(VIEW.location, Matrix4f())
        UniformUtils.setUniform(PROJECTION.location, Matrix4f())

        glUseProgram(0)
        Logger.info("EntityRenderer initialized!")
    }

    fun start() {
        glUseProgram(program.id)
    }

    fun stop() {
        glUseProgram(0)
    }

    fun render(scene: Scene) {
        UniformUtils.setUniform(VIEW.location, scene.camera.getViewMatrix())
        setLightUniforms(scene.lights)

        renderEntity(scene.entities)
    }

    /* Light */

    // TODO: position[COUNT] みたいな変数を持つクラスにする
    private fun setLightUniforms(lights: List<Light>) {
        glUniform1i(glGetUniformLocation(program.id, "light_count"), lights.size)

        lights.forEachIndexed { index, light ->
            glUniform3f(glGetUniformLocation(program.id, "lights[$index].position"), light.position.x, light.position.y, light.position.z)
            glUniform3f(glGetUniformLocation(program.id, "lights[$index].ambient"), light.ambient.x, light.ambient.y, light.ambient.z)
            glUniform3f(glGetUniformLocation(program.id, "lights[$index].diffuse"), light.diffuse.x, light.diffuse.y, light.diffuse.z)
            glUniform3f(glGetUniformLocation(program.id, "lights[$index].specular"), light.specular.x, light.specular.y, light.specular.z)
            glUniform1f(glGetUniformLocation(program.id, "lights[$index].shininess"), light.shininess)
            glUniform1f(glGetUniformLocation(program.id, "lights[$index].constant"), light.constant)
            glUniform1f(glGetUniformLocation(program.id, "lights[$index].linear"), light.linear)
            glUniform1f(glGetUniformLocation(program.id, "lights[$index].quadratic"), light.quadratic)
        }
    }

    /* Entity */

    private fun renderEntity(entities: List<Entity>) {
        for (entity in entities) processEntity(entity)

        for ((model, targets) in modelEntitiesMap) {
            // モデル毎に設定するのはマテリアル
            bindModel(model)
            for (target in targets) {
                // エンティティ毎に設定するのはModel行列
                prepareEntity(target)
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            }
            unbindModel()
        }

        modelEntitiesMap.clear()
    }

    // FIXME: ここでgetUniformLocationしない
    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vaoID)

        program.enableAttributes()
        val material = model.material

        // Diffuse texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, material.diffuseTexture.id)
        glUniform1i(glGetUniformLocation(program.id, "material.diffuseTexture"), 0)

        // Specular texture
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, material.specularTexture.id)
        glUniform1i(glGetUniformLocation(program.id, "material.specularTexture"), 1)

        // Normal texture
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, material.normalTexture.id)
        glUniform1i(glGetUniformLocation(program.id, "material.normalTexture"), 2)

        glUniform3f(glGetUniformLocation(program.id, "material.diffuseColor"), material.diffuseColor.x, material.diffuseColor.y, material.diffuseColor.z)
        glUniform3f(glGetUniformLocation(program.id, "material.ambientColor"), material.ambientColor.x, material.ambientColor.y, material.ambientColor.z)
        glUniform3f(glGetUniformLocation(program.id, "material.specularColor"), material.specularColor.x, material.specularColor.y, material.specularColor.z)
        glUniform3f(glGetUniformLocation(program.id, "material.emissiveColor"), material.emissiveColor.x, material.emissiveColor.y, material.emissiveColor.z)
        glUniform3f(glGetUniformLocation(program.id, "material.emissiveColor"), material.emissiveColor.x, material.emissiveColor.y, material.emissiveColor.z)
        glUniform1f(glGetUniformLocation(program.id, "material.shininess"), material.shininess)
        glUniform1f(glGetUniformLocation(program.id, "material.opacity"), material.opacity)
    }

    // FIXME: 本当にいる？bindModelで全て上書きしちゃえばよくない？
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

    // TODO: 複数モデルをもつエンティティのバッチ処理にする
    private fun processEntity(entity: Entity) {
        for (model in entity.getModels()) {
            val batch = modelEntitiesMap.getOrPut(model) { mutableListOf() }
            batch.add(entity)
        }
    }

    private fun prepareEntity(entity: Entity) {
        UniformUtils.setUniform(
            MODEL.location,
            MVP.getModelMatrix(entity.position, entity.rot, entity.scale)
        )
    }
}
