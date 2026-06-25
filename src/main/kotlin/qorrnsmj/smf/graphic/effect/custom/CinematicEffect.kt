package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.CinematicOverlay
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.effect.shader.CinematicShaderProgram
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.UniformUtils

class CinematicEffect : Effect(program) {
    var fadeAlpha = 0f
    var fadeColor = Vector3f(0f, 0f, 0f)
    var letterboxRatio = 0f

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
