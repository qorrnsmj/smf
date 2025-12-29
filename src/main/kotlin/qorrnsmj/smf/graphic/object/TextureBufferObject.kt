package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*

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
