package qorrnsmj.smf.graphic.effect.custom

import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

abstract class Effect(val program: ShaderProgram) {
    var postEffectOrder: Int = PostEffectOrder.DEFAULT
        internal set

    open fun use() {
        program.use()
    }

    open fun unuse() {
    }
}

fun MutableList<Effect>.addPostEffect(effect: Effect, order: Int = PostEffectOrder.DEFAULT) {
    remove(effect)
    effect.postEffectOrder = order

    val insertAt = indexOfFirst { it.postEffectOrder > order }
    if (insertAt == -1) {
        add(effect)
    } else {
        add(insertAt, effect)
    }
}

object PostEffectOrder {
    const val DEFAULT = 0
    const val CINEMATIC = 900
}
