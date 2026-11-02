package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class BlurHorizontalEffect() : Effect(program), Resizable {
    private var targetWidth = 0
    var blurStrength = 5f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationTargetWidth, targetWidth)
        UniformUtils.setUniform(locationBlurStrength, blurStrength)
    }

    override fun unuse() {
    }

    override fun resize(width: Int, height: Int) {
        targetWidth = width
    }

    companion object {
        val program = ShaderProgram("effect/blur_horizontal.vert", "effect/blur_horizontal.frag")
        val locationTargetWidth = glGetUniformLocation(program.id, "targetWidth")
        val locationBlurStrength = glGetUniformLocation(program.id, "blurStrength")
    }
}
