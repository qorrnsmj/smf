package qorrnsmj.smf.game.level

class LevelManager {
    private var currentLevel: Level? = null
    private var nextLevel: Level? = null
    private var isTransitioning: Boolean = false

    fun loadLevel(level: Level) {
        if (isTransitioning) return

        if (currentLevel != null) {
            currentLevel!!.unload()
        }

        nextLevel = level
        isTransitioning = true
    }

    fun updateTransition() {
        if (!isTransitioning) return

        currentLevel = nextLevel
        currentLevel!!.load()
        currentLevel!!.start()
        nextLevel = null
        isTransitioning = false
    }

    fun update(delta: Float) {
        currentLevel?.update(delta)
    }

    fun getCurrentLevel(): Level? = currentLevel

    fun isTransitioning(): Boolean = isTransitioning
}
