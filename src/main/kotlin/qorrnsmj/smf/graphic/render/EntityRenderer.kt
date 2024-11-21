package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.component.Entity
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.graphic.render.camera.Camera
import qorrnsmj.smf.graphic.shader.custom.DefaultShader
import qorrnsmj.smf.graphic.shader.custom.DefaultShader.Uniform.*
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
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

        UniformUtils.setUniform(LIGHT_POSITION.location, Vector3f(0f, 50f, 0f))
        UniformUtils.setUniform(AMBIENT_COLOR.location, Vector3f(1f, 1f, 1f))
        UniformUtils.setUniform(SPECULAR_STRENGTH.location, 0.5f)
        UniformUtils.setUniform(SHININESS.location, 32.0f)

        UniformUtils.setUniform(CONSTANT.location, 1f)
        UniformUtils.setUniform(LINEAR.location, 0f)
        UniformUtils.setUniform(QUADRATIC.location, 0f)

        glUseProgram(0)
        Logger.info("EntityRenderer initialized!")
    }

    fun start() {
        glUseProgram(program.id)
    }

    fun stop() {
        glUseProgram(0)
    }

    fun setProjection(matrix: Matrix4f) {
        UniformUtils.setUniform(PROJECTION.location, matrix)
    }

    fun setCamera(camera: Camera) {
        UniformUtils.setUniform(VIEW.location, camera.getViewMatrix())
    }

    fun render(entities: MutableList<Entity>) {
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
            MVP.getModelMatrix(entity.pos, entity.rot, entity.scale)
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
