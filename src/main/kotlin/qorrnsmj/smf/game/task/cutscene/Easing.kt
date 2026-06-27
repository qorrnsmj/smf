package qorrnsmj.smf.game.task.cutscene

enum class Easing {
    LINEAR,
    SMOOTH_STEP,
    EASE_IN_OUT_CUBIC;

    fun apply(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return when (this) {
            LINEAR -> t
            SMOOTH_STEP -> t * t * (3f - 2f * t)
            EASE_IN_OUT_CUBIC -> {
                if (t < 0.5f) {
                    4f * t * t * t
                } else {
                    val shifted = -2f * t + 2f
                    1f - shifted * shifted * shifted / 2f
                }
            }
        }
    }
}
