package qorrnsmj.smf.graphic.shadow

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.resource.shadow.LocalShadowFrameBuffer
import qorrnsmj.smf.graphic.resource.model.Model
import qorrnsmj.smf.graphic.resource.shadow.PointShadowCubeMap
import qorrnsmj.smf.graphic.resource.shadow.ShadowFrameBuffer
import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.light.SpotLight
import qorrnsmj.smf.graphic.SceneRenderer
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.UniformUtils

class ShadowRenderer : SceneRenderer {
    private enum class ShadowCullMode {
        NONE,
        FRONT,
        BACK,
    }

    private companion object {
        const val SUN_SHADOW_MAP_SIZE = 2048
        const val LOCAL_SHADOW_MAP_SIZE = 1024
        const val POINT_SHADOW_MAP_SIZE = 512
        const val SHADOW_HALF_SIZE = 2500f
        const val SHADOW_NEAR = 1f
        const val SHADOW_FAR = 8000f
        const val SHADOW_LIGHT_DISTANCE = 3500f
        const val LOCAL_SHADOW_NEAR = 5f
        const val LOCAL_SHADOW_FAR = 4000f
        const val POINT_SHADOW_FOV = 90f
        const val ENTITY_TARGET_PADDING = 300f
        val DISABLED_STATE = ShadowRenderState(false, Matrix4f(), 0)
    }

    private val sunFrameBuffer = ShadowFrameBuffer(SUN_SHADOW_MAP_SIZE, SUN_SHADOW_MAP_SIZE)
    private val localFrameBuffer = LocalShadowFrameBuffer(LOCAL_SHADOW_MAP_SIZE, LOCAL_SHADOW_MAP_SIZE, MAX_LOCAL_LIGHT_SHADOWS)
    private val pointFrameBuffers = mutableListOf<PointShadowCubeMap>()
    private val program = ShadowShaderProgram()
    private val locationModel = glGetUniformLocation(program.id, "model")
    private val locationLightSpaceMatrix = glGetUniformLocation(program.id, "lightSpaceMatrix")
    private val locationPointShadowPass = glGetUniformLocation(program.id, "pointShadowPass")
    private val locationPointLightPosition = glGetUniformLocation(program.id, "pointLightPosition")
    private val locationPointShadowFarPlane = glGetUniformLocation(program.id, "pointShadowFarPlane")
    var currentState: ShadowRenderState = DISABLED_STATE
        private set

    override fun render(scene: Scene) {
        currentState = if (scene.renderSettings.renderProfile.shadowsEnabled) {
            createShadowState(scene)
        } else {
            DISABLED_STATE
        }
    }

    private fun createShadowState(scene: Scene): ShadowRenderState {
        val target = createShadowTarget(scene)
        val sunState = renderSunShadow(scene, target)
        val localState = renderSpotShadows(scene)
        val pointState = renderPointShadows(scene)
        return sunState.copy(local = localState, point = pointState)
    }

    private fun renderSunShadow(scene: Scene, target: Vector3f): ShadowRenderState {
        val sun = scene.environment.sunLight ?: return ShadowRenderState(false, Matrix4f(), 0)
        val sunMatrix = createSunLightSpaceMatrix(sun, target)
        renderDepthPass(
            size = SUN_SHADOW_MAP_SIZE,
            lightSpaceMatrix = sunMatrix,
            scene = scene,
            bindTarget = { sunFrameBuffer.bind() },
            cullMode = ShadowCullMode.FRONT,
        )
        return ShadowRenderState(
            enabled = true,
            lightSpaceMatrix = sunMatrix,
            depthTextureId = sunFrameBuffer.depthTexture.id,
            strength = sun.shadowStrength,
        )
    }

    private fun renderSpotShadows(scene: Scene): LocalShadowRenderState {
        val lightShadowIndices = IntArray(MAX_LOCAL_LIGHT_SHADOWS) { -1 }
        val matrices = mutableListOf<Matrix4f>()
        val strengths = mutableListOf<Float>()

        scene.world.lights.forEachIndexed { lightIndex, light ->
            if (matrices.size >= MAX_LOCAL_LIGHT_SHADOWS) return@forEachIndexed
            if (light !is SpotLight || !light.castsShadow || light.intensity <= 0f) return@forEachIndexed

            val shadowFarPlane = estimateShadowRange(light)
            val lightSpaceMatrix = createSpotLightSpaceMatrix(light, shadowFarPlane) ?: return@forEachIndexed
            val layerIndex = matrices.size
            renderDepthPass(
                size = LOCAL_SHADOW_MAP_SIZE,
                lightSpaceMatrix = lightSpaceMatrix,
                scene = scene,
                bindTarget = { localFrameBuffer.bindLayer(layerIndex) },
                cullMode = ShadowCullMode.FRONT,
            )
            lightShadowIndices[lightIndex] = layerIndex
            matrices += lightSpaceMatrix
            strengths += light.shadowStrength
        }

        if (matrices.isEmpty()) return LocalShadowRenderState()

        return LocalShadowRenderState(
            enabled = true,
            depthTextureId = localFrameBuffer.depthTextureId,
            count = matrices.size,
            matrices = matrices,
            strengths = strengths.toFloatArray(),
            lightShadowIndices = lightShadowIndices,
        )
    }

