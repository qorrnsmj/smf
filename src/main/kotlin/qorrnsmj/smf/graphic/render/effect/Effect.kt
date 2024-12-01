package qorrnsmj.smf.graphic.render.effect

import qorrnsmj.smf.graphic.shader.ShaderProgram

// TODO: このクラスでUniformとかのエフェクトのパラメーターを調整できるようにする
// TODO: シェーダーはcompanionで持たせるようにする (uniformの値を変えるのはuseで行う)
abstract class Effect(val program: ShaderProgram) {
    open fun use() {
        program.use()
    }

    open fun unuse() {
        program.unuse()
    }
}
