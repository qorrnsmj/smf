package qorrnsmj.smf.graphic.text

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.util.Cleanable

/**
 * Manages a texture atlas containing rendered glyphs for a font.
 * Handles dynamic expansion and OpenGL texture operations.
 */
class GlyphAtlas(
    private var initialWidth: Int = 512,
    private var initialHeight: Int = 512
) : Cleanable {

    private var textureId: Int = 0
    private var width: Int = initialWidth
    private var height: Int = initialHeight
    private var currentX: Int = 0
    private var currentY: Int = 0
    private var rowHeight: Int = 0

    init {
        createTexture()
    }

    private fun createTexture() {
        textureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureId)

        // Create empty texture with red format (for alpha channel storage)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, 0)

        // Set texture parameters for crisp text rendering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glBindTexture(GL_TEXTURE_2D, 0)
    }

    /**
     * Add a glyph bitmap to the atlas and return its coordinates
     */
    fun addGlyph(bitmap: ByteArray, glyphWidth: Int, glyphHeight: Int): Pair<Int, Int> {
        // Check if we need to move to next row
        if (currentX + glyphWidth > width) {
            currentX = 0
            currentY += rowHeight
            rowHeight = 0
        }

        // Check if we need to expand the texture vertically
        if (currentY + glyphHeight > height) {
            expandTexture()
        }

        val x = currentX
        val y = currentY

        // Upload glyph bitmap to texture
        glBindTexture(GL_TEXTURE_2D, textureId)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        val buffer = org.lwjgl.BufferUtils.createByteBuffer(bitmap.size)
        buffer.put(bitmap).flip()
        glTexSubImage2D(
            GL_TEXTURE_2D, 0,
            x, y,
            glyphWidth, glyphHeight,
            GL_RED, GL_UNSIGNED_BYTE,
            buffer
        )
        glBindTexture(GL_TEXTURE_2D, 0)

        // Update position for next glyph
        currentX += glyphWidth
        rowHeight = maxOf(rowHeight, glyphHeight)

        return Pair(x, y)
    }

    private fun expandTexture() {
        val oldTextureId = textureId
        val newHeight = height * 2

        // Create new larger texture
        val newTextureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, newTextureId)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, newHeight, 0, GL_RED, GL_UNSIGNED_BYTE, 0)

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        // Copy old texture data to new texture using framebuffer blit
        val fbo = glGenFramebuffers()
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo)
        glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, oldTextureId, 0)

        glBindTexture(GL_TEXTURE_2D, newTextureId)
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height)

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        glDeleteFramebuffers(fbo)

        // Replace old texture
        glDeleteTextures(oldTextureId)
        textureId = newTextureId
        height = newHeight

        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun getTextureId(): Int = textureId
    fun getWidth(): Int = width
    fun getHeight(): Int = height

    override fun cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId)
            textureId = 0
        }
    }
}
