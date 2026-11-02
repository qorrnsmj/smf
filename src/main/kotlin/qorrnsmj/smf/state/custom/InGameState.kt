package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.level.LevelManager
import qorrnsmj.smf.state.State

class InGameState : State() {
    override fun start() {
        super.start()

        levelManager.loadLevel(SMF.initialLevelId)
        val loadingState = levelManager.loadingState
        if (loadingState.phase == LevelManager.LevelLoadingPhase.FAILED) {
            glfwSetWindowTitle(
                SMF.window.id,
                "SMF - Failed to load ${loadingState.targetLevelName}: ${loadingState.errorMessage ?: "unknown error"}",
            )
        }
    }
}
