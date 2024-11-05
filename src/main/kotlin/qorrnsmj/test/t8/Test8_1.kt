package qorrnsmj.test.t8

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import qorrnsmj.smf.core.window.Window

/** テクスチャをはりつける
 */
object Test8_1 {
    lateinit var window: Window
    lateinit var renderer: Renderer

    private fun loop() {
        val cube1 = Cube(0f, 0f, 0f)

        while (!window.shouldClose()) {
            // update
            cube1.updateAngle(cube1.angle + 0.01f)

            // render
            renderer.clear()
            renderer.begin()
            //cube1.draw()
            renderer.end()

            // window update
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test8_1", true)
        window.show()
        renderer = Renderer
        renderer.init()

        loop()

        renderer.dispose()
        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
