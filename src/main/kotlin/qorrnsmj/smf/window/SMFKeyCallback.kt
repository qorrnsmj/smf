package qorrnsmj.smf.window

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import qorrnsmj.smf.SMF
import qorrnsmj.smf.state.States

class SMFKeyCallback : GLFWKeyCallback() {
    override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true)
            return
        }

        if (key == GLFW_KEY_0 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.EMPTY)

        if (key == GLFW_KEY_1 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.EXAMPLE1)

        if (key == GLFW_KEY_2 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.EXAMPLE2)

        if (key == GLFW_KEY_F11 && action == GLFW_PRESS)
            SMF.window.toggleFullscreen()
    }
}
