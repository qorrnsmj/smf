package qorrnsmj.smf.graphic.resource.buffer

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.Object

class TextureBufferObject() : Object() {
    override val id = glGenTextures()
    val fileName = ""

    override fun bind() {
        glBindTexture(GL_TEXTURE_2D, id)
    }

    override fun delete() {
        glDeleteTextures(id)
    }

    override fun toString(): String {
        return fileName
    }
}
