package qorrnsmj.smf.editor

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL33C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_FLOAT
import org.lwjgl.opengl.GL33C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL33C.glBindBuffer
import org.lwjgl.opengl.GL33C.glBindVertexArray
import org.lwjgl.opengl.GL33C.glBufferData
import org.lwjgl.opengl.GL33C.glBufferSubData
import org.lwjgl.opengl.GL33C.glDeleteBuffers
import org.lwjgl.opengl.GL33C.glDeleteVertexArrays
import org.lwjgl.opengl.GL33C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL33C.glGenBuffers
import org.lwjgl.opengl.GL33C.glGenVertexArrays
import org.lwjgl.opengl.GL33C.glVertexAttribPointer
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.graphic.terrain.component.SingleTexture
import qorrnsmj.smf.graphic.terrain.component.TerrainMaterial
import qorrnsmj.smf.graphic.terrain.component.TerrainMesh
import qorrnsmj.smf.graphic.terrain.component.TerrainModel
import qorrnsmj.smf.graphic.render.EditorDebugLine
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

internal class EditorTerrainPreview(
    private val source: EditorTerrainData,
    initialMapSize: Float,
) {
    private val resolution = 257
    private var terrainSize = initialMapSize
    private val wireStep = 8
    private val positions = FloatArray(resolution * resolution * 3)
    private val texCoords = FloatArray(resolution * resolution * 2)
    private val normals = FloatArray(resolution * resolution * 3)
    private val indices = IntArray(6 * (resolution - 1) * (resolution - 1))
    private val previewHeights = Array(resolution) { FloatArray(resolution) }
    private val vao = glGenVertexArrays()
    private val positionVbo = glGenBuffers()
    private val texCoordVbo = glGenBuffers()
    private val normalVbo = glGenBuffers()
    private val ebo = glGenBuffers()
    private var wireframeCache = emptyList<EditorDebugLine>()
    private var wireframeDirty = true

    val terrain: Terrain

    init {
        buildStaticBuffers()
        terrain = Terrain(
            model = TerrainModel(
                mesh = TerrainMesh(
                    vao = vao,
                    vertexCount = indices.size,
                    size = Vector2f(terrainSize, terrainSize),
                    gridResolution = resolution,
                    heights = previewHeights,
                ),
                material = TerrainMaterial(SingleTexture(Textures.TERRAIN_GRASS)),
            ),
            position = Vector3f(-terrainSize * 0.5f, 0f, -terrainSize * 0.5f),
        )
        update()
    }

    fun setMapSize(size: Float) {
        terrainSize = size.coerceAtLeast(1f)
        terrain.model.mesh.size.x = terrainSize
        terrain.model.mesh.size.y = terrainSize
        terrain.position.x = -terrainSize * 0.5f
        terrain.position.z = -terrainSize * 0.5f
        update()
    }

    fun update() {
        fillDynamicBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, positionVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, positions)
        glBindBuffer(GL_ARRAY_BUFFER, normalVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, normals)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        wireframeDirty = true
    }

    fun dispose() {
        glDeleteBuffers(positionVbo)
        glDeleteBuffers(texCoordVbo)
        glDeleteBuffers(normalVbo)
        glDeleteBuffers(ebo)
        glDeleteVertexArrays(vao)
    }

    fun wireframeLines(): List<EditorDebugLine> {
        if (!wireframeDirty) return wireframeCache
        val sampledResolution = ((resolution - 1) / wireStep) + 1
        val lines = ArrayList<EditorDebugLine>(sampledResolution * sampledResolution * 2)
        val color = Vector4f(0.05f, 0.95f, 1f, 0.78f)
        val samples = (0 until resolution step wireStep).toMutableList()
        if (samples.last() != resolution - 1) samples.add(resolution - 1)
        for (z in samples) {
            for (x in samples) {
                val nextX = samples.firstOrNull { it > x }
                val nextZ = samples.firstOrNull { it > z }
                if (nextX != null) lines.add(EditorDebugLine(vertexAt(x, z), vertexAt(nextX, z), color))
                if (nextZ != null) lines.add(EditorDebugLine(vertexAt(x, z), vertexAt(x, nextZ), color))
            }
        }
        wireframeCache = lines
        wireframeDirty = false
        return wireframeCache
    }

    private fun buildStaticBuffers() {
        fillTexCoords()
        fillIndices()

        glBindVertexArray(vao)

        glBindBuffer(GL_ARRAY_BUFFER, positionVbo)
        glBufferData(GL_ARRAY_BUFFER, positions.size * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)

        glBindBuffer(GL_ARRAY_BUFFER, texCoordVbo)
        val texCoordBuffer = BufferUtils.createFloatBuffer(texCoords.size)
        texCoordBuffer.put(texCoords).flip()
        glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(1)

        glBindBuffer(GL_ARRAY_BUFFER, normalVbo)
        glBufferData(GL_ARRAY_BUFFER, normals.size * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(2)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indexBuffer = BufferUtils.createIntBuffer(indices.size)
        indexBuffer.put(indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun fillTexCoords() {
        var vertex = 0
        for (z in 0 until resolution) {
            for (x in 0 until resolution) {
                texCoords[vertex * 2] = x.toFloat() / (resolution - 1)
                texCoords[vertex * 2 + 1] = z.toFloat() / (resolution - 1)
                vertex++
            }
        }
    }

    private fun fillIndices() {
        var pointer = 0
        for (z in 0 until resolution - 1) {
            for (x in 0 until resolution - 1) {
                val topLeft = z * resolution + x
                val topRight = topLeft + 1
                val bottomLeft = (z + 1) * resolution + x
                val bottomRight = bottomLeft + 1

                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }
    }

    private fun fillDynamicBuffers() {
        var vertex = 0
        for (z in 0 until resolution) {
            for (x in 0 until resolution) {
                val sourceX = ((x.toFloat() / (resolution - 1)) * (source.width - 1)).toInt()
                val sourceY = ((z.toFloat() / (resolution - 1)) * (source.height - 1)).toInt()
                val height = source.get(sourceX, sourceY) * CM_TO_METERS

                positions[vertex * 3] = x.toFloat() / (resolution - 1) * terrainSize
                positions[vertex * 3 + 1] = height
                positions[vertex * 3 + 2] = z.toFloat() / (resolution - 1) * terrainSize
                previewHeights[x][z] = height

                normals[vertex * 3] = 0f
                normals[vertex * 3 + 1] = 1f
                normals[vertex * 3 + 2] = 0f
                vertex++
            }
        }
    }

    private fun vertexAt(x: Int, z: Int): Vector3f {
        val vertex = z * resolution + x
        return Vector3f(
            positions[vertex * 3] + terrain.position.x,
            positions[vertex * 3 + 1] + terrain.position.y,
            positions[vertex * 3 + 2] + terrain.position.z,
        )
    }

    private companion object {
        const val CM_TO_METERS = 0.01f
    }
}
