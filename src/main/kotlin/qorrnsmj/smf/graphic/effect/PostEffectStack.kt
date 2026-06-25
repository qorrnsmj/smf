package qorrnsmj.smf.graphic.effect

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
