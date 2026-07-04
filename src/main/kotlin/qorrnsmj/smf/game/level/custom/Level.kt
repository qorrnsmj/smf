package qorrnsmj.smf.game.level.custom

import qorrnsmj.smf.SMF
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.cutscene.CutsceneManager
import qorrnsmj.smf.graphic.Scene

abstract class Level {
    val scene: Scene = Scene()
    val cutscenes: CutsceneManager = CutsceneManager(scene)
    lateinit var player: Player
    private var levelSwitcher: ((String) -> Unit)? = null

    abstract fun load()

    open fun unload() {
        cutscenes.stop()
    }

    open fun start() {}

    open fun input(delta: Float) {}

    open fun update(delta: Float) {}

    protected fun handleCutsceneInput(): Boolean {
        cutscenes.handleInput(SMF.window)
        return cutscenes.isPlaying
    }

    protected fun updateCutscenes(delta: Float) {
        cutscenes.update(delta / FixedTimestepGame.TARGET_UPS)
    }

    internal fun attachLevelSwitcher(levelSwitcher: (String) -> Unit) {
        this.levelSwitcher = levelSwitcher
    }

    protected fun switchLevel(levelName: String) {
        levelSwitcher?.invoke(levelName)
            ?: error("Level switcher is not attached.")
    }
}
