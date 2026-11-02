package qorrnsmj.smf.graphic.text

import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

/**
 * Represents a text element to be rendered on screen
 */
data class TextElement(
    val text: String,
    val font: Font,
    val x: Float,
    val y: Float,
    val color: Vector3f = Vector3f(1f, 1f, 1f),
    val anchor: TextAnchor = TextAnchor.TOP_LEFT,
)

enum class TextAnchor {
    TOP_LEFT,
    BOTTOM_CENTER,
}

data class TextStyle(
    val font: Font,
    val color: Vector3f = Vector3f(1f, 1f, 1f),
    val bold: Boolean = false,
)

data class TextRun(
    val text: String,
    val style: TextStyle,
)

data class TextBoxPadding(
    val left: Float = 18f,
    val top: Float = 22f,
    val right: Float = 18f,
    val bottom: Float = 18f,
)

data class TextBoxElement(
    val runs: List<TextRun>,
    val x: Float,
    val y: Float,
    val width: Float,
    val speaker: Entity? = null,
    val speakerName: String? = speaker?.displayName,
    val speakerStyle: TextStyle? = null,
    val backgroundColor: Vector4f = Vector4f(0.05f, 0.06f, 0.08f, 0.88f),
    val borderColor: Vector4f = Vector4f(0.85f, 0.85f, 0.85f, 0.35f),
    val speakerBackgroundColor: Vector4f = Vector4f(0.1f, 0.12f, 0.16f, 0.95f),
    val padding: TextBoxPadding = TextBoxPadding(),
    val lineSpacing: Float = 6f,
    val minHeight: Float = 92f,
) {
    constructor(
        text: String,
        style: TextStyle,
        x: Float,
        y: Float,
        width: Float,
        speaker: Entity? = null,
        speakerStyle: TextStyle? = null,
    ) : this(
        runs = listOf(TextRun(text, style)),
        x = x,
        y = y,
        width = width,
        speaker = speaker,
        speakerStyle = speakerStyle,
    )
}
