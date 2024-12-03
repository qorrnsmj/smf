package qorrnsmj.smf.graphic.effect.custom

import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.shader.custom.MonochromeShaderProgram

class MonochromeEffect() : Effect(program) {
    override fun use() {
        program.use()
    }

    override fun unuse() {
        program.unuse()
    }

    companion object {
        val program = MonochromeShaderProgram()
    }
}
