package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger

class LocalShadowFrameBuffer(
    val width: Int,
    val height: Int,
    val layers: Int,
) : Object() {
    override val id: Int = glGenFramebuffers()
    val depthTextureId: Int = glGenTextures()

    init {
        try {
            glBindTexture(GL_TEXTURE_2D_ARRAY, depthTextureId)
            glTexImage3D(
                GL_TEXTURE_2D_ARRAY,
                0,
                GL_DEPTH_COMPONENT24,
                width,
                height,
                layers,
                0,
                GL_DEPTH_COMPONENT,
                GL_FLOAT,
                0,
            )
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
            MemoryStack.stackPush().use {
                val borderColor = it.floats(1f, 1f, 1f, 1f)
                glTexParameterfv(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BORDER_COLOR, borderColor)
            }

            bindLayer(0)
            glDrawBuffer(GL_NONE)
            glReadBuffer(GL_NONE)

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            check(status == GL_FRAMEBUFFER_COMPLETE) { "Local shadow frame-buffer is not complete: $status" }
            bindDefault()
            glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
        } catch (e: Exception) {
            Logger.error(e)
            delete()
        }
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
    }

    fun bindLayer(layer: Int) {
        bind()
        glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthTextureId, 0, layer)
    }

    fun bindDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun delete() {
        glDeleteTextures(depthTextureId)
        glDeleteFramebuffers(id)
    }
}
