package qorrnsmj.smf.game.terrain

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.game.terrain.custom.FlatTerrain
import qorrnsmj.smf.game.terrain.custom.FlatTerrain.Companion.SIZE
import qorrnsmj.smf.game.terrain.custom.FlatTerrain.Companion.VERTEX_COUNT
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.graphic.`object`.VertexArrayObject

object TerrainLoader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<TextureBufferObject>()

    // TODO: EntityLoaderみたいに綺麗にまとめる
    fun loadModel(terrain: FlatTerrain): TerrainModel {
        val count = VERTEX_COUNT * VERTEX_COUNT
        val positions = FloatArray(count * 3)
        val texCoords = FloatArray(count * 2)
        val normals = FloatArray(count * 3)
        val indices = IntArray(6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT - 1))
        var vertexPointer = 0

        // 頂点座標、法線、テクスチャ座標の計算
        for (i in 0 until VERTEX_COUNT) {
            for (j in 0 until VERTEX_COUNT) {
                positions[vertexPointer * 3] = j.toFloat() / (VERTEX_COUNT - 1) * SIZE
                positions[vertexPointer * 3 + 1] = 0f
                positions[vertexPointer * 3 + 2] = i.toFloat() / (VERTEX_COUNT - 1) * SIZE

                texCoords[vertexPointer * 2] = j.toFloat() / (VERTEX_COUNT - 1)
                texCoords[vertexPointer * 2 + 1] = i.toFloat() / (VERTEX_COUNT - 1)

                normals[vertexPointer * 3] = 0f
                normals[vertexPointer * 3 + 1] = 1f
                normals[vertexPointer * 3 + 2] = 0f

                vertexPointer++
            }
        }

        // インデックス配列の計算
        var pointer = 0
        for (gz in 0 until VERTEX_COUNT - 1) {
            for (gx in 0 until VERTEX_COUNT - 1) {
                val topLeft = (gz * VERTEX_COUNT) + gx
                val topRight = topLeft + 1
                val bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx
                val bottomRight = bottomLeft + 1

                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }

        val mesh = loadMesh(positions, texCoords, normals, indices)
        val material = TerrainMaterial.createDefault()
        val model = TerrainModel("terrain", mesh, material)

        val faceCount = mesh.vertexCount.div(3)
        Logger.info("\"\" loaded ($faceCount faces)")

        return model
    }

    private fun loadMesh(positions: FloatArray, texCoords: FloatArray, normals: FloatArray, indices: IntArray): Mesh {
        val vao = VertexArrayObject()
        vaos.add(vao.id)
        vao.bind()

        // bind
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        //bindVBO(2, 3, normals)
        bindEBO(indices)

        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        //glEnableVertexAttribArray(2)

        // unbind
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return Mesh(vao.id, indices.size)
    }
    
    private fun bindVBO(attribIndex: Int, attribSize: Int, data: FloatArray) {
        val vboID = glGenBuffers()
        vbos.add(vboID)

        val buffer = BufferUtils.createFloatBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(attribIndex, attribSize, GL_FLOAT, false, 0, 0)
    }

    private fun bindEBO(data: IntArray) {
        val eboID = glGenBuffers()
        ebos.add(eboID)

        val buffer = BufferUtils.createIntBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
    }

    fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
