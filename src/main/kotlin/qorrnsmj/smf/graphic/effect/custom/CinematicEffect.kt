package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.effect.shader.CinematicShaderProgram
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.UniformUtils

class CinematicEffect : Effect(program) {
    var fadeAlpha = 0f
    var fadeColor = Vector3f(0f, 0f, 0f)
    var letterboxRatio = 0f

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

    fun update(overlay: CinematicOverlay) {
        fadeAlpha = overlay.fadeAlpha.coerceIn(0f, 1f)
        fadeColor = overlay.fadeColor
        letterboxRatio = overlay.letterboxRatio.coerceIn(0f, 0.45f)
    }

    fun isActive(): Boolean = fadeAlpha > 0f || letterboxRatio > 0f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationFadeAlpha, fadeAlpha)
        UniformUtils.setUniform(locationFadeColor, fadeColor)
        UniformUtils.setUniform(locationLetterboxRatio, letterboxRatio)
    }

    companion object {
        val program = CinematicShaderProgram()
        val locationFadeAlpha = glGetUniformLocation(program.id, "fadeAlpha")
        val locationFadeColor = glGetUniformLocation(program.id, "fadeColor")
        val locationLetterboxRatio = glGetUniformLocation(program.id, "letterboxRatio")
    }
}
