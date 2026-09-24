package qorrnsmj.smf.graphic.text

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.SceneRenderer
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils
import kotlin.text.iterator

/**
 * Renders text using bitmap fonts with OpenGL.
 * Handles 2D screen-space text rendering with orthographic projection.
 */
class TextRenderer : SceneRenderer, Resizable {
    private lateinit var shaderProgram: TextShaderProgram
    private var vao: Int = 0
    private var vbo: Int = 0
    private var rectProgram: Int = 0
    private var rectVao: Int = 0
    private var rectVbo: Int = 0
    private var rectProjectionLocation: Int = 0
    private var rectColorLocation: Int = 0

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

        initializeRectRenderer()
        resize(screenWidth, screenHeight)
    }

    private fun initializeRectRenderer() {
        rectProgram = createRectProgram()
        rectProjectionLocation = glGetUniformLocation(rectProgram, "u_projection")
        rectColorLocation = glGetUniformLocation(rectProgram, "u_color")

        rectVao = glGenVertexArrays()
        rectVbo = glGenBuffers()

        glBindVertexArray(rectVao)
        glBindBuffer(GL_ARRAY_BUFFER, rectVbo)
        glBufferData(GL_ARRAY_BUFFER, 12 * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    override fun render(scene: Scene) {
        val textElements = scene.world.textElements + listOfNotNull(
            scene.sceneEffects.cinematicOverlay.subtitle,
            scene.sceneEffects.cinematicOverlay.debugStatus,
        )
        if (textElements.isEmpty() && scene.world.textBoxes.isEmpty()) return

        start()
        renderTextBoxes(scene.world.textBoxes)
        renderText(textElements)
        stop()
    }

    private fun start() {
        shaderProgram.use()

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        cullFaceWasEnabled = glIsEnabled(GL_CULL_FACE)
        if (cullFaceWasEnabled) {
            glDisable(GL_CULL_FACE)
        }

        glDisable(GL_DEPTH_TEST)
    }

    private fun stop() {
        glEnable(GL_DEPTH_TEST)
        if (cullFaceWasEnabled) {
            glEnable(GL_CULL_FACE)
        }
        glDisable(GL_BLEND)
        glUseProgram(0)
    }

    private fun renderTextBoxes(textBoxes: List<TextBoxElement>) {
        for (textBox in textBoxes) {
            renderTextBox(textBox)
        }
    }

    private fun renderTextBox(textBox: TextBoxElement) {
        val contentX = textBox.x + textBox.padding.left
        val contentY = textBox.y + textBox.padding.top
        val contentWidth = textBox.width - textBox.padding.left - textBox.padding.right
        val layout = layoutRuns(textBox.runs, contentWidth, textBox.lineSpacing)
        val boxHeight = maxOf(
            textBox.minHeight,
            textBox.padding.top + layout.height + textBox.padding.bottom,
        )

        drawRect(textBox.x, textBox.y, textBox.width, boxHeight, textBox.backgroundColor)
        drawRect(textBox.x, textBox.y, textBox.width, 1f, textBox.borderColor)
        drawRect(textBox.x, textBox.y + boxHeight - 1f, textBox.width, 1f, textBox.borderColor)
        drawRect(textBox.x, textBox.y, 1f, boxHeight, textBox.borderColor)
        drawRect(textBox.x + textBox.width - 1f, textBox.y, 1f, boxHeight, textBox.borderColor)

        val speakerName = (textBox.speaker?.displayName ?: textBox.speakerName)?.takeIf { it.isNotBlank() }
        val speakerStyle = textBox.speakerStyle ?: textBox.runs.firstOrNull()?.style
        if (speakerName != null && speakerStyle != null) {
            val labelPaddingX = 12f
            val labelWidth = speakerStyle.font.getTextWidth(speakerName).toFloat() + labelPaddingX * 2f
            val labelHeight = speakerStyle.font.lineHeight + 10f
            val labelX = textBox.x + 14f
            val labelY = textBox.y - labelHeight + 8f
            drawRect(labelX, labelY, labelWidth, labelHeight, textBox.speakerBackgroundColor)
            drawRect(labelX, labelY, labelWidth, 1f, textBox.borderColor)
            drawRect(labelX, labelY, 1f, labelHeight, textBox.borderColor)
            drawRect(labelX + labelWidth - 1f, labelY, 1f, labelHeight, textBox.borderColor)
            renderSingleText(speakerName, speakerStyle.font, labelX + labelPaddingX, labelY + labelHeight - 8f, speakerStyle.color, speakerStyle.bold)
        }

        for (run in layout.runs) {
            renderSingleText(run.text, run.style.font, contentX + run.x, contentY + run.y, run.style.color, run.style.bold)
        }
    }

    private fun renderText(textElements: List<TextElement>) {
        for (textElement in textElements) {
            val anchoredX = when (textElement.anchor) {
                TextAnchor.TOP_LEFT -> textElement.x
                TextAnchor.BOTTOM_CENTER ->
                    (screenWidth - textElement.font.getTextWidth(textElement.text)) / 2f + textElement.x
            }
            val anchoredY = when (textElement.anchor) {
                TextAnchor.TOP_LEFT -> textElement.y
                TextAnchor.BOTTOM_CENTER -> screenHeight + textElement.y
            }
            renderSingleText(
                textElement.text,
                textElement.font,
                anchoredX,
                anchoredY,
                textElement.color,
            )
        }
    }

    private fun renderSingleText(
        text: String,
        font: Font,
        x: Float,
        y: Float,
        color: Vector3f = Vector3f(1f, 1f, 1f),
        bold: Boolean = false,
    ) {
        if (text.isEmpty()) return
        renderSingleTextPass(text, font, x, y, color)
        if (bold) {
            renderSingleTextPass(text, font, x + 1f, y, color)
        }
    }

    private fun renderSingleTextPass(text: String, font: Font, x: Float, y: Float, color: Vector3f) {
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

    private fun layoutRuns(runs: List<TextRun>, maxWidth: Float, lineSpacing: Float): TextLayout {
        val laidOutRuns = mutableListOf<PositionedTextRun>()
        var cursorX = 0f
        var cursorY = 0f
        var lineHeight = runs.firstOrNull()?.style?.font?.lineHeight?.toFloat() ?: 0f
        var segmentStartX = 0f
        var segment = StringBuilder()
        var segmentStyle: TextStyle? = null

        fun emitSegment() {
            val style = segmentStyle ?: return
            if (segment.isNotEmpty()) {
                laidOutRuns.add(PositionedTextRun(segment.toString(), style, segmentStartX, cursorY))
                segment = StringBuilder()
            }
        }

        fun newLine(nextStyle: TextStyle) {
            emitSegment()
            cursorX = 0f
            cursorY += lineHeight + lineSpacing
            lineHeight = nextStyle.font.lineHeight.toFloat()
            segmentStartX = 0f
        }

        for (run in runs) {
            if (segmentStyle != run.style) {
                emitSegment()
                segmentStyle = run.style
                segmentStartX = cursorX
            }

            for (char in run.text) {
                if (char == '\n') {
                    newLine(run.style)
                    continue
                }

                val charInfo = run.style.font.getCharInfo(char) ?: continue
                val advance = charInfo.advance.toFloat()
                if (cursorX > 0f && cursorX + advance > maxWidth) {
                    newLine(run.style)
                }

                if (segment.isEmpty()) {
                    segmentStartX = cursorX
                }
                segment.append(char)
                cursorX += advance
                lineHeight = maxOf(lineHeight, run.style.font.lineHeight.toFloat())
            }
        }

        emitSegment()
        return TextLayout(laidOutRuns, cursorY + lineHeight)
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

    private fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Vector4f) {
        if (width <= 0f || height <= 0f || color.w <= 0f) return

        val x2 = x + width
        val y2 = y + height
        val vertices = floatArrayOf(
            x, y2,
            x, y,
            x2, y,
            x, y2,
            x2, y,
            x2, y2,
        )

        glUseProgram(rectProgram)
        glUniform4f(rectColorLocation, color.x, color.y, color.z, color.w)
        glBindVertexArray(rectVao)
        glBindBuffer(GL_ARRAY_BUFFER, rectVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)
        glDrawArrays(GL_TRIANGLES, 0, 6)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        shaderProgram.use()
    }

    private fun createRectProgram(): Int {
        val vertexShader = compileShader(
            GL_VERTEX_SHADER,
            """
            #version 330 core
            layout (location = 0) in vec2 position;
            uniform mat4 u_projection;
            void main() {
                gl_Position = u_projection * vec4(position, 0.0, 1.0);
            }
            """.trimIndent(),
        )
        val fragmentShader = compileShader(
            GL_FRAGMENT_SHADER,
            """
            #version 330 core
            out vec4 fragColor;
            uniform vec4 u_color;
            void main() {
                fragColor = u_color;
            }
            """.trimIndent(),
        )

        val result = glCreateProgram()
        glAttachShader(result, vertexShader)
        glAttachShader(result, fragmentShader)
        glLinkProgram(result)
        check(glGetProgrami(result, GL_LINK_STATUS) == GL_TRUE) {
            glGetProgramInfoLog(result)
        }
        return result
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        check(glGetShaderi(shader, GL_COMPILE_STATUS) == GL_TRUE) {
            glGetShaderInfoLog(shader)
        }
        return shader
    }

    private fun createOrthographicProjection(): Matrix4f {
        return Matrix4f.orthographic(0f, screenWidth.toFloat(), screenHeight.toFloat(), 0f, -1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        shaderProgram.use()
        val projection = createOrthographicProjection()
        UniformUtils.setUniform(shaderProgram.getUniformLocation("u_projection"), projection)
        glUseProgram(rectProgram)
        UniformUtils.setUniform(rectProjectionLocation, projection)
        shaderProgram.use()
    }

    private data class PositionedTextRun(
        val text: String,
        val style: TextStyle,
        val x: Float,
        val y: Float,
    )

    private data class TextLayout(
        val runs: List<PositionedTextRun>,
        val height: Float,
    )
}
