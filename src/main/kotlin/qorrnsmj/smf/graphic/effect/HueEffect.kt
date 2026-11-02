package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.util.UniformUtils

class HueEffect() : Effect(program) {
    var hueShift = 0f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationHueShift, hueShift)
    }

    override fun unuse() {
    }

    companion object {
        val program = ShaderProgram("effect/hue.vert", "effect/hue.frag")
        val locationHueShift = glGetUniformLocation(program.id, "hueShift")
    }
}
