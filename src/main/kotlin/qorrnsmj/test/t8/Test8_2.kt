package qorrnsmj.test.t8

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.core.window.Window
import qorrnsmj.test.t8.block.StoneBlock
import qorrnsmj.test.t8.render.Renderer

/** テクスチャをはりつける
 * - Rendererを使って実装
 * - Textureクラスも実装
 * - フィルタリングの違いを見る (linear, nearest)
 */
object Test8_2 {
    private lateinit var window: Window
    private lateinit var stoneBlock: StoneBlock

    private fun init() {
        Renderer.init()
        stoneBlock = StoneBlock()
    }

    private fun loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            // render
            Renderer.clear()
            stoneBlock.draw()

            // update
            stoneBlock.updateAngle(stoneBlock.angle + 0.01f)
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test8_2", true)
        window.show()

        init()
        loop()

        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
