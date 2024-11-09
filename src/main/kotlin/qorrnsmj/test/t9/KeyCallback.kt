package qorrnsmj.test.t9

import org.lwjgl.glfw.GLFW.GLFW_KEY_F11
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFWKeyCallback

class KeyCallback : GLFWKeyCallback() {
    override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
            Test9_2.window.toggleFullscreen()
        }
    }
}
