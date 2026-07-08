package qorrnsmj.smf.graphic.resource.buffer

import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.graphic.resource.Object

class RenderBufferObject() : Object() {
    override val id = glGenRenderbuffers()

    override fun bind() {
        glBindRenderbuffer(GL_RENDERBUFFER, id)
    }

    override fun delete() {
        glDeleteRenderbuffers(id)
    }
}
