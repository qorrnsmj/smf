package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger

class ShadowFrameBuffer(val width: Int, val height: Int) : Object() {
    override val id: Int = glGenFramebuffers()
    val depthTexture = TextureBufferObject()

    init {
        try {
            depthTexture.bind()
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
            MemoryStack.stackPush().use {
                val borderColor = it.floats(1f, 1f, 1f, 1f)
                glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor)
            }

            bind()
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture.id, 0)
            glDrawBuffer(GL_NONE)
            glReadBuffer(GL_NONE)

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            check(status == GL_FRAMEBUFFER_COMPLETE) { "Shadow frame-buffer is not complete: $status" }
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
        depthTexture.delete()
        glDeleteFramebuffers(id)
    }
}
