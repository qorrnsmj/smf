package qorrnsmj.test.t9

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.test.t10.KeyCallback
import qorrnsmj.test.t10.Test10_1
import qorrnsmj.test.t9.game.GrassBlock
import qorrnsmj.test.t9.game.Ground
import qorrnsmj.test.t9.render.Renderer

/** 画面の解像度を変えたときの処理を追加
 * - glViewportでビューポートを設定
 * - 投影行列の再計算
 * - フルスクにも対応させる
 */
object Test9_2 {
    lateinit var window: Window
    private lateinit var camera: Camera
    private lateinit var grassBlock: GrassBlock
    private lateinit var ground: Ground

    private fun init() {
        Renderer.init()
        camera = Camera(
            position = Vector3f(0.0f, 0.0f, 5.0f)
        )
        grassBlock = GrassBlock()
        ground = Ground(0.0f, -5.0f, 0.0f, 10.0f)

        GLFW.glfwSetFramebufferSizeCallback(window.id) { _, width, height ->
            GL33.glViewport(0, 0, width, height)
//            Renderer.setProjection(ProjectionMatrix.getPerspectiveMatrix(
//                width.toFloat() / height.toFloat()
//            ))
        }
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
        window = Window(1600, 1600, "Test9_2", true)
        KeyCallback(window).set(window.id)
        window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        window.show()

        init()
        loop()

        window.cleanup()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
