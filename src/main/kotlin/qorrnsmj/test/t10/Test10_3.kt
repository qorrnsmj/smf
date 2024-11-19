package qorrnsmj.test.t10

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.test.t10.game.Moon
import qorrnsmj.test.t10.render.Renderer

/** ライティングの実装
 * - Specular Light (反射光)
 * - [反射光によるライティング](https://wgld.org/d/webgl/w023.html)
 * - [Basic Lighting](https://learnopengl.com/Lighting/Basic-Lighting)
 */
object Test10_3 {
    lateinit var window: Window
    private lateinit var camera: Camera
    private lateinit var moon: Moon

    private fun init() {
        Renderer.init("test10_3")
        GLFW.glfwSetFramebufferSizeCallback(window.id) { _, width, height ->
            GL33.glViewport(0, 0, width, height)
            Renderer.setUniform("projection", MVP.getPerspectiveMatrix(
                width.toFloat() / height.toFloat()
            ))
        }

        camera = Camera(Vector3f(0.0f, 0.0f, 5.0f))
        moon = Moon()

        // Uniforms for Lightning
        Renderer.setUniform("lightDir", Vector3f(-1.0f, 0.0f, 0.0f))
        Renderer.setUniform("ambientColor", Vector3f(0.1f, 0.1f, 0.1f))
        Renderer.setUniform("specularStrength", 0.5f) // 反射光の強さ
        Renderer.setUniform("shininess", 32.0f) // 反射光の鋭さ
    }

    private fun loop() {
        val orbitRadius = 5.0f // 月の軌道半径（地球からの距離）
        val orbitSpeed = 0.01f  // 月の回転速度（角速度）

        while (!window.shouldClose()) {
            // camera input and update
            camera.processKeyboardInput(window)
            camera.processMouseMovement(window)
            Renderer.setUniform("view", MVP.getViewMatrix(
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
        window = Window(1600, 1600, "Test10_3", true)
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
