package qorrnsmj.smf.graphic.text

import qorrnsmj.smf.math.Vector3f

/**
 * Represents a text element to be rendered on screen
 */
data class TextElement(
    val text: String,
    val font: Font,
    val x: Float,
    val y: Float,
    val color: Vector3f = Vector3f(1f, 1f, 1f)
)