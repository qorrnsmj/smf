package qorrnsmj.smf.graphic.resource.buffer

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.Object

class VertexArrayObject : Object() {
    override val id = glGenVertexArrays()

    override fun bind() {
        glBindVertexArray(id)
    }

    override fun delete() {
        glDeleteVertexArrays(id)
    }
}
