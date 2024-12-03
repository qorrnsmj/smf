package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.shader.custom.NoiseShaderProgram
import qorrnsmj.smf.util.UniformUtils

class NoiseEffect() : Effect(program) {
    var intensity = 0.3f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationIntensity, intensity)
    }

    override fun unuse() {
        program.unuse()
    }

    companion object {
        val program = NoiseShaderProgram()
        val locationIntensity = glGetUniformLocation(program.id, "intensity")
    }
}
