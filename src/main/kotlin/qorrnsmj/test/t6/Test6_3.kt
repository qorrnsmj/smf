package qorrnsmj.test.t6

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.core.window.Window
import kotlin.math.abs
import kotlin.math.sin

/** 立方体を作る
 * - 回転もさせちゃう
 * - Cubeクラスに落とし込む
 * - https://stackoverflow.com/questions/16881807/once-more-triangle-strips-vs-triangle-lists
 */
object Test6_3 {
    private lateinit var window: Window

    private fun loop() {
        val cube = Cube(0f, 0f, 0f)

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            cube.angle += 0.01f
            cube.scale = abs(sin(cube.angle))
            cube.draw()

            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test6_3", true)
        window.show()

        loop()

        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
