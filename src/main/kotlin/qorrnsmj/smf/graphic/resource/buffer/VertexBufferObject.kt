package qorrnsmj.smf.graphic.resource.buffer

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.Object

class VertexBufferObject : Object() {
    override val id = glGenBuffers()

    override fun bind() {
        glBindBuffer(GL_ARRAY_BUFFER, id)
    }

    override fun delete() {
        glDeleteBuffers(id)
    }
}
