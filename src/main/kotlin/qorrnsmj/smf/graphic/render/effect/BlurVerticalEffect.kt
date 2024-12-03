package qorrnsmj.smf.graphic.render.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.shader.custom.BlurVerticalShaderProgram
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class BlurVerticalEffect() : Effect(program), Resizable {
    private var targetHeight = 0
    var blurStrength = 8f // TODO: 初期値どうする

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationTargetHeight, targetHeight)
        UniformUtils.setUniform(locationBlurStrength, blurStrength)
    }

    override fun unuse() {
        program.unuse()
    }

    override fun resize(width: Int, height: Int) {
        targetHeight = height
    }

    companion object {
        val program = BlurVerticalShaderProgram()
        val locationTargetHeight = glGetUniformLocation(program.id, "targetHeight")
        val locationBlurStrength = glGetUniformLocation(program.id, "blurStrength")
    }
}
