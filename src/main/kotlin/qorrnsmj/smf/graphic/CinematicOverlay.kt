package qorrnsmj.smf.graphic

import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.math.Vector3f

data class CinematicOverlay(
    var fadeAlpha: Float = 0f,
    var fadeColor: Vector3f = Vector3f(0f, 0f, 0f),
    var letterboxRatio: Float = 0f,
    var subtitle: TextElement? = null,
    var debugStatus: TextElement? = null,
) {
    fun clear() {
        fadeAlpha = 0f
        fadeColor = Vector3f(0f, 0f, 0f)
        letterboxRatio = 0f
        subtitle = null
        debugStatus = null
    }
}
