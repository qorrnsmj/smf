package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class BlurVerticalEffect() : Effect(program), Resizable {
    private var targetHeight = 0
    var blurStrength = 5f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationTargetHeight, targetHeight)
        UniformUtils.setUniform(locationBlurStrength, blurStrength)
    }

    override fun unuse() {
    }

    override fun resize(width: Int, height: Int) {
        targetHeight = height
    }

    companion object {
        val program = ShaderProgram("effect/blur_vertical.vert", "effect/blur_vertical.frag")
        val locationTargetHeight = glGetUniformLocation(program.id, "targetHeight")
        val locationBlurStrength = glGetUniformLocation(program.id, "blurStrength")
    }
}
