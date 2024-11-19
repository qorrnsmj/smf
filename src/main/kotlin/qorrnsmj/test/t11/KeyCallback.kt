package qorrnsmj.test.t11

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import qorrnsmj.smf.window.Window

class KeyCallback(private val targetWindow: Window) : GLFWKeyCallback() {
    override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true)
            return
        }

        if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
            targetWindow.toggleFullscreen()
        }
    }
}
