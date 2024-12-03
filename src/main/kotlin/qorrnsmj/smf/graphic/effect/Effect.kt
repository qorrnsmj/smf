package qorrnsmj.smf.graphic.effect

import qorrnsmj.smf.graphic.shader.ShaderProgram

abstract class Effect(val program: ShaderProgram) {
    open fun use() {
        program.use()
    }

    open fun unuse() {
        program.unuse()
    }
}
