package qorrnsmj.smf.window

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.level.Levels
import qorrnsmj.smf.debug.DebugVisual

class SMFKeyCallback : GLFWKeyCallback() {
    override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_F12 && action == GLFW_PRESS) {
            SMF.requestScreenshot()
            return
        }
        if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
            SMF.window.toggleFullscreen()
            return
        }

        if (SMF.isEditorMode) {
            return
        }

        if (key == GLFW_KEY_E && action == GLFW_PRESS) {
            Logger.info("Wireframe: {}", SMF.toggleWireframe())
        }

        if (action == GLFW_PRESS) {
            val levelId = when (key) {
                GLFW_KEY_0 -> Levels.TEST_LEVEL
                GLFW_KEY_1 -> Levels.SUNWASH_MEADOW
                GLFW_KEY_2 -> Levels.MOONVEIL_MEADOW
                else -> null
            }
            if (levelId != null && SMF.stateMachine.switchLevel(levelId)) {
                Logger.info("Switching level by shortcut: {}", levelId)
                return
            }
        }

        if (key == GLFW_KEY_F1 && action == GLFW_PRESS) {
            val enabled = SMF.renderer.debugRenderer.toggle(DebugVisual.COLLIDERS)
            Logger.info("Collider debug: {}", enabled)
        }
    }
}
