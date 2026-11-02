package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.util.UniformUtils

class NoiseEffect() : Effect(program) {
    var intensity = 0.3f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationIntensity, intensity)
    }

    override fun unuse() {
    }

    companion object {
        val program = ShaderProgram("effect/noise.vert", "effect/noise.frag")
        val locationIntensity = glGetUniformLocation(program.id, "intensity")
    }
}
