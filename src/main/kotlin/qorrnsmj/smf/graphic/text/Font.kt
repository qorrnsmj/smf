package qorrnsmj.smf.graphic.text

import qorrnsmj.smf.util.Cleanable

/**
 * Represents a loaded font with glyph atlas and metrics data.
 * Manages font rendering resources and provides character information.
 */
class Font(
    val name: String,
    val size: Float,
    val glyphAtlas: GlyphAtlas,
    val lineHeight: Int
) : Cleanable {

    /**
     * Information about a single character glyph
     */
    data class CharInfo(
        val textureX: Int,
        val textureY: Int,
        val width: Int,
        val height: Int,
        val bearingX: Int,
        val bearingY: Int,
        val advance: Int
    )

    private val charInfoMap = mutableMapOf<Char, CharInfo>()

    /**
     * Add character information to this font
     */
    internal fun addCharInfo(char: Char, info: CharInfo) {
        charInfoMap[char] = info
    }

    /**
     * Get character information for a specific character
     */
    fun getCharInfo(char: Char): CharInfo? = charInfoMap[char]

    /**
     * Check if this font has a character available
     */
    fun hasChar(char: Char): Boolean = charInfoMap.containsKey(char)

    /**
     * Calculate text width in pixels
     */
    fun getTextWidth(text: String): Int {
        var width = 0
        for (char in text) {
            val charInfo = getCharInfo(char) ?: continue
            width += charInfo.advance
        }
        return width
    }

    override fun cleanup() {
        glyphAtlas.cleanup()
        charInfoMap.clear()
    }
}
