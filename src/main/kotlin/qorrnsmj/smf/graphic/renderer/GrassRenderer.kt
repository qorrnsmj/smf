package qorrnsmj.smf.graphic.renderer

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.buffer.VertexArrayObject
import qorrnsmj.smf.graphic.resource.buffer.VertexBufferObject
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.grass.*
import qorrnsmj.smf.graphic.scene.light.SpotLight
import qorrnsmj.smf.graphic.render.RenderSettings
import qorrnsmj.smf.graphic.render.ShadingMode
import qorrnsmj.smf.graphic.scene.shadow.ShadowRenderState
import qorrnsmj.smf.graphic.resource.texture.TextureLoader
import qorrnsmj.smf.graphic.resource.texture.TexturePresets
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils.setUniform
import kotlin.math.cos
import kotlin.math.sin

class GrassRenderer(
    private val renderSettings: RenderSettings = RenderSettings(),
    private val shadowStateProvider: () -> ShadowRenderState? = { null },
) : SceneRenderer, Resizable {
    constructor(
        shadowRenderer: ShadowRenderer,
        renderSettings: RenderSettings = RenderSettings(),
    ) : this(renderSettings, { shadowRenderer.currentState })

    private var resources: Resources? = null
    private var projection = MVP.getPerspectiveMatrix(16f / 9f)

    override fun render(scene: Scene) {
        val patches = scene.world.grassPatches.filter { it.instances.isNotEmpty() }
        if (patches.isEmpty()) {
            clear()
            return
        }
        if (renderSettings.shadingMode != ShadingMode.RENDERED) return
        val gpu = resources ?: Resources().also { resources = it }
        val obsolete = gpu.patches.keys.filter { it !in patches }
        obsolete.forEach { gpu.patches.remove(it)?.delete() }

        val culling = glIsEnabled(GL_CULL_FACE)
        val blending = glIsEnabled(GL_BLEND)
        val depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK)
        gpu.program.use()
        glDisable(GL_CULL_FACE)
        glDisable(GL_BLEND)
        glDepthMask(true)
        try {
            setUniform(gpu.uniform("projection"), projection)
            setUniform(gpu.uniform("view"), scene.world.camera.getViewMatrix())
            setUniform(gpu.uniform("cameraPosition"), scene.world.camera.position)
            setUniform(gpu.uniform("foliageTexture"), gpu.grassTexture.id, FOLIAGE_TEXTURE_UNIT)
            val shadow = shadowStateProvider()
            val shadowEnabled = shadow?.enabled == true && shadow.depthTextureId != 0
            setUniform(gpu.uniform("shadowEnabled"), if (shadowEnabled) 1 else 0)
            setUniform(gpu.uniform("lightSpaceMatrix"), shadow?.lightSpaceMatrix ?: Matrix4f())
            setUniform(gpu.uniform("shadowStrength"), shadow?.strength ?: 0f)
            setUniform(gpu.uniform("shadowFilterRadius"), shadow?.filterRadius ?: 1f)
            setUniform(gpu.uniform("shadowMinBias"), shadow?.receiverMinBias ?: 0.00004f)
            if (shadowEnabled) {
                setUniform(gpu.uniform("shadowMap"), shadow!!.depthTextureId, SHADOW_TEXTURE_UNIT)
            }
            loadLights(gpu, scene)
            val fog = scene.environment.fog
            setUniform(gpu.uniform("fog.enabled"), if (fog.enabled) 1 else 0)
            setUniform(gpu.uniform("fog.color"), fog.color)
            setUniform(gpu.uniform("fog.distanceDensity"), fog.distanceDensity)
            setUniform(gpu.uniform("fog.distanceGradient"), fog.distanceGradient)
            setUniform(gpu.uniform("fog.heightDensity"), fog.heightDensity)
            setUniform(gpu.uniform("fog.bottomY"), fog.bottomY)
            setUniform(gpu.uniform("fog.topY"), fog.topY)
            setUniform(gpu.uniform("fog.heightFalloff"), fog.heightFalloff)
            setUniform(gpu.uniform("fog.heightDistanceStart"), fog.heightDistanceStart)
            setUniform(gpu.uniform("fog.heightDistanceEnd"), fog.heightDistanceEnd)
            for (patch in patches) {
                val geometry = gpu.geometries.getValue(patch.foliageType)
                val mesh = gpu.patches.getOrPut(patch) { PatchMesh(patch, geometry.vertices) }
                setUniform(gpu.uniform("textured"), if (patch.foliageType == FoliageType.GRASS) 1 else 0)
                setUniform(gpu.uniform("timeSeconds"), patch.timeSeconds)
                setUniform(gpu.uniform("windDirection"), patch.windDirection)
                setUniform(gpu.uniform("windStrength"), patch.windStrength.coerceAtLeast(0f))
                setUniform(gpu.uniform("windSpeed"), patch.windSpeed.coerceAtLeast(0f))
                setUniform(gpu.uniform("drawDistance"), patch.drawDistance.coerceAtLeast(1f))
                mesh.vao.bind()
                glDrawArraysInstanced(GL_TRIANGLES, 0, geometry.vertexCount, patch.instances.size)
            }
        } finally {
            glBindVertexArray(0)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glActiveTexture(GL_TEXTURE0 + SHADOW_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_2D, 0)
            glActiveTexture(GL_TEXTURE0 + FOLIAGE_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_2D, 0)
            glActiveTexture(GL_TEXTURE0)
            glDepthMask(depthWrite)
            if (culling) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
            if (blending) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        }
    }

    override fun resize(width: Int, height: Int) {
        projection = MVP.getPerspectiveMatrix(width.coerceAtLeast(1) / height.coerceAtLeast(1).toFloat())
    }

    private fun loadLights(gpu: Resources, scene: Scene) {
        val sun = scene.environment.celestialLight
        setUniform(gpu.uniform("sunLight.direction"), sun?.direction ?: Vector3f(0f, -1f, 0f))
        setUniform(gpu.uniform("sunLight.color"), sun?.color ?: Vector3f(1f, 1f, 1f))
        setUniform(gpu.uniform("sunLight.intensity"), sun?.intensity ?: 0f)
        setUniform(gpu.uniform("sunLight.ambientColor"), sun?.ambientColor ?: Vector3f(1f, 1f, 1f))
        setUniform(gpu.uniform("sunLight.ambientIntensity"), sun?.ambientIntensity ?: 1f)

        val lights = scene.world.activeLights.take(MAX_LOCAL_LIGHTS)
        setUniform(gpu.uniform("lightCount"), lights.size)
        lights.forEachIndexed { index, light ->
            val prefix = "lights[$index]"
            setUniform(gpu.uniform("$prefix.position"), light.position)
            setUniform(gpu.uniform("$prefix.color"), light.diffuse)
            setUniform(gpu.uniform("$prefix.intensity"), light.intensity)
            setUniform(gpu.uniform("$prefix.constant"), light.constant)
            setUniform(gpu.uniform("$prefix.linear"), light.linear)
            setUniform(gpu.uniform("$prefix.quadratic"), light.quadratic)
            if (light is SpotLight) {
                setUniform(gpu.uniform("$prefix.type"), 1)
                setUniform(gpu.uniform("$prefix.direction"), light.direction)
                setUniform(gpu.uniform("$prefix.innerCutOff"), light.innerCutOff)
                setUniform(gpu.uniform("$prefix.outerCutOff"), light.outerCutOff)
            } else {
                setUniform(gpu.uniform("$prefix.type"), 0)
                setUniform(gpu.uniform("$prefix.direction"), Vector3f(0f, -1f, 0f))
                setUniform(gpu.uniform("$prefix.innerCutOff"), 1f)
                setUniform(gpu.uniform("$prefix.outerCutOff"), 0f)
            }
        }
    }

    fun clear() {
        resources?.delete()
        resources = null
    }

    private class Resources {
        val program = ShaderProgram("grass.vert", "grass.frag")
        val grassTexture = TextureLoader.loadTexture(
            "assets/texture/foliage/grass.png",
            TexturePresets.FOLIAGE,
        )
        val geometries = FoliageType.entries.associateWith(::Geometry)
        val patches = mutableMapOf<GrassPatch, PatchMesh>()
        private val uniforms = mutableMapOf<String, Int>()

        init {
            check(glGetProgrami(program.id, GL_LINK_STATUS) == GL_TRUE) { "Grass shader failed to link" }
        }

        fun uniform(name: String): Int = uniforms.getOrPut(name) { glGetUniformLocation(program.id, name) }

        fun delete() {
            patches.values.forEach { it.delete() }
            patches.clear()
            geometries.values.forEach { it.delete() }
            grassTexture.delete()
            program.delete()
        }
    }

    private class Geometry(type: FoliageType) {
        val vertices = VertexBufferObject()
        val vertexCount: Int

        init {
            val data = when (type) {
                FoliageType.GRASS -> createGrassVertices()
                FoliageType.FLOWERS -> createFlowerVertices()
            }
            vertexCount = data.size / VERTEX_FLOATS
            vertices.bind()
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
        }

        fun delete() = vertices.delete()
    }

    private class PatchMesh(patch: GrassPatch, vertices: VertexBufferObject) {
        val vao = VertexArrayObject()
        private val instances = VertexBufferObject()

        init {
            vao.bind()
            vertices.bind()
            val vertexStride = VERTEX_FLOATS * Float.SIZE_BYTES
            glVertexAttribPointer(0, 3, GL_FLOAT, false, vertexStride, 0L)
            glVertexAttribPointer(1, 1, GL_FLOAT, false, vertexStride, 3L * Float.SIZE_BYTES)
            glVertexAttribPointer(2, 3, GL_FLOAT, false, vertexStride, 4L * Float.SIZE_BYTES)
            glVertexAttribPointer(5, 2, GL_FLOAT, false, vertexStride, 7L * Float.SIZE_BYTES)

            val data = FloatArray(patch.instances.size * 7)
            patch.instances.forEachIndexed { index, instance ->
                val offset = index * 7
                data[offset] = instance.position.x
                data[offset + 1] = instance.position.y
                data[offset + 2] = instance.position.z
                data[offset + 3] = instance.scale
                data[offset + 4] = instance.rotation
                data[offset + 5] = instance.phase
                data[offset + 6] = instance.tint
            }
            instances.bind()
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
            val instanceStride = 7 * Float.SIZE_BYTES
            glVertexAttribPointer(3, 3, GL_FLOAT, false, instanceStride, 0L)
            glVertexAttribPointer(4, 4, GL_FLOAT, false, instanceStride, 3L * Float.SIZE_BYTES)
            glVertexAttribDivisor(3, 1)
            glVertexAttribDivisor(4, 1)
            for (attribute in 0..5) glEnableVertexAttribArray(attribute)
            glBindVertexArray(0)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
        }

        fun delete() {
            vao.delete()
            instances.delete()
        }
    }

    private companion object {
        const val VERTEX_FLOATS = 9
        const val MAX_LOCAL_LIGHTS = 30
        const val FOLIAGE_TEXTURE_UNIT = 0
        const val SHADOW_TEXTURE_UNIT = 1

        val GRASS_COLORS = listOf(
            Vector3f(0.25f, 0.62f, 0.10f),
            Vector3f(0.34f, 0.72f, 0.12f),
            Vector3f(0.43f, 0.80f, 0.15f),
            Vector3f(0.20f, 0.53f, 0.08f),
        )

        fun createGrassVertices(): FloatArray {
            val data = ArrayList<Float>(2 * 12 * VERTEX_FLOATS)
            val halfWidth = 0.48f
            val height = 1.12f
            val color = Vector3f(1f, 1f, 1f)
            repeat(2) { card ->
                val angle = card * 1.5708f
                val side = Vector3f(cos(angle) * halfWidth, 0f, sin(angle) * halfWidth)
                val bottomLeft = side.negate()
                val bottomRight = side
                val middleLeft = Vector3f(-side.x, height * 0.55f, -side.z)
                val middleRight = Vector3f(side.x, height * 0.55f, side.z)
                val topLeft = Vector3f(-side.x, height, -side.z)
                val topRight = Vector3f(side.x, height, side.z)
                addTexturedQuad(data, bottomLeft, bottomRight, middleRight, middleLeft, 0f, 0.45f, 1f, 0.45f, color)
                addTexturedQuad(data, middleLeft, middleRight, topRight, topLeft, 0.45f, 1f, 0.45f, 0f, color)
            }
            return data.toFloatArray()
        }

        fun createFlowerVertices(): FloatArray {
            val data = ArrayList<Float>(420)
            val layouts = listOf(
                FlowerLayout(-0.24f, -0.08f, 0.72f, Vector3f(0.95f, 0.91f, 0.76f)),
                FlowerLayout(0.08f, 0.04f, 0.88f, Vector3f(0.40f, 0.63f, 0.94f)),
                FlowerLayout(0.27f, -0.03f, 0.66f, Vector3f(1.00f, 0.70f, 0.12f)),
            )
            val stemColor = Vector3f(0.23f, 0.52f, 0.09f)
            val leafColor = Vector3f(0.31f, 0.64f, 0.11f)
            val centerColor = Vector3f(0.98f, 0.57f, 0.06f)
            layouts.forEachIndexed { flowerIndex, flower ->
                val base = Vector3f(flower.x, 0f, flower.z)
                val top = Vector3f(flower.x + if (flowerIndex == 1) 0.035f else -0.025f, flower.height, flower.z)
                addQuad(data,
                    Vector3f(base.x - 0.018f, 0f, base.z), Vector3f(base.x + 0.018f, 0f, base.z),
                    Vector3f(top.x + 0.012f, top.y, top.z), Vector3f(top.x - 0.012f, top.y, top.z),
                    0f, 1f, stemColor)
                addQuad(data,
                    Vector3f(base.x, 0f, base.z - 0.018f), Vector3f(base.x, 0f, base.z + 0.018f),
                    Vector3f(top.x, top.y, top.z + 0.012f), Vector3f(top.x, top.y, top.z - 0.012f),
                    0f, 1f, stemColor)

                val leafY = flower.height * 0.42f
                addTriangle(data,
                    Vector3f(flower.x, leafY - 0.06f, flower.z), 0.35f,
                    Vector3f(flower.x + if (flowerIndex % 2 == 0) 0.20f else -0.20f, leafY + 0.06f, flower.z + 0.025f), 0.55f,
                    Vector3f(flower.x, leafY + 0.04f, flower.z), 0.48f,
                    leafColor)

                val petalRadius = if (flowerIndex == 1) 0.14f else 0.12f
                repeat(5) { petal ->
                    val angle = petal * 1.25664f + flowerIndex * 0.31f
                    val halfWidth = 0.38f
                    addTriangle(data,
                        top, 1f,
                        Vector3f(top.x + cos(angle - halfWidth) * petalRadius, top.y + 0.018f,
                            top.z + sin(angle - halfWidth) * petalRadius), 1f,
                        Vector3f(top.x + cos(angle + halfWidth) * petalRadius, top.y + 0.018f,
                            top.z + sin(angle + halfWidth) * petalRadius), 1f,
                        flower.color)
                }
                val centerTop = Vector3f(top.x, top.y + 0.035f, top.z)
                repeat(4) { side ->
                    val angleA = side * 1.5708f
                    val angleB = (side + 1) * 1.5708f
                    addTriangle(data, centerTop, 1f,
                        Vector3f(top.x + cos(angleA) * 0.035f, top.y + 0.02f, top.z + sin(angleA) * 0.035f), 1f,
                        Vector3f(top.x + cos(angleB) * 0.035f, top.y + 0.02f, top.z + sin(angleB) * 0.035f), 1f,
                        centerColor)
                }
            }
            return data.toFloatArray()
        }

        fun addQuad(
            data: MutableList<Float>,
            bottomLeft: Vector3f,
            bottomRight: Vector3f,
            topRight: Vector3f,
            topLeft: Vector3f,
            bottomWeight: Float,
            topWeight: Float,
            color: Vector3f,
        ) {
            addTriangle(data, bottomLeft, bottomWeight, bottomRight, bottomWeight, topRight, topWeight, color)
            addTriangle(data, bottomLeft, bottomWeight, topRight, topWeight, topLeft, topWeight, color)
        }

        fun addTriangle(
            data: MutableList<Float>,
            first: Vector3f,
            firstWeight: Float,
            second: Vector3f,
            secondWeight: Float,
            third: Vector3f,
            thirdWeight: Float,
            color: Vector3f,
        ) {
            addVertex(data, first, firstWeight, color)
            addVertex(data, second, secondWeight, color)
            addVertex(data, third, thirdWeight, color)
        }

        fun addVertex(data: MutableList<Float>, position: Vector3f, bendWeight: Float, color: Vector3f) {
            addVertex(data, position, bendWeight, color, 0f, 0f)
        }

        fun addTexturedQuad(
            data: MutableList<Float>,
            bottomLeft: Vector3f,
            bottomRight: Vector3f,
            topRight: Vector3f,
            topLeft: Vector3f,
            bottomWeight: Float,
            topWeight: Float,
            bottomV: Float,
            topV: Float,
            color: Vector3f,
        ) {
            addVertex(data, bottomLeft, bottomWeight, color, 0f, bottomV)
            addVertex(data, bottomRight, bottomWeight, color, 1f, bottomV)
            addVertex(data, topRight, topWeight, color, 1f, topV)
            addVertex(data, bottomLeft, bottomWeight, color, 0f, bottomV)
            addVertex(data, topRight, topWeight, color, 1f, topV)
            addVertex(data, topLeft, topWeight, color, 0f, topV)
        }

        fun addVertex(
            data: MutableList<Float>,
            position: Vector3f,
            bendWeight: Float,
            color: Vector3f,
            u: Float,
            v: Float,
        ) {
            data.add(position.x)
            data.add(position.y)
            data.add(position.z)
            data.add(bendWeight)
            data.add(color.x)
            data.add(color.y)
            data.add(color.z)
            data.add(u)
            data.add(v)
        }

        data class FlowerLayout(val x: Float, val z: Float, val height: Float, val color: Vector3f)
    }
}
