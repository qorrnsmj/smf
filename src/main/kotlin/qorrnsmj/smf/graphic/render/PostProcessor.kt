package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.`object`.FrameBufferObject
import qorrnsmj.smf.graphic.`object`.VertexArrayObject
import qorrnsmj.smf.graphic.`object`.VertexBufferObject
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.util.Resizable

class PostProcessor : Resizable {
    private var width = 0
    private var height = 0
    private var effects: List<Effect> = emptyList()
    private lateinit var inFbo: FrameBufferObject
    private lateinit var outFbo: FrameBufferObject
    private val quadVao = VertexArrayObject()
    private val quadVbo = VertexBufferObject()
    private val quadVertices = floatArrayOf(
        -1f, -1f, 0f, 0f, 0f,
        1f, -1f, 0f, 1f, 0f,
        -1f, 1f, 0f, 0f, 1f,
        1f, 1f, 0f, 1f, 1f,
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
        this.effects = effects

        for (effect in effects.dropLast(1)) {
            outFbo.bind()
            prepareEffect(effect, inFbo)
            effect.use()
            renderScreenQuad()
            effect.unuse()

            val tmp = inFbo
            inFbo = outFbo
            outFbo = tmp
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        val lastEffect = effects.last()
        prepareEffect(lastEffect, inFbo)
        lastEffect.use()
        renderScreenQuad()
        lastEffect.unuse()
    }

    private fun prepareEffect(effect: Effect, source: FrameBufferObject) {
    }

    private fun renderScreenQuad() {
        quadVao.bind()
        glActiveTexture(GL_TEXTURE0)
        inFbo.colorTexture.bind()

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height

        inFbo = FrameBufferObject(width, height)
        outFbo = FrameBufferObject(width, height)

        effects.filterIsInstance<Resizable>()
            .forEach { it.resize(width, height) }
    }
}