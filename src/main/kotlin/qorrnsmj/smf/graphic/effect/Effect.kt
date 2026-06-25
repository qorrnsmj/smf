package qorrnsmj.smf.graphic.effect

import qorrnsmj.smf.graphic.`object`.ShaderProgram

abstract class Effect(val program: ShaderProgram) {
    var postEffectOrder: Int = PostEffectOrder.DEFAULT
        internal set

    open fun use() {
        program.use()
    }

    open fun unuse() {
    }
}
