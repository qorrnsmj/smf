package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33.*

/**
 * Vertex array object class.
 */
class VertexArrayObject {
    /**
     * Stores the handle of the vertex array object.
     */
    val id = glGenVertexArrays()

    /**
     * Bind this vertex array object.
     */
    fun bind(): VertexArrayObject {
        glBindVertexArray(id)
        return this
    }

    /**
     * Unbind this vertex array object.
     */
    fun unbind(): VertexArrayObject {
        glBindVertexArray(0)
        return this
    }

    /**
     * Deletes this vertex array object.
     */
    fun delete(): VertexArrayObject {
        unbind()
        glDeleteVertexArrays(id)
        return this
    }
}
