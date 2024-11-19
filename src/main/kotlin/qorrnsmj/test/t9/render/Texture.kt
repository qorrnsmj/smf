package qorrnsmj.test.t9.render

import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.util.ResourceUtils

class Texture(file: String) {
    private val id = glGenTextures()
    private lateinit var path: String

    init {
        bind()

        // テクスチャのパラメータを設定
        // GL_TEXTURE_WRAP_S: 横方向のラップ方法
        // GL_TEXTURE_WRAP_T: 縦方向のラップ方法
        // GL_TEXTURE_MIN_FILTER: 縮小時のフィルタリング方法
        // GL_TEXTURE_MAG_FILTER: 拡大時のフィルタリング方法
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST) // GL_LINEAR or GL_NEAREST
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST) // GL_LINEAR or GL_NEAREST

        // OpenGLにテクスチャを設定
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            STBImage.stbi_set_flip_vertically_on_load(true)
            val image = STBImage.stbi_load(path.toString(), width, height, channels, 4)
                ?: throw RuntimeException("Failed to load texture: $path")

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, image)
            STBImage.stbi_image_free(image)
        }

        unbind()
    }

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
