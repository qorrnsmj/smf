package qorrnsmj.test.t6

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram
import qorrnsmj.smf.graphic.shader.VertexArrayObject
import qorrnsmj.smf.graphic.shader.VertexBufferObject
import qorrnsmj.smf.core.window.Window

/** Test2, Test4をsmfのWindowクラス(GL33)で実装する
 * - GL32以降は、primitive-draw-callsは使えない
 * - (location = 0) に頂点
 * - (location = 1) に色
 */
object Test6_1 {
    private lateinit var window: Window
    private lateinit var program: ShaderProgram
    private lateinit var vao: VertexArrayObject
    private lateinit var vbo: VertexBufferObject

    private fun setShader() {
        program = ShaderProgram().apply {
            attachShader(Shader(GL_VERTEX_SHADER, "../../test/test6_1.vert"))
            attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test6_1.frag"))
            link()
            use()
        }

        // とりあえず三角形
        val vertices = floatArrayOf(
            // x, y, z, r, g, b, a
            -0.5f, -0.5f, 0.0f,   1.0f, 0.0f, 0.0f, 1.0f,
             0.5f, -0.5f, 0.0f,   0.0f, 1.0f, 0.0f, 1.0f,
             0.0f,  0.5f, 0.0f,   0.0f, 0.0f, 1.0f, 1.0f
            // glEnable(GL_BLEND) しないとalpha値が効かないかも
            // -> そもそも背景設定してないから分かんないや
        )

        // VAO, VBOのバインド
        vao = VertexArrayObject()
        vbo = VertexBufferObject()
        vao.bind()
        vbo.bind(GL_ARRAY_BUFFER)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        // 頂点 (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        // 色 (location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)
    }

    private fun loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            glDrawArrays(GL_TRIANGLES, 0, 3)

            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(800, 600, "Test6_1", true)
        window.show()

        setShader()
        loop()

        program.delete()
        vao.delete()
        vbo.delete()
        window.destroy()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
