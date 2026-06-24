package qorrnsmj.smf.game.task.cutscene

internal data class BgmFadeCue(
    val startSeconds: Float,
    val durationSeconds: Float,
    val fromVolume: Float,
    val toVolume: Float,
) {
    val endSeconds: Float = startSeconds + durationSeconds

    fun volumeAt(timeSeconds: Float): Float {
        if (durationSeconds <= 0f) return toVolume
        val progress = ((timeSeconds - startSeconds) / durationSeconds).coerceIn(0f, 1f)
        val eased = Easing.SMOOTH_STEP.apply(progress)
        return fromVolume + (toVolume - fromVolume) * eased
    }
}
