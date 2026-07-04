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
    private var homeLevel: Level? = null
    private var activeLevelName: String? = null
    @Volatile
    private var pendingLoadedLevel: PendingLoadedLevel? = null
    private var isTransitioning: Boolean = false
    private val stateRef = AtomicReference(LevelLoadingState())

    val loadingState: LevelLoadingState
        get() = stateRef.get()

    fun loadLevel(entry: Levels.LevelEntry) {
        if (homeLevel != null || isTransitioning) return

        val levelName = entry.id
        val level = entry.create()
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.LOADING))
        prepareLevel(level)
        level.load()
        homeLevel = level
        currentLevel = level
        activeLevelName = levelName
        stateRef.set(LevelLoadingState(targetLevelName = levelName, phase = LevelLoadingPhase.COMPLETE))
        level.start()
    }

    fun switchLevel(levelName: String) {
        if (isTransitioning) return

        if (levelName == HOME_LEVEL_NAME && homeLevel != null) {
            switchToHome()
            return
        }

        isTransitioning = true
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.UNLOADING))

        currentLevel?.takeUnless { it === homeLevel }?.unload()
        currentLevel = null
        activeLevelName = null

        scope.launch {
            try {
                stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = levelName, phase = LevelLoadingPhase.LOADING))
                val level = createLevel(levelName)
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
        currentLevel?.update(delta)
    }

    fun getCurrentLevel(): Level? = currentLevel

    fun isTransitioning(): Boolean = isTransitioning

    fun stop() {
        currentLevel?.takeUnless { it === homeLevel }?.unload()
        homeLevel?.unload()
        job.cancelChildren()
    }

    private fun switchToHome() {
        isTransitioning = true
        stateRef.set(LevelLoadingState(isLoading = true, targetLevelName = HOME_LEVEL_NAME, phase = LevelLoadingPhase.UNLOADING))
        currentLevel?.takeUnless { it === homeLevel }?.unload()
        currentLevel = homeLevel
        activeLevelName = HOME_LEVEL_NAME
        stateRef.set(LevelLoadingState(targetLevelName = HOME_LEVEL_NAME, phase = LevelLoadingPhase.COMPLETE))
        currentLevel?.start()
        isTransitioning = false
    }

    private fun switchToLevel(level: Level, levelName: String) {
        currentLevel?.takeUnless { it === homeLevel }?.unload()
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

    private fun createLevel(levelName: String): Level {
        return Levels.create(levelName)
    }

    private data class PendingLoadedLevel(
        val name: String,
        val level: Level,
    )

    companion object {
        const val HOME_LEVEL_NAME = "test"
    }

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
