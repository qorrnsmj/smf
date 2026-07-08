package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.graphic.effect.custom.Effect
import qorrnsmj.smf.graphic.effect.custom.CinematicEffect.CinematicOverlay

data class SceneEffects(
    val postEffects: MutableList<Effect> = mutableListOf(),
    val cinematicOverlay: CinematicOverlay = CinematicOverlay(),
) {
    fun isNotEmpty(): Boolean = postEffects.isNotEmpty()
}
