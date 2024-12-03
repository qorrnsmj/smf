package qorrnsmj.smf.graphic.effect.custom

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.shader.custom.ContrastShaderProgram
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
        val program = ContrastShaderProgram()
        val locationContrast = glGetUniformLocation(program.id, "contrast")
    }
}
