package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger

class FrameBufferObject(val width: Int, val height: Int) : Object() {
    override val id = glGenFramebuffers()
    val colorTexture = TextureBufferObject()
    val depthTexture = TextureBufferObject()
    val colorBuffer = RenderBufferObject()
    val depthBuffer = RenderBufferObject()

    init {
        try {
            colorTexture.bind()
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            colorTexture.unbind()

            depthTexture.bind()
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            depthTexture.unbind()

            colorBuffer.bind()
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, width, height)
            colorBuffer.unbind()

            depthBuffer.bind()
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height)
            depthBuffer.unbind()

            bind()
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture.id, 0)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture.id, 0)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_RENDERBUFFER, colorBuffer.id)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer.id)
            unbind()

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            check (status == GL_FRAMEBUFFER_COMPLETE) { "Frame-buffer is not complete" }
        } catch (e: Exception) {
            Logger.error(e)
            delete()
        }
    }

    override fun bind() {
        //glActiveTexture(GL_TEXTURE0 + unit)
        glBindFramebuffer(GL_FRAMEBUFFER, id)
    }

    override fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun delete() {
        colorTexture.delete()
        depthTexture.delete()
        colorBuffer.delete()
        depthBuffer.delete()
        glDeleteFramebuffers(id)
    }
}
