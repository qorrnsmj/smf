package qorrnsmj.smf.game.map

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_FLOAT
import org.lwjgl.opengl.GL33C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL33C.glBindBuffer
import org.lwjgl.opengl.GL33C.glBindVertexArray
import org.lwjgl.opengl.GL33C.glBufferData
import org.lwjgl.opengl.GL33C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL33C.glGenBuffers
import org.lwjgl.opengl.GL33C.glVertexAttribPointer
import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.VertexArrayObject
import qorrnsmj.smf.math.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object MapMeshLoader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()

    fun load(brushes: List<MapBrush>): Map<String, Mesh> {
        val meshes = mutableMapOf<String, MeshData>()

        for (brush in brushes) {
            appendBrush(brush, meshes)
        }

        return meshes.mapValues { (_, meshData) ->
            loadMeshInternal(
                positions = meshData.positions.toFloatArray(),
                texCoords = meshData.texCoords.toFloatArray(),
                normals = meshData.normals.toFloatArray(),
                indices = meshData.indices.toIntArray(),
            )
        }
    }

    private fun appendBrush(
        brush: MapBrush,
        meshes: MutableMap<String, MeshData>,
    ) {
        for (face in brush.faces) {
            val faceVertices = MapGeometryBuilder.buildFaceVertices(brush, face)
            if (faceVertices.size >= 3) {
                val meshData = meshes.getOrPut(face.texture.lowercase()) { MeshData() }
                appendFace(face, faceVertices, meshData.positions, meshData.texCoords, meshData.normals, meshData.indices)
            }
        }
    }

    private fun appendFace(
        face: MapBrushFace,
        vertices: List<Vector3f>,
        positions: MutableList<Float>,
        texCoords: MutableList<Float>,
        normals: MutableList<Float>,
        indices: MutableList<Int>,
    ) {
        val startIndex = positions.size / 3
        val normal = MapGeometryBuilder.toPlane(face).normal

        for (vertex in vertices) {
            positions.add(vertex.x)
            positions.add(vertex.y)
            positions.add(vertex.z)
            val uv = calculateUv(face, vertex)
            texCoords.add(uv.first)
            texCoords.add(uv.second)
            normals.add(normal.x)
            normals.add(normal.y)
            normals.add(normal.z)
        }

        for (index in 1 until vertices.lastIndex) {
            indices.add(startIndex)
            indices.add(startIndex + index)
            indices.add(startIndex + index + 1)
        }
    }

    private fun calculateUv(face: MapBrushFace, vertex: Vector3f): Pair<Float, Float> {
        val plane = MapGeometryBuilder.toPlane(face)
        val axes = textureAxes(plane.normal)
        val radians = Math.toRadians(face.textureRotation.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()

        val projectedU = vertex.dot(axes.first)
        val projectedV = vertex.dot(axes.second)
        val rotatedU = projectedU * cos - projectedV * sin
        val rotatedV = projectedU * sin + projectedV * cos

        return Pair(
            (rotatedU + face.textureShiftX) / (64f * face.textureScaleX),
            (rotatedV + face.textureShiftY) / (64f * face.textureScaleY),
        )
    }

    private fun textureAxes(normal: Vector3f): Pair<Vector3f, Vector3f> {
        val absX = abs(normal.x)
        val absY = abs(normal.y)
        val absZ = abs(normal.z)

        return when {
            absY >= absX && absY >= absZ -> Pair(Vector3f(1f, 0f, 0f), Vector3f(0f, 0f, -1f))
            absX >= absZ -> Pair(Vector3f(0f, 0f, 1f), Vector3f(0f, -1f, 0f))
            else -> Pair(Vector3f(1f, 0f, 0f), Vector3f(0f, -1f, 0f))
        }
    }

    private fun loadMeshInternal(
        positions: FloatArray,
        texCoords: FloatArray,
        normals: FloatArray,
        indices: IntArray,
    ): Mesh {
        val vao = VertexArrayObject()
        vaos.add(vao.id)
        vao.bind()

        bindVbo(0, 3, positions)
        bindVbo(1, 2, texCoords)
        bindVbo(2, 3, normals)
        bindEbo(indices)

        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return Mesh(
            vao = vao.id,
            vertexCount = indices.size,
        )
    }

    private fun bindVbo(attribIndex: Int, attribSize: Int, data: FloatArray) {
        val vbo = glGenBuffers()
        vbos.add(vbo)

        val buffer = BufferUtils.createFloatBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(attribIndex, attribSize, GL_FLOAT, false, 0, 0)
    }

    private fun bindEbo(data: IntArray) {
        val ebo = glGenBuffers()
        ebos.add(ebo)

        val buffer = BufferUtils.createIntBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
    }

    private data class MeshData(
        val positions: MutableList<Float> = mutableListOf(),
        val texCoords: MutableList<Float> = mutableListOf(),
        val normals: MutableList<Float> = mutableListOf(),
        val indices: MutableList<Int> = mutableListOf(),
    )
}
