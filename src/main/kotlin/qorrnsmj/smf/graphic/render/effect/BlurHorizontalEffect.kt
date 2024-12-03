package qorrnsmj.smf.graphic.render.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.shader.custom.BlurHorizontalShaderProgram
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils

class BlurHorizontalEffect() : Effect(program), Resizable {
    private var targetWidth = 0
    var blurStrength = 8f // TODO: 初期値どうする

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationTargetWidth, targetWidth)
        UniformUtils.setUniform(locationBlurStrength, blurStrength)
    }

    override fun unuse() {
        program.unuse()
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
