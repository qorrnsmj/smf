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
            bindModel(model)
            for (target in targets) {
                prepareEntity(target)
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            }
            unbindModel()
        }

        modelEntitiesMap.clear()
    }

    private fun processEntity(entity: Entity) {
        val batch = modelEntitiesMap.getOrPut(entity.getModel()) { mutableListOf() }
        batch.add(entity)
    }

    private fun prepareEntity(entity: Entity) {
        UniformUtils.setUniform(
            MODEL.location,
            MVP.getModelMatrix(entity.position, entity.rot, entity.scale)
        )
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vaoID)
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, model.texture.id)
    }

    private fun unbindModel() {
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glBindVertexArray(0)

        glBindTexture(GL_TEXTURE_2D, 0)
    }
}
