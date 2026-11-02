package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.UniformUtils.setUniform

class ColorEffect() : Effect(program) {
    var exposure = 1f
    var saturation = 1f
    var lift = Vector3f(0f, 0f, 0f)
    var tint = Vector3f(1f, 1f, 1f)

    override fun use() {
        program.use()
        setUniform(locationExposure, exposure)
        setUniform(locationSaturation, saturation)
        setUniform(locationLift, lift)
        setUniform(locationTint, tint)
    }

    override fun unuse() {
    }

    companion object {
        val program = ShaderProgram("effect/color.vert", "effect/color.frag")
        val locationExposure = glGetUniformLocation(program.id, "exposure")
        val locationSaturation = glGetUniformLocation(program.id, "saturation")
        val locationLift = glGetUniformLocation(program.id, "lift")
        val locationTint = glGetUniformLocation(program.id, "tint")
    }
}
