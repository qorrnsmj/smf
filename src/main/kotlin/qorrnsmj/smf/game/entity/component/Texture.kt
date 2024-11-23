package qorrnsmj.smf.game.entity.component

import org.lwjgl.opengl.GL33.*

class Texture(val id: Int) {
    fun bind() {
        glBindTexture(GL_TEXTURE_2D, id)
    }

    fun unbind() {
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun delete() {
        glDeleteTextures(id)
    }
}
