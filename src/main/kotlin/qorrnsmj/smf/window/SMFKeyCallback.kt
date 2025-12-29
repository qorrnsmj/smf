package qorrnsmj.smf.window

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL33C.GL_FILL
import org.lwjgl.opengl.GL33C.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL33C.GL_LINE
import org.lwjgl.opengl.GL33C.glPolygonMode
import qorrnsmj.smf.SMF
import qorrnsmj.smf.state.States

class SMFKeyCallback : GLFWKeyCallback() {
    private var polygonMode = 0

    override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true)
            return
        }

        if (key == GLFW_KEY_E && action == GLFW_PRESS) {
            if (polygonMode == 0) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                polygonMode = 1
            } else {
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
                polygonMode = 0
            }
        }

        if (key == GLFW_KEY_0 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.EMPTY)

        if (key == GLFW_KEY_1 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.EXAMPLE1)

        if (key == GLFW_KEY_2 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.SOLAR_SYSTEM)

        if (key == GLFW_KEY_3 && action == GLFW_PRESS)
            SMF.stateMachine.changeState(States.GLTF)

        if (key == GLFW_KEY_F11 && action == GLFW_PRESS)
            SMF.window.toggleFullscreen()
    }
}
