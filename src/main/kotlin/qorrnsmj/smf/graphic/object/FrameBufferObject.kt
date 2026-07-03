package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger

class FrameBufferObject(val width: Int, val height: Int) : Object() {
    override val id = glGenFramebuffers()
    val colorTexture = TextureBufferObject()
    private val depthRenderBuffer = glGenRenderbuffers()

    init {
        try {
            colorTexture.bind()
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBuffer)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height)
            glBindRenderbuffer(GL_RENDERBUFFER, 0)

            bind()
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture.id, 0)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBuffer)
            glDrawBuffer(GL_COLOR_ATTACHMENT0)
            glReadBuffer(GL_COLOR_ATTACHMENT0)

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            check(status == GL_FRAMEBUFFER_COMPLETE) { "Frame-buffer is not complete: $status" }
            bindDefault()
        } catch (e: Exception) {
            Logger.error(e)
            delete()
        }
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
    }

    fun bindDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun delete() {
        colorTexture.delete()
        glDeleteRenderbuffers(depthRenderBuffer)
        glDeleteFramebuffers(id)
    }
}