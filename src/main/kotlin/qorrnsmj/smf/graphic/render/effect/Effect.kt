package qorrnsmj.smf.graphic.render.effect

import qorrnsmj.smf.graphic.shader.ShaderProgram

// このクラスでUniformとかのエフェクトのパラメーターを調整できるようにする
abstract class Effect(val program: ShaderProgram) {
    open fun use() {
        program.use()
    }

    open fun unuse() {
        program.unuse()
    }
}
