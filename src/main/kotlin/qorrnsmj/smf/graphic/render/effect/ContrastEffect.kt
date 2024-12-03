package qorrnsmj.smf.graphic.render.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.shader.custom.ContrastEffectShaderProgram
import qorrnsmj.smf.util.UniformUtils

class ContrastEffect() : Effect(program) {
    var contrast = 3f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationContrast, contrast)
    }

    override fun unuse() {
        program.unuse()
    }

    companion object {
        val program = ContrastEffectShaderProgram()
        val locationContrast = glGetUniformLocation(program.id, "contrast")
    }
}
