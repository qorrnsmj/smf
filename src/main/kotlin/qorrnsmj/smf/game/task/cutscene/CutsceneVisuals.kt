package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.math.Vector3f

internal data class FadeCue(
    val startSeconds: Float,
    val durationSeconds: Float,
    val fromAlpha: Float,
    val toAlpha: Float,
    val color: Vector3f,
) {
    val endSeconds: Float = startSeconds + durationSeconds

    fun alphaAt(timeSeconds: Float): Float {
        if (durationSeconds <= 0f) return toAlpha
        val progress = ((timeSeconds - startSeconds) / durationSeconds).coerceIn(0f, 1f)
        return fromAlpha + (toAlpha - fromAlpha) * Easing.SMOOTH_STEP.apply(progress)
    }
}

internal data class SubtitleCue(
    val startSeconds: Float,
    val durationSeconds: Float,
    val text: String,
    val color: Vector3f,
) {
    val endSeconds: Float = startSeconds + durationSeconds
}

internal data class CutsceneVisualState(
    val fadeAlpha: Float,
    val fadeColor: Vector3f,
    val letterboxRatio: Float,
    val subtitle: SubtitleCue?,
)