    private fun renderPointShadows(scene: Scene): PointShadowRenderState {
        val pointLights = scene.world.lights.withIndex()
            .filter { (_, light) -> light is PointLight && light.castsShadow && light.intensity > 0f }
            .take(MAX_POINT_LIGHT_SHADOWS)

        if (pointLights.isEmpty()) return PointShadowRenderState()

        ensurePointShadowBuffers(pointLights.size)
        val textureIds = IntArray(pointLights.size)
        val strengths = FloatArray(pointLights.size)
        val farPlanes = FloatArray(pointLights.size)
        val lightShadowIndices = IntArray(MAX_LOCAL_LIGHT_SHADOWS) { -1 }

        pointLights.forEachIndexed { shadowIndex, indexedLight ->
            val light = indexedLight.value as PointLight
            val frameBuffer = pointFrameBuffers[shadowIndex]
            val shadowFarPlane = estimateShadowRange(light)
            val matrices = createPointShadowMatrices(light.position, shadowFarPlane)
            matrices.forEachIndexed { faceIndex, matrix ->
                renderDepthPass(
                    size = POINT_SHADOW_MAP_SIZE,
                    lightSpaceMatrix = matrix,
                    scene = scene,
                    bindTarget = { frameBuffer.bindFace(faceIndex) },
                    pointLightPosition = light.position,
                    pointShadowFarPlane = shadowFarPlane,
                    cullMode = ShadowCullMode.NONE,
                )
            }
            textureIds[shadowIndex] = frameBuffer.depthTextureId
            strengths[shadowIndex] = light.shadowStrength
            farPlanes[shadowIndex] = shadowFarPlane
            lightShadowIndices[indexedLight.index] = shadowIndex
        }

        return PointShadowRenderState(
            enabled = true,
            count = pointLights.size,
            textureIds = textureIds,
            strengths = strengths,
            lightShadowIndices = lightShadowIndices,
            farPlanes = farPlanes,
        )
    }

    private fun ensurePointShadowBuffers(count: Int) {
        while (pointFrameBuffers.size < count) {
            pointFrameBuffers += PointShadowCubeMap(POINT_SHADOW_MAP_SIZE)
        }
    }

