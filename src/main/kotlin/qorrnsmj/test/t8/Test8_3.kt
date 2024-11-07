package qorrnsmj.test.t8

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.core.window.Window
import qorrnsmj.test.t8.block.GrassBlock
import qorrnsmj.test.t8.block.StoneBlock
import qorrnsmj.test.t8.render.Renderer

/** テクスチャをはりつける
 * - uvを使って、貼り付けるテクスチャの範囲を指定する
 * - vertexColorとテクスチャの色を混ぜる
 */
object Test8_3 {
    private lateinit var window: Window
    private lateinit var grassBlock: GrassBlock

    private fun init() {
        Renderer.init()
        grassBlock = GrassBlock()
    }

    private fun loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            // render
            Renderer.clear()
            Renderer.begin()
            grassBlock.draw()
            Renderer.end()

            // update
            grassBlock.updateAngle(grassBlock.angle + 0.01f)
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test8_3", true)
        window.show()

        init()
        loop()

        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
