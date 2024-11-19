package qorrnsmj.test.t9

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import qorrnsmj.smf.window.Window
//import qorrnsmj.smf.graphic.render.ViewMatrix
import qorrnsmj.test.t9.game.GrassBlock
import qorrnsmj.test.t9.render.Renderer
import qorrnsmj.test.t9.game.Ground

/** Camera.classを作って、自由に移動させる */
object Test9_1 {
    private lateinit var window: Window
    private lateinit var camera: Camera
    private lateinit var grassBlock: GrassBlock
    private lateinit var ground: Ground

    private fun init() {
        Renderer.init()
        camera = Camera()
        grassBlock = GrassBlock()
        ground = Ground()
    }

    private fun loop() {
        while (!window.shouldClose()) {
            // camera input and update
            camera.processKeyboardInput(window)
            camera.processMouseMovement(window)
//            Renderer.setView(ViewMatrix.getMatrix(
//                eye = camera.position,
//                center = camera.position.add(camera.front),
//                up = camera.up
//            ))

            // render
            Renderer.clear()
            grassBlock.draw()
            ground.draw()

            // update
            grassBlock.updateAngle(grassBlock.angle + 0.01f)
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600 / 2, 1600 / 2, "Test9_1", true)
        window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        window.show()

        init()
        loop()

        window.cleanup()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
