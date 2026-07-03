package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger

class PointShadowCubeMap(val size: Int) : Object() {
    override val id: Int = glGenFramebuffers()
    val depthTextureId: Int = glGenTextures()

    init {
        try {
            glBindTexture(GL_TEXTURE_CUBE_MAP, depthTextureId)
            for (face in 0 until 6) {
                glTexImage2D(
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                    0,
                    GL_DEPTH_COMPONENT24,
                    size,
                    size,
                    0,
                    GL_DEPTH_COMPONENT,
                    GL_FLOAT,
                    0,
                )
            }
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

            bindFace(0)
            glDrawBuffer(GL_NONE)
            glReadBuffer(GL_NONE)

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            check(status == GL_FRAMEBUFFER_COMPLETE) { "Point shadow frame-buffer is not complete: $status" }
            bindDefault()
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
        } catch (e: Exception) {
            Logger.error(e)
            delete()
        }
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
    }

    fun bindFace(faceIndex: Int) {
        bind()
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_TEXTURE_CUBE_MAP_POSITIVE_X + faceIndex,
            depthTextureId,
            0,
        )
    }

    fun bindDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun delete() {
        glDeleteTextures(depthTextureId)
        glDeleteFramebuffers(id)
    }
}
