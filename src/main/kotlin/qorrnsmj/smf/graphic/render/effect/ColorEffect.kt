package qorrnsmj.smf.graphic.render.effect

import qorrnsmj.smf.graphic.shader.custom.ColorEffectShaderProgram

class ColorEffect() : Effect(program) {
    override fun use() {
        program.use()
    }

    override fun unuse() {
        program.unuse()
    }

    companion object {
        val program = ColorEffectShaderProgram()
    }
}
