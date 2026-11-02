package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.effect.CinematicEffect.CinematicOverlay

data class SceneEffects(
    val postEffects: MutableList<Effect> = mutableListOf(),
    val cinematicOverlay: CinematicOverlay = CinematicOverlay(),
) {
    fun isNotEmpty(): Boolean = postEffects.isNotEmpty()
}
