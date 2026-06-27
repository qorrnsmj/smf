package qorrnsmj.smf.game.task.cutscene

internal data class TimedCutsceneEvent(
    val timeSeconds: Float,
    val name: String,
    val skipPolicy: CutsceneSkipPolicy,
    val action: () -> Unit,
    var fired: Boolean = false,
) {
    init {
        require(timeSeconds >= 0f) { "Cutscene event time must be zero or greater" }
    }
}

enum class CutsceneSkipPolicy {
    SKIP,
    FIRE_ON_SKIP,
}
