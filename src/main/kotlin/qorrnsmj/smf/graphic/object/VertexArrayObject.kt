package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*

class VertexArrayObject : Object() {
    override val id = glGenVertexArrays()

    override fun bind() {
        glBindVertexArray(id)
    }

    override fun delete() {
        glDeleteVertexArrays(id)
    }
}
