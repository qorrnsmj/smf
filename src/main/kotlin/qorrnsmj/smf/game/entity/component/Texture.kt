package qorrnsmj.smf.game.entity.component

import org.lwjgl.opengl.GL33.*

// TODO: initで this.id = Loader.loadTexture(file) する(モデルとかもこんな感じにする)
data class Texture(val id: Int = 0) {
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
