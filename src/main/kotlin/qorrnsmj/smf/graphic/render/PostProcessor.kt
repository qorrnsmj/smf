package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.`object`.FrameBufferObject
import qorrnsmj.smf.graphic.`object`.VertexArrayObject
import qorrnsmj.smf.graphic.`object`.VertexBufferObject
import qorrnsmj.smf.graphic.render.effect.ContrastEffect
import qorrnsmj.smf.graphic.render.effect.Effect
import qorrnsmj.smf.util.Resizable

class PostProcessor : Resizable {
    private lateinit var fbo: FrameBufferObject
    private lateinit var tmpFbo: FrameBufferObject
    private val quadVao = VertexArrayObject()
    private val quadVbo = VertexBufferObject()
    private val quadVertices = floatArrayOf(
        -1f, -1f, 0f, 0f, 0f,
        1f, -1f, 0f, 1f, 0f,
        -1f, 1f, 0f, 0f, 1f,
        1f, 1f, 0f, 1f, 1f
    )

    init {
        quadVao.bind()
        quadVbo.bind()

        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 3 * Float.SIZE_BYTES.toLong())
        glEnableVertexAttribArray(1)

        quadVbo.unbind()
        quadVao.unbind()
    }

    fun bindFrameBuffer() {
        fbo.bind()
    }

    fun unbindFrameBuffer() {
        fbo.unbind()
    }

    fun applyPostProcess(effect: Effect) {
        effect.use()
        renderScreenQuad()
        effect.unuse()
    }

    // FIXME
    fun applyEffects(effects: List<Effect>) {
        val testEffects = listOf(ContrastEffect())

        for (effect in testEffects) {
            tmpFbo.bind()
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // Render the effect to the tmpFbo
            effect.use()
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, fbo.colorTexture.id)
            renderScreenQuad()
            effect.unuse()
            tmpFbo.unbind()

            // Swap the frame-buffers
            val tmp = fbo
            fbo = tmpFbo
            tmpFbo = tmp
        }

        // Bind the default frame-buffer and render the final texture
        fbo.unbind()
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fbo.colorTexture.id)
        renderScreenQuad()
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun renderScreenQuad() {
        quadVao.bind()
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fbo.colorTexture.id)

        // Render the screen quad
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glBindTexture(GL_TEXTURE_2D, 0)
        quadVao.unbind()
    }

    /* Misc */

    fun cleanup() {
        fbo.delete()
        tmpFbo.delete()
    }

    override fun resize(width: Int, height: Int) {
        fbo = FrameBufferObject(width, height)
        tmpFbo = FrameBufferObject(width, height)
    }
}
