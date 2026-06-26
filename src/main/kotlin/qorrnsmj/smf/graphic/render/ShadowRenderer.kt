package qorrnsmj.smf.graphic.render

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.map.GameMap
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.`object`.ShadowFrameBuffer
import qorrnsmj.smf.graphic.render.shader.ShadowShaderProgram
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.UniformUtils

class ShadowRenderer {
    private companion object {
        const val SHADOW_MAP_SIZE = 2048
        const val SHADOW_HALF_SIZE = 2500f
        const val SHADOW_NEAR = 1f
        const val SHADOW_FAR = 8000f
    }

    private val frameBuffer = ShadowFrameBuffer(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE)
    private val program = ShadowShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationLightSpaceMatrix = glGetUniformLocation(program.id, "lightSpaceMatrix")

    fun render(scene: Scene): ShadowRenderState {
        val light = scene.lights.firstOrNull()
            ?: return ShadowRenderState(false, Matrix4f(), 0)

        val lightSpaceMatrix = createLightSpaceMatrix(light.position, createShadowTarget(scene))

        frameBuffer.bind()
        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE)
        glClear(GL_DEPTH_BUFFER_BIT)

        val cullWasEnabled = glIsEnabled(GL_CULL_FACE)
        val previousCullFace = glGetInteger(GL_CULL_FACE_MODE)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_FRONT)

        program.use()
        UniformUtils.setUniform(locationLightSpaceMatrix, lightSpaceMatrix)

        scene.terrain?.let { renderTerrain(it) }
        scene.map?.let { renderMap(it) }
        renderEntities(scene.entities)

        if (cullWasEnabled) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        glCullFace(previousCullFace)
        frameBuffer.bindDefault()

        return ShadowRenderState(
            enabled = true,
            lightSpaceMatrix = lightSpaceMatrix,
            depthTextureId = frameBuffer.depthTexture.id,
        )
    }

    private fun createLightSpaceMatrix(lightPosition: Vector3f, target: Vector3f): Matrix4f {
        val projection = MVP.getOrthographicMatrix(
            -SHADOW_HALF_SIZE,
            SHADOW_HALF_SIZE,
            -SHADOW_HALF_SIZE,
            SHADOW_HALF_SIZE,
            SHADOW_NEAR,
            SHADOW_FAR,
        )
        val lightToTarget = target.subtract(lightPosition)
        val lightDirection = if (lightToTarget.lengthSquared() > 0.0001f) {
            lightToTarget.normalize()
        } else {
            Vector3f(0f, -1f, 0f)
        }
        val worldUp = Vector3f(0f, 1f, 0f)
        val up = if (kotlin.math.abs(lightDirection.dot(worldUp)) > 0.95f) {
            Vector3f(0f, 0f, 1f)
        } else {
            worldUp
        }
        val view = MVP.getViewMatrix(lightPosition, target, up)
        return projection.multiply(view)
    }

    private fun createShadowTarget(scene: Scene): Vector3f {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var hasBounds = false

        fun include(min: Vector3f, max: Vector3f) {
            minX = minOf(minX, min.x)
            minY = minOf(minY, min.y)
            minZ = minOf(minZ, min.z)
            maxX = maxOf(maxX, max.x)
            maxY = maxOf(maxY, max.y)
            maxZ = maxOf(maxZ, max.z)
            hasBounds = true
        }

        scene.terrain?.let { terrain ->
            val size = terrain.model.mesh.size
            include(
                terrain.position,
                Vector3f(
                    terrain.position.x + size.x,
                    terrain.position.y,
                    terrain.position.z + size.y,
                ),
            )
        }

        scene.map?.brushes?.forEach { brush ->
            include(brush.bounds.min, brush.bounds.max)
        }

        if (!hasBounds) return Vector3f()

        return Vector3f(
            (minX + maxX) * 0.5f,
            (minY + maxY) * 0.5f,
            (minZ + maxZ) * 0.5f,
        )
    }

    private fun renderMap(gameMap: GameMap) {
        UniformUtils.setUniform(
            locationModel,
            MVP.getModelMatrix(Vector3f(), Vector3f(), Vector3f(1f, 1f, 1f)),
        )

        for (mesh in gameMap.meshesByTexture.values) {
            glBindVertexArray(mesh.vao)
            glDrawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0)
        }
        glBindVertexArray(0)
    }

    private fun renderTerrain(terrain: Terrain) {
        UniformUtils.setUniform(
            locationModel,
            MVP.getModelMatrix(terrain.position, Vector3f(), Vector3f(1f, 1f, 1f)),
        )
        glBindVertexArray(terrain.model.mesh.vao)
        glDrawElements(GL_TRIANGLES, terrain.model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    private fun renderEntities(entities: List<Entity>) {
        val batchMap = mutableMapOf<Model, MutableList<Entity>>()
        for (entity in entities) {
            processEntity(entity, batchMap)
        }

        for ((model, targets) in batchMap) {
            if (model.material.alphaMode == AlphaMode.BLEND) continue

            glBindVertexArray(model.mesh.vao)
            for (target in targets) {
                val world = target.worldTransform
                UniformUtils.setUniform(locationModel, MVP.getModelMatrix(world.position, world.rotation, world.scale))
                glDrawElements(GL_TRIANGLES, model.mesh.vertexCount, model.mesh.vertexType, 0)
            }
        }
        glBindVertexArray(0)
    }

    private fun processEntity(entity: Entity, batchMap: MutableMap<Model, MutableList<Entity>>) {
        if (entity.model != EntityModels.EMPTY) {
            batchMap.getOrPut(entity.model) { mutableListOf() }.add(entity)
        }

        for (child in entity.children) {
            processEntity(child, batchMap)
        }
    }
}
