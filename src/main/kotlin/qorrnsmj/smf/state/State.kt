package qorrnsmj.smf.state

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.level.LevelManager

abstract class State {
    protected val levelManager: LevelManager = LevelManager()
    protected var delta: Float = 1f

    fun activeLevel() = levelManager.getCurrentLevel()

    fun switchLevel(levelId: String): Boolean {
        if (levelManager.getCurrentLevel() == null) return false
        levelManager.switchLevel(levelId)
        return true
    }

    open fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
    }

    open fun input() {
        levelManager.getCurrentLevel()?.input(delta)
    }

    open fun update(delta: Float) {
        this.delta = delta
        levelManager.updateTransition()
        levelManager.update(delta)
    }

    open fun render(alpha: Float) {
        levelManager.getCurrentLevel()?.let { level ->
            SMF.applyGameRenderSettings()
            SMF.renderer.debugHud.update(
                level = level,
                stageName = levelManager.getCurrentLevelName() ?: "unknown",
                fps = SMF.timer.getFPS(),
                ups = SMF.timer.getUPS(),
                wireframeEnabled = SMF.wireframeEnabled,
                colliderDebugEnabled = SMF.renderer.debugRenderer.isEnabled(qorrnsmj.smf.debug.DebugVisual.COLLIDERS),
            )
            SMF.renderer.render(level.scene)
        }
    }

    open fun stop() {
        levelManager.stop()
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}
