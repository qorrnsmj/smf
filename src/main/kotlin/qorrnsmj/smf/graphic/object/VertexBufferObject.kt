package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*

class VertexBufferObject : Object() {
    override val id = glGenBuffers()

    override fun bind() {
        glBindBuffer(GL_ARRAY_BUFFER, id)
    }

    override fun unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    override fun delete() {
        glDeleteBuffers(id)
    }
}
