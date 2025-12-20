package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.`object`.FrameBufferObject
import qorrnsmj.smf.graphic.`object`.VertexArrayObject
import qorrnsmj.smf.graphic.`object`.VertexBufferObject
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.util.impl.Resizable

class PostProcessor : Resizable {
    private var width = 0
    private var height = 0
    private lateinit var inFbo: FrameBufferObject
    private lateinit var outFbo: FrameBufferObject
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
    }

    fun bindFrameBuffer() {
        inFbo.bind()
    }

    fun bindDefaultFrameBuffer() {
        inFbo.bindDefault()
    }

    fun applyPostProcess(effects: List<Effect>) {
        // Apply each effect except the last one
        for (effect in effects.dropLast(1)) {
            if (effect is Resizable) {
                effect.resize(width, height)
            }

            outFbo.bind()
            effect.use()
            renderScreenQuad()
            effect.unuse()

            val tmp = inFbo
            inFbo = outFbo
            outFbo = tmp
        }

        val lastEffect = effects.last()
        if (lastEffect is Resizable) {
            lastEffect.resize(width, height)
        }

        // Bind default FBO
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        // Apply the last effect
        lastEffect.use()
        renderScreenQuad()
        lastEffect.unuse()
    }

    private fun renderScreenQuad() {
        quadVao.bind()
        inFbo.colorTexture.bind()

        // Render the screen quad
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    /* Misc */

    fun cleanup() {
        inFbo.delete()
        outFbo.delete()
        quadVbo.delete()
        quadVao.delete()

        Logger.info("PostProcessor cleaned up!")
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height

        inFbo = FrameBufferObject(width, height)
        outFbo = FrameBufferObject(width, height)
    }
}
