package qorrnsmj.test.t10

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33
import qorrnsmj.smf.core.window.Window
import qorrnsmj.smf.graphic.render.Projection
import qorrnsmj.smf.graphic.render.View
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.test.t10.content.Moon
import qorrnsmj.test.t10.render.Renderer
import kotlin.math.cos
import kotlin.math.sin

/** ライティングの実装
 * - Ambient Light (環境光)
 * - [環境光によるライティング](https://wgld.org/d/webgl/w022.html)
 * - [Basic Lighting](https://learnopengl.com/Lighting/Basic-Lighting)
 */
object Test10_2 {
    lateinit var window: Window
    private lateinit var camera: Camera
    private lateinit var moon: Moon

    private fun init() {
        Renderer.init("test10_2")
        GLFW.glfwSetFramebufferSizeCallback(window.id) { _, width, height ->
            GL33.glViewport(0, 0, width, height)
            Renderer.setUniform("projection", Projection.getPerspectiveMatrix(
                width.toFloat() / height.toFloat()
            ))
        }

        camera = Camera(Vector3f(0.0f, 0.0f, 5.0f))
        moon = Moon()

        // Uniforms for Lightning
        Renderer.setUniform("lightDir", Vector3f(-1.0f, 0.0f, 0.0f))
        Renderer.setUniform("ambientColor", Vector3f(0.1f, 0.1f, 0.1f))
    }

    private fun loop() {
        val orbitRadius = 5.0f // 月の軌道半径（地球からの距離）
        val orbitSpeed = 0.01f  // 月の回転速度（角速度）

        while (!window.shouldClose()) {
            // camera input and update
            camera.processKeyboardInput(window)
            camera.processMouseMovement(window)
            Renderer.setUniform("view", View.getMatrix(
                eye = camera.position,
                center = camera.position.add(camera.front),
                up = camera.up
            ))

            // 月の位置と回転を更新
            //moon.x = (orbitRadius * cos(moon.angle)) // x座標は円周上を移動
            //moon.z = (orbitRadius * sin(moon.angle)) // z座標も円周上を移動
            moon.angle += orbitSpeed
            moon.updateSphere()

            // render
            Renderer.clear()
            moon.draw()

            // update
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test10_2", true)
        window.setKeyCallback(KeyCallback(window))
        window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        window.show()

        init()
        loop()

        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
