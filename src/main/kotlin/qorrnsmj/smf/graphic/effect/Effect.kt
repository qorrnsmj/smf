package qorrnsmj.smf.graphic.effect

import qorrnsmj.smf.graphic.`object`.ShaderProgram

abstract class Effect(val program: ShaderProgram) {
    open fun use() {
        program.use()
    }

    open fun unuse() {
    }
}