    private fun renderDepthPass(
        size: Int,
        lightSpaceMatrix: Matrix4f,
        scene: Scene,
        bindTarget: () -> Unit,
        pointLightPosition: Vector3f? = null,
        pointShadowFarPlane: Float = LOCAL_SHADOW_FAR,
        cullMode: ShadowCullMode = ShadowCullMode.BACK,
    ) {
        val previousViewport = IntArray(4)
        glGetIntegerv(GL_VIEWPORT, previousViewport)
        bindTarget()
        glViewport(0, 0, size, size)
        glClear(GL_DEPTH_BUFFER_BIT)

        val cullWasEnabled = glIsEnabled(GL_CULL_FACE)
        val previousCullFace = glGetInteger(GL_CULL_FACE_MODE)
        when (cullMode) {
            ShadowCullMode.NONE -> glDisable(GL_CULL_FACE)
            ShadowCullMode.FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT)
            }
            ShadowCullMode.BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK)
            }
        }

        program.use()
        UniformUtils.setUniform(locationLightSpaceMatrix, lightSpaceMatrix)
        UniformUtils.setUniform(locationPointShadowPass, if (pointLightPosition != null) 1 else 0)
        UniformUtils.setUniform(locationPointShadowFarPlane, pointShadowFarPlane)
        UniformUtils.setUniform(locationPointLightPosition, pointLightPosition ?: Vector3f())

        scene.world.terrain?.let { renderTerrain(it) }
        renderEntities(scene.world.entities)

        if (cullWasEnabled) {
            glEnable(GL_CULL_FACE)
            glCullFace(previousCullFace)
        } else {
            glDisable(GL_CULL_FACE)
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3])
    }

    private fun createSunLightSpaceMatrix(sunLight: DirectionalLight, target: Vector3f): Matrix4f {
        val projection = MVP.getOrthographicMatrix(
            -SHADOW_HALF_SIZE,
            SHADOW_HALF_SIZE,
            -SHADOW_HALF_SIZE,
            SHADOW_HALF_SIZE,
            SHADOW_NEAR,
            SHADOW_FAR,
        )
        val lightDirection = sunLight.direction
        val lightPosition = target.subtract(lightDirection.scale(SHADOW_LIGHT_DISTANCE))
        val view = MVP.getViewMatrix(lightPosition, target, stableUp(lightDirection))
        return projection.multiply(view)
    }

    private fun createSpotLightSpaceMatrix(light: SpotLight, farPlane: Float): Matrix4f? {
        if (light.direction.length() < 0.001f) return null
        val fov = (light.outerCutOffDegrees * 2f).coerceIn(20f, 120f)
        val lookTarget = light.position.add(light.direction.scale(farPlane * 0.5f))
        val projection = MVP.getPerspectiveMatrix(1f, fov, LOCAL_SHADOW_NEAR, farPlane)
        val view = MVP.getViewMatrix(light.position, lookTarget, stableUp(light.direction))
        return projection.multiply(view)
    }

    private fun createPointShadowMatrices(position: Vector3f, farPlane: Float): List<Matrix4f> {
        val projection = MVP.getPerspectiveMatrix(1f, POINT_SHADOW_FOV, LOCAL_SHADOW_NEAR, farPlane)
        val faces = listOf(
            Vector3f(1f, 0f, 0f) to Vector3f(0f, -1f, 0f),
            Vector3f(-1f, 0f, 0f) to Vector3f(0f, -1f, 0f),
            Vector3f(0f, 1f, 0f) to Vector3f(0f, 0f, 1f),
            Vector3f(0f, -1f, 0f) to Vector3f(0f, 0f, -1f),
            Vector3f(0f, 0f, 1f) to Vector3f(0f, -1f, 0f),
            Vector3f(0f, 0f, -1f) to Vector3f(0f, -1f, 0f),
        )
        return faces.map { (direction, up) ->
            projection.multiply(MVP.getViewMatrix(position, position.add(direction), up))
        }
    }

    private fun estimateShadowRange(light: PointLight): Float {
        return estimateShadowRange(light.constant, light.linear, light.quadratic, light.intensity)
    }

    private fun estimateShadowRange(light: SpotLight): Float {
        return estimateShadowRange(light.constant, light.linear, light.quadratic, light.intensity)
    }

    private fun estimateShadowRange(constant: Float, linear: Float, quadratic: Float, intensity: Float): Float {
        val minimumVisibleRadiance = 0.025f
        val target = (intensity / minimumVisibleRadiance).coerceAtLeast(1f)
        val a = quadratic
        val b = linear
        val c = constant - target

        val distance = when {
            a > 0.000001f -> {
                val discriminant = b * b - 4f * a * c
                if (discriminant <= 0f) 600f else (-b + kotlin.math.sqrt(discriminant)) / (2f * a)
            }
            b > 0.000001f -> -c / b
            else -> 600f
        }

        return distance.coerceIn(120f, 1800f)
    }

    private fun stableUp(direction: Vector3f): Vector3f {
        val normalized = direction.normalize()
        val worldUp = Vector3f(0f, 1f, 0f)
        return if (kotlin.math.abs(normalized.dot(worldUp)) > 0.95f) Vector3f(0f, 0f, 1f) else worldUp
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

        fun includeEntity(entity: Entity) {
            val position = entity.worldTransform.position
            include(
                Vector3f(position.x - ENTITY_TARGET_PADDING, position.y - ENTITY_TARGET_PADDING, position.z - ENTITY_TARGET_PADDING),
                Vector3f(position.x + ENTITY_TARGET_PADDING, position.y + ENTITY_TARGET_PADDING, position.z + ENTITY_TARGET_PADDING),
            )
            entity.children.forEach { includeEntity(it) }
        }

        scene.world.terrain?.let { terrain ->
            val size = terrain.model.mesh.size
            include(terrain.position, Vector3f(terrain.position.x + size.x, terrain.position.y, terrain.position.z + size.y))
        }
        scene.world.entities.forEach { includeEntity(it) }

        if (!hasBounds) return Vector3f()
        return Vector3f((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f)
    }

    private fun renderTerrain(terrain: Terrain) {
        UniformUtils.setUniform(locationModel, MVP.getModelMatrix(terrain.position, Quaternion.identity(), Vector3f(1f, 1f, 1f)))
        glBindVertexArray(terrain.model.mesh.vao)
        glDrawElements(GL_TRIANGLES, terrain.model.mesh.vertexCount, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    private fun renderEntities(entities: List<Entity>) {
        val batchMap = mutableMapOf<Model, MutableList<Entity>>()
        for (entity in entities) processEntity(entity, batchMap)

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
        if (entity.model != EntityModels.EMPTY) batchMap.getOrPut(entity.model) { mutableListOf() }.add(entity)
        for (child in entity.children) processEntity(child, batchMap)
    }
}
