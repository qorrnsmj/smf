package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*

class TextureBufferObject() : Object() {
    override val id = glGenTextures()

    override fun bind() {
        glBindTexture(GL_TEXTURE_2D, id)
    }

    override fun delete() {
        glDeleteTextures(id)
    }
}
