package qorrnsmj.test.t11.core.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.test.t11.core.model.Model
import qorrnsmj.test.t11.core.render.UniformType.*
import qorrnsmj.test.t11.core.render.shader.EntityShaderProgram
import qorrnsmj.test.t11.game.entity.Entity

class EntityRenderer {
    val program = EntityShaderProgram()

    init {
        Logger.info("EntityRenderer initializing...")
        glUseProgram(program.id)

        MasterRenderer.setUniform(program.uniformLocationMap[AMBIENT_COLOR]!!, Vector3f(0.1f, 0.1f, 0.1f))
        MasterRenderer.setUniform(program.uniformLocationMap[SPECULAR_STRENGTH]!!, 0.5f)
        MasterRenderer.setUniform(program.uniformLocationMap[SHININESS]!!, 32.0f)

        MasterRenderer.setUniform(program.uniformLocationMap[LIGHT_POSITION]!!, Vector3f(0f, 50f, 0f))
        MasterRenderer.setUniform(program.uniformLocationMap[CONSTANT]!!, 1f)
        MasterRenderer.setUniform(program.uniformLocationMap[LINEAR]!!, 0f)
        MasterRenderer.setUniform(program.uniformLocationMap[QUADRATIC]!!, 0f)

        glUseProgram(0)
        Logger.info("EntityRenderer initialized!!")
    }

    fun render(modelEntitiesMap: HashMap<Model, MutableList<Entity>>) {
        glUseProgram(program.id)

        for ((model, entities) in modelEntitiesMap) {
            bindModel(model)
            for (entity in entities) {
                prepareEntity(entity)
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
            }
            unbindModel()
        }

        glUseProgram(0)
    }

    private fun bindModel(model: Model) {
        glBindVertexArray(model.mesh.vao)
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

    private fun prepareEntity(entity: Entity) {
        MasterRenderer.setUniform(
            program.uniformLocationMap[MODEL]!!,
            MVP.getModelMatrix(entity.pos, entity.rot, entity.scale)
        )
    }
}
