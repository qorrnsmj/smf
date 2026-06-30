package qorrnsmj.smf.game.level

data class LevelLoadingState(
    val isLoading: Boolean = false,
    val targetLevelName: String? = null,
    val phase: LevelLoadingPhase = LevelLoadingPhase.IDLE,
    val errorMessage: String? = null,
)

enum class LevelLoadingPhase {
    IDLE,
    UNLOADING,
    LOADING,
    STARTING,
    COMPLETE,
    FAILED,
}
