package qorrnsmj.test.t11.core.model

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.`object`.Mesh

object Loader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<Texture>()

    fun loadMesh(positions: FloatArray, texCoords: FloatArray, normals: FloatArray, indices: IntArray): Mesh {
        val vaoID = glGenVertexArrays()
        vaos.add(vaoID)

        // bind
        glBindVertexArray(vaoID)
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        bindVBO(2, 3, normals)
        bindEBO(indices)

        // unbind
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return Mesh(vaoID, indices.size)
    }

    // TODO: テクスチャクラスに追加するものがなかったらテクスチャクラスの中身をここに書く
    // TODO: 法線マップとかテクスチャクラスに書く？
    fun loadTexture(imageFile: String): Texture {
        val texture = Texture(imageFile)
        textures.add(texture)

        return texture
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

    fun cleanUp() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
