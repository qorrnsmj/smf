package qorrnsmj.smf.graphic.effect.custom

import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.effect.shader.ColorShaderProgram

class ColorEffect() : Effect(program) {
    override fun use() {
        program.use()
    }

    override fun unuse() {
    }

    companion object {
        val program = ColorShaderProgram()
    }
}
