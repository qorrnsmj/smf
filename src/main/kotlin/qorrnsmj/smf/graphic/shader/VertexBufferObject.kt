package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33.*
import java.nio.FloatBuffer
import java.nio.IntBuffer

class VertexBufferObject {
    val id = glGenBuffers()
    private var target = 0

    /**
     * Binds this VBO with specified target. The target in the tutorial should
     * be `GL_ARRAY_BUFFER` most of the time.
     *
     * @param target Target to bind
     */
    fun bind(target: Int): VertexBufferObject {
        this.target = target
        glBindBuffer(target, id)
        return this
    }

    /**
     * Unbinds this VBO with the current target.
     */
    fun unbind(): VertexBufferObject {
        target = 0
        glBindBuffer(target, 0)
        return this
    }

    /**
     * Deletes this vertex buffer object.
     */
    fun delete() {
        unbind()
        glDeleteBuffers(id)
    }

    /**
     * Upload vertex data to this VBO with specified target, data and usage. The
     * target in the tutorial should be `GL_ARRAY_BUFFER` and usage
     * should be `GL_STATIC_DRAW` most of the time.
     *
     * @param target Target to upload
     * @param data   Buffer with the data to upload
     * @param usage  Usage of the data
     */
    fun uploadData(target: Int, data: FloatBuffer, usage: Int): VertexBufferObject {
        glBufferData(target, data, usage)
        return this
    }

    /**
     * Upload null data to this VBO with specified target, size and usage. The
     * target in the tutorial should be `GL_ARRAY_BUFFER` and usage
     * should be `GL_STATIC_DRAW` most of the time.
     *
     * @param target Target to upload
     * @param size   Size in bytes of the VBO data store
     * @param usage  Usage of the data
     */
    fun uploadData(target: Int, size: Long, usage: Int): VertexBufferObject {
        glBufferData(target, size, usage)
        return this
    }

    /**
     * Upload sub data to this VBO with specified target, offset and data. The
     * target in the tutorial should be `GL_ARRAY_BUFFER` most of the
     * time.
     *
     * @param target Target to upload
     * @param offset Offset where the data should go in bytes
     * @param data   Buffer with the data to upload
     */
    fun uploadSubData(target: Int, offset: Long, data: FloatBuffer): VertexBufferObject {
        glBufferSubData(target, offset, data)
        return this
    }

    /**
     * Upload element data to this EBO with specified target, data and usage.
     * The target in the tutorial should be `GL_ELEMENT_ARRAY_BUFFER`
     * and usage should be `GL_STATIC_DRAW` most of the time.
     *
     * @param target Target to upload
     * @param data   Buffer with the data to upload
     * @param usage  Usage of the data
     */
    fun uploadData(target: Int, data: IntBuffer, usage: Int): VertexBufferObject {
        glBufferData(target, data, usage)
        return this
    }

    fun uploadData(target: Int, data: FloatArray, usage: Int): VertexBufferObject {
        glBufferData(target, data, usage)
        return this
    }
}
