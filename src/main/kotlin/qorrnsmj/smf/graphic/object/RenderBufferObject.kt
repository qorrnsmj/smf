package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33.*

class RenderBufferObject() : Object() {
    override val id = glGenRenderbuffers()

    override fun bind() {
        glBindRenderbuffer(GL_RENDERBUFFER, id)
    }

    override fun unbind() {
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
    }

    override fun delete() {
        glDeleteRenderbuffers(id)
    }
}
