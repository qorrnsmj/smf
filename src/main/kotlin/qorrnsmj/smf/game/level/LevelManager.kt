package qorrnsmj.smf.game.level

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.level.custom.Level
import java.util.concurrent.atomic.AtomicReference

// TODO: シンプルに
class LevelManager {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private var currentLevel: Level? = null
    private var activeLevelName: String? = null
    @Volatile
    private var pendingLoadedLevel: PendingLoadedLevel? = null
    private var isTransitioning: Boolean = false
    private val stateRef = AtomicReference(LevelLoadingState())

    val loadingState: LevelLoadingState
        get() = stateRef.get()

    fun loadLevel(levelName: String) {
        if (currentLevel != null || isTransitioning) return

        val level = Levels.create(levelName)
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.LOADING))
        try {
            prepareLevel(level)
            level.load()
            currentLevel = level
            activeLevelName = levelName
            stateRef.set(LevelLoadingState(targetLevelName = levelName, phase = LevelLoadingPhase.COMPLETE))
            level.start()
        } catch (exception: Exception) {
            Logger.error(exception, "Failed to load initial level: {}", levelName)
            stateRef.set(
                LevelLoadingState(
                    isLoading = false,
                    targetLevelName = levelName,
                    phase = LevelLoadingPhase.FAILED,
                    errorMessage = exception.message ?: exception.javaClass.simpleName,
                )
            )
        }
    }

    fun switchLevel(levelName: String) {
        if (isTransitioning || levelName == activeLevelName) return

        isTransitioning = true
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.UNLOADING))

        currentLevel?.unload()
        currentLevel = null
        activeLevelName = null

        scope.launch {
            try {
                stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.LOADING))
                val level = Levels.create(levelName)
                pendingLoadedLevel = PendingLoadedLevel(levelName, level)
            } catch (exception: Exception) {
                Logger.error(exception, "Failed to load level: {}", levelName)
                stateRef.set(
                    LevelLoadingState(
                        isLoading = false,
                        targetLevelName = levelName,
                        phase = LevelLoadingPhase.FAILED,
                        errorMessage = exception.message,
                    )
                )
                isTransitioning = false
            }
        }
    }

    fun updateTransition() {
        val loadedLevel = pendingLoadedLevel ?: return
        pendingLoadedLevel = null

        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = loadedLevel.name, phase = LevelLoadingPhase.STARTING))
        switchToLevel(loadedLevel.level, loadedLevel.name)
    }

    fun update(delta: Float) {
        currentLevel?.updateLevel(delta)
    }

    fun getCurrentLevel(): Level? = currentLevel

    fun getCurrentLevelName(): String? = activeLevelName

    fun isTransitioning(): Boolean = isTransitioning

    fun stop() {
        currentLevel?.unload()
        currentLevel = null
        activeLevelName = null
        pendingLoadedLevel = null
        isTransitioning = false
        job.cancelChildren()
    }

    private fun switchToLevel(level: Level, levelName: String) {
        prepareLevel(level)
        currentLevel = level
        activeLevelName = levelName
        isTransitioning = true
        currentLevel!!.load()
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.STARTING))
        currentLevel!!.start()
        stateRef.set(LevelLoadingState(targetLevelName = levelName, phase = LevelLoadingPhase.COMPLETE))
        isTransitioning = false
    }

    private fun prepareLevel(level: Level) {
        level.attachLevelSwitcher(::switchLevel)
    }

    private data class PendingLoadedLevel(
        val name: String,
        val level: Level,
    )

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
}
