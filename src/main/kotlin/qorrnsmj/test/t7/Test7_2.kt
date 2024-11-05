package qorrnsmj.test.t7

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import qorrnsmj.smf.core.window.Window

/** indexed triangle listで立方体を作る
 * - ついでにRendererの実装
 * - VAOとShaderProgramは使いまわし
 *
 */
object Test7_2 {
    private lateinit var window: Window

    private fun loop() {
        val cube1 = Cube(0f, 0f, 0f)

        while (!window.shouldClose()) {
            // update
            cube1.updateAngle(cube1.angle + 0.01f)

            // render
            Renderer.clear()
            Renderer.begin()
            cube1.draw()
            Renderer.end()

            // window update
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test7_2", true)
        window.show()

        Renderer.init()
        loop()

        window.destroy()
        Renderer.dispose()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
