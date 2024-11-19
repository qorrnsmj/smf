package qorrnsmj.test.t11

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.test.t11.core.render.Camera
import qorrnsmj.test.t11.core.render.MVP
import qorrnsmj.test.t11.core.render.MasterRenderer
import qorrnsmj.test.t11.game.entity.custom.Ship1
import qorrnsmj.test.t11.game.entity.custom.Ship3
import qorrnsmj.test.t11.game.entity.custom.Tree

/** OBJファイルの読み込みとレンダラーの最適化
 * - Mesh, Texture, Model, Entity
 * - Loader, OBJLoader
 * - MasterRenderer, EntityRenderer
 * - バッチ処理
 * - [OpenGL 3D Game Tutorial 2: VAOs and VBOs](https://www.youtube.com/watch?v=WMiggUPst-Q&list=PLRIWtICgwaX0u7Rf9zkZhLoLuZVfUksDP&index=2&ab_channel=ThinMatrix)
 * - [OpenGL 3D Game Tutorial 3: Rendering with Index Buffers](https://www.youtube.com/watch?v=z2yFlvkBbmk&list=PLRIWtICgwaX0u7Rf9zkZhLoLuZVfUksDP&index=3&ab_channel=ThinMatrix)
 * - [OpenGL 3D Game Tutorial 9: OBJ File Format](https://www.youtube.com/watch?v=KMWUjNE0fYI&list=PLRIWtICgwaX0u7Rf9zkZhLoLuZVfUksDP&index=9&ab_channel=ThinMatrix)
 * - [OpenGL 3D Game Tutorial 10: Loading 3D Models](https://www.youtube.com/watch?v=YKFYtekgnP8&list=PLRIWtICgwaX0u7Rf9zkZhLoLuZVfUksDP&index=12&ab_channel=ThinMatrix)
 * - [MayaとBlenderでメッシュの向き（ノーマル）を確認する方法の比較](https://redhologerbera.hatenablog.com/entry/2024/01/23/111845)
 */
object Test11_1 {
    lateinit var window: Window
    private lateinit var renderer: MasterRenderer
    private lateinit var camera: Camera

    private fun init() {
        camera = Camera(Vector3f(0f, 0f, 10f))
    }

    private fun loop() {
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        val tree = Tree().apply {
            scale = Vector3f(0.3f, 0.3f, 0.3f)
        }
        val ship1 = Ship1().apply {
            pos = Vector3f(10f, 0f, 0f)
            scale = Vector3f(10f, 10f, 10f)
        }
        val ship3 = Ship3().apply {
            pos = Vector3f(20f, 0f, 0f)
            scale = Vector3f(10f, 10f, 10f)
        }

        while (!window.shouldClose()) {
            // camera
            camera.processKeyboardInput(window)
            camera.processMouseMovement(window)
            renderer.updateViewMatrix(MVP.getViewMatrix(
                eye = camera.position,
                center = camera.position.add(camera.front),
                up = camera.up
            ))

            // render
            renderer.render(mutableListOf(tree, ship1, ship3))

            // update
            tree.rot.y += 0.1f
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // window
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test11_1", true)
        KeyCallback(window).set(window.id)
        window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        window.show()

        // renderer
        renderer = MasterRenderer.apply {
            updateModelMatrix(Matrix4f())
            updateViewMatrix(Matrix4f())
            updateProjectionMatrix(MVP.getPerspectiveMatrix(window.getBufferedWidth() / window.getBufferedHeight().toFloat()))
        }
        GLFW.glfwSetFramebufferSizeCallback(window.id) { _, width, height ->
            glViewport(0, 0, width, height)
            renderer.updateProjectionMatrix(
                MVP.getPerspectiveMatrix(width.toFloat() / height.toFloat()))
        }

        init()
        loop()

        window.cleanup()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
