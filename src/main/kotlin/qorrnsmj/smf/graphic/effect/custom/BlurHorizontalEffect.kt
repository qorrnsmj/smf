package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.effect.shader.BlurHorizontalShaderProgram
import qorrnsmj.smf.util.impl.Resizable
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
        val program = BlurHorizontalShaderProgram()
        val locationTargetWidth = glGetUniformLocation(program.id, "targetWidth")
        val locationBlurStrength = glGetUniformLocation(program.id, "blurStrength")
    }
}
