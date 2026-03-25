package qorrnsmj.smf.graphic.text

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.util.ResourceUtils
import java.nio.ByteBuffer

/**
 * Loads TrueType fonts and generates Font objects with glyph atlases.
 * Uses LWJGL STB TrueType binding for font loading and rendering.
 */
object FontLoader {

    private const val FIRST_CHAR = 32  // Space character
    private const val CHAR_COUNT = 224 // ASCII + extended characters

    /**
     * Load a font from the resources and generate glyph atlas
     */
    fun loadFont(fontPath: String, fontSize: Float): Font {
        Logger.info("Loading font: $fontPath with size $fontSize")

        val fontData = loadFontData(fontPath)
        val fontInfo = STBTTFontinfo.create()

        if (!stbtt_InitFont(fontInfo, fontData)) {
            throw RuntimeException("Failed to initialize font: $fontPath")
        }

        // Calculate scale for desired font size
        val scale = stbtt_ScaleForPixelHeight(fontInfo, fontSize)

        // Get font metrics
        val ascent = IntArray(1)
        val descent = IntArray(1) 
        val lineGap = IntArray(1)
        stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap)

        val lineHeight = ((ascent[0] - descent[0] + lineGap[0]) * scale).toInt()

        // Create glyph atlas and font
        val glyphAtlas = GlyphAtlas()
        val font = Font(fontPath, fontSize, glyphAtlas, lineHeight)

        // Generate glyphs for common characters
        generateGlyphs(fontInfo, scale, font, glyphAtlas)

        Logger.info("Font loaded successfully: $fontPath")
        return font
    }

    private fun loadFontData(fontPath: String): ByteBuffer {
        try {
            return ResourceUtils.getResourceAsDirectBuffer(fontPath)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load font file: $fontPath", e)
        }
    }

    private fun generateGlyphs(fontInfo: STBTTFontinfo, scale: Float, font: Font, atlas: GlyphAtlas) {
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val xOffset = stack.mallocInt(1)
            val yOffset = stack.mallocInt(1)

            for (i in 0 until CHAR_COUNT) {
                val char = (FIRST_CHAR + i).toChar()
                val codepoint = char.code

                // Get glyph bitmap
                val bitmap = stbtt_GetCodepointBitmap(
                    fontInfo, scale, scale, codepoint,
                    width, height, xOffset, yOffset
                )

                if (bitmap != null) {
                    val glyphWidth = width[0]
                    val glyphHeight = height[0]
                    val bearingX = xOffset[0]
                    val bearingY = yOffset[0]

                    // Get advance width for character spacing
                    val advanceWidth = IntArray(1)
                    val leftSideBearing = IntArray(1)
                    stbtt_GetCodepointHMetrics(fontInfo, codepoint, advanceWidth, leftSideBearing)
                    val advance = (advanceWidth[0] * scale).toInt()

                    var atlasX = 0
                    var atlasY = 0

                    if (glyphWidth > 0 && glyphHeight > 0) {
                        // Convert bitmap to byte array
                        val bitmapBytes = ByteArray(glyphWidth * glyphHeight)
                        bitmap.get(bitmapBytes)

                        // Add glyph to atlas
                        val atlasPosition = atlas.addGlyph(bitmapBytes, glyphWidth, glyphHeight)
                        atlasX = atlasPosition.first
                        atlasY = atlasPosition.second
                    }

                    // Create character info
                    val charInfo = Font.CharInfo(
                        textureX = atlasX,
                        textureY = atlasY,
                        width = glyphWidth,
                        height = glyphHeight,
                        bearingX = bearingX,
                        bearingY = bearingY,
                        advance = advance
                    )

                    font.addCharInfo(char, charInfo)

                    // stbtt_FreeBitmap checks remaining() on the ByteBuffer, so rewind after reads.
                    if (bitmap.capacity() > 0) {
                        bitmap.rewind()
                        stbtt_FreeBitmap(bitmap)
                    }
                }
            }
        }
    }

    /**
     * Load a font from assets/font directory
     */
    fun loadAssetFont(fontName: String, fontSize: Float): Font {
        return loadFont("assets/font/$fontName", fontSize)
    }
}
