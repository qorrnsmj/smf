package qorrnsmj.smf.graphic.effect

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram
import qorrnsmj.smf.util.UniformUtils

class ContrastEffect() : Effect(program) {
    var contrast = 3f

    override fun use() {
        program.use()
        UniformUtils.setUniform(locationContrast, contrast)
    }

    override fun unuse() {
    }

    companion object {
        val program = ShaderProgram("effect/contrast.vert", "effect/contrast.frag")
        val locationContrast = glGetUniformLocation(program.id, "contrast")
    }
}
