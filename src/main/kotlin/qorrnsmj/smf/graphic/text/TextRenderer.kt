package qorrnsmj.smf.graphic.text

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.render.shader.TextShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.Cleanable
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

/**
 * Renders text using bitmap fonts with OpenGL.
 * Handles 2D screen-space text rendering with orthographic projection.
 */
class TextRenderer : Cleanable, Resizable {

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
        // Load text shaders
        shaderProgram = TextShaderProgram()

        // Create VAO and VBO for text rendering
        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        
        // Allocate buffer for dynamic text rendering
        glBufferData(GL_ARRAY_BUFFER, maxVertices * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)

        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        // Texture coordinate attribute  
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 2 * Float.SIZE_BYTES.toLong())
        glEnableVertexAttribArray(1)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    fun start() {
        shaderProgram.use()
        
        // Set up orthographic projection for screen coordinates
        val projectionMatrix = createOrthographicProjection()
        UniformUtils.setUniform(shaderProgram.getUniformLocation("u_projection"), projectionMatrix)

        // Enable blending for text transparency
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Orthographic top-left projection flips winding; avoid losing glyph quads to back-face culling.
        cullFaceWasEnabled = glIsEnabled(GL_CULL_FACE)
        if (cullFaceWasEnabled) {
            glDisable(GL_CULL_FACE)
        }
        
        // Disable depth testing for UI overlay
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

    /**
     * Render text at specified screen coordinates
     */
    private fun renderSingleText(text: String, font: Font, x: Float, y: Float, color: Vector3f = Vector3f(1f, 1f, 1f)) {
        if (text.isEmpty()) return

        // Set text color uniform
        UniformUtils.setUniform(shaderProgram.getUniformLocation("u_textColor"), color)

        // Bind font texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, font.glyphAtlas.getTextureId())
        glUniform1i(shaderProgram.getUniformLocation("u_glyphTexture"), 0)

        // Generate vertices for text
        val vertices = generateTextVertices(text, font, x, y)
        
        if (vertices.isNotEmpty()) {
            // Upload vertex data
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)

            // Draw text
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
            // STB y bearing (yOffset) is relative to baseline and is typically negative for glyph tops.
            val yPos = y + charInfo.bearingY

            val w = charInfo.width.toFloat()
            val h = charInfo.height.toFloat()

            // OpenGL texture coordinates are bottom-left origin, so use explicit top/bottom values.
            val texLeft = charInfo.textureX / atlasWidth
            val texRight = (charInfo.textureX + charInfo.width) / atlasWidth
            val texBottom = charInfo.textureY / atlasHeight
            val texTop = (charInfo.textureY + charInfo.height) / atlasHeight

            // Generate two triangles for the character quad
            // Triangle 1
            vertices.addAll(arrayOf(
                xPos, yPos + h, texLeft, texTop,      // Bottom left
                xPos, yPos, texLeft, texBottom,       // Top left
                xPos + w, yPos, texRight, texBottom   // Top right
            ))

            // Triangle 2
            vertices.addAll(arrayOf(
                xPos, yPos + h, texLeft, texTop,        // Bottom left
                xPos + w, yPos, texRight, texBottom,    // Top right
                xPos + w, yPos + h, texRight, texTop    // Bottom right
            ))

            // Advance cursor for next character
            x += charInfo.advance
        }

        return vertices.toFloatArray()
    }

    private fun createOrthographicProjection(): Matrix4f {
        // Create orthographic projection matrix for screen coordinates
        // (0,0) at top-left, (width, height) at bottom-right
        return Matrix4f.orthographic(0f, screenWidth.toFloat(), screenHeight.toFloat(), 0f, -1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    override fun cleanup() {
        shaderProgram.delete()
        
        if (vbo != 0) {
            glDeleteBuffers(vbo)
            vbo = 0
        }
        
        if (vao != 0) {
            glDeleteVertexArrays(vao)
            vao = 0
        }
    }
}
