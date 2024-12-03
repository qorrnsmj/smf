package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.shader.custom.HueShaderProgram
import qorrnsmj.smf.util.UniformUtils

class HueEffect() : Effect(program) {
    var hueShift = 0f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationHueShift, hueShift)
    }

    override fun unuse() {
        program.unuse()
    }

    companion object {
        val program = HueShaderProgram()
        val locationHueShift = glGetUniformLocation(program.id, "hueShift")
    }
}
