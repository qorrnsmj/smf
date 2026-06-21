package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.graphic.render.shader.TextShaderProgram
import qorrnsmj.smf.graphic.text.Font
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils
import kotlin.text.iterator

/**
 * Renders text using bitmap fonts with OpenGL.
 * Handles 2D screen-space text rendering with orthographic projection.
 */
class TextRenderer : Resizable {
    private lateinit var shaderProgram: TextShaderProgram
    private var vao: Int = 0
    private var vbo: Int = 0

    private var screenWidth: Int = 800
    private var screenHeight: Int = 600
    private var cullFaceWasEnabled: Boolean = false

    // Reusable vertex buffer for dynamic text rendering
    private val maxQuads = 1000
    private val verticesPerQuad = 6
    private val floatsPerVertex = 4 // x, y, u, v
    private val maxVertices = maxQuads * verticesPerQuad * floatsPerVertex

    init {
        initializeRenderer()
    }

    private fun initializeRenderer() {
        shaderProgram = TextShaderProgram()

        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, maxVertices * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 2 * Float.SIZE_BYTES.toLong())
        glEnableVertexAttribArray(1)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    fun start() {
        shaderProgram.use()

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        cullFaceWasEnabled = glIsEnabled(GL_CULL_FACE)
        if (cullFaceWasEnabled) {
            glDisable(GL_CULL_FACE)
        }

        glDisable(GL_DEPTH_TEST)
    }

    fun stop() {
        glEnable(GL_DEPTH_TEST)
        if (cullFaceWasEnabled) {
            glEnable(GL_CULL_FACE)
        }
        glDisable(GL_BLEND)
        glUseProgram(0)
    }

    fun renderText(textElements: List<TextElement>) {
        for (textElement in textElements) {
            renderSingleText(
                textElement.text,
                textElement.font,
                textElement.x,
                textElement.y,
                textElement.color
            )
        }
    }

    private fun renderSingleText(text: String, font: Font, x: Float, y: Float, color: Vector3f = Vector3f(1f, 1f, 1f)) {
        if (text.isEmpty()) return

        UniformUtils.setUniform(shaderProgram.getUniformLocation("u_textColor"), color)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, font.glyphAtlas.getTextureId())
        glUniform1i(shaderProgram.getUniformLocation("u_glyphTexture"), 0)

        val vertices = generateTextVertices(text, font, x, y)

        if (vertices.isNotEmpty()) {
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)
            glDrawArrays(GL_TRIANGLES, 0, vertices.size / floatsPerVertex)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindVertexArray(0)
        }
    }

    private fun generateTextVertices(text: String, font: Font, startX: Float, startY: Float): FloatArray {
        val vertices = mutableListOf<Float>()
        var x = startX
        val y = startY

        val atlasWidth = font.glyphAtlas.getWidth().toFloat()
        val atlasHeight = font.glyphAtlas.getHeight().toFloat()

        for (char in text) {
            val charInfo = font.getCharInfo(char) ?: continue

            val xPos = x + charInfo.bearingX
            val yPos = y + charInfo.bearingY

            val w = charInfo.width.toFloat()
            val h = charInfo.height.toFloat()

            val texLeft = charInfo.textureX / atlasWidth
            val texRight = (charInfo.textureX + charInfo.width) / atlasWidth
            val texBottom = charInfo.textureY / atlasHeight
            val texTop = (charInfo.textureY + charInfo.height) / atlasHeight

            vertices.addAll(arrayOf(
                xPos, yPos + h, texLeft, texTop,
                xPos, yPos, texLeft, texBottom,
                xPos + w, yPos, texRight, texBottom,
                xPos, yPos + h, texLeft, texTop,
                xPos + w, yPos, texRight, texBottom,
                xPos + w, yPos + h, texRight, texTop
            ))

            x += charInfo.advance
        }

        return vertices.toFloatArray()
    }

    private fun createOrthographicProjection(): Matrix4f {
        return Matrix4f.orthographic(0f, screenWidth.toFloat(), screenHeight.toFloat(), 0f, -1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        shaderProgram.use()
        UniformUtils.setUniform(shaderProgram.getUniformLocation("u_projection"), createOrthographicProjection())
    }
}
