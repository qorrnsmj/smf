package qorrnsmj.test.t6

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.graphic.render.Projection
import qorrnsmj.smf.graphic.render.View
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram
import qorrnsmj.smf.graphic.shader.VertexArrayObject
import qorrnsmj.smf.graphic.shader.VertexBufferObject
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.core.window.Window
import kotlin.math.cos
import kotlin.math.sin

/** Triangle-Stripでの四角形の描画 & MVP行列での回転
 * - GL_QUADS は OpenGL3.1以降のコアプロファイルで非推奨
 * - 四角形回す (glRotatefは使えない。これも非推奨)
 * - 方法としてはuniform変数を使ってMVP行列を乗算する (Model-View-Projection)
 * - https://www.opengl-tutorial.org/beginners-tutorials/tutorial-3-matrices/#scaling-matrices
 */
object Test6_2 {
    private lateinit var window: Window
    private lateinit var program: ShaderProgram
    private lateinit var vao: VertexArrayObject
    private lateinit var vbo: VertexBufferObject
    private var angle = 0f

    private fun setShader() {
        // とりあえず四角形
        val vertices = floatArrayOf(
            // x, y, z, r, g, b, a
            -0.5f, -0.5f, 0.0f,   1.0f, 0.0f, 0.0f, 1.0f,
             0.5f, -0.5f, 0.0f,   0.0f, 1.0f, 0.0f, 1.0f,
            -0.5f,  0.5f, 0.0f,   0.0f, 0.0f, 1.0f, 1.0f,
             0.5f,  0.5f, 0.0f,   1.0f, 1.0f, 0.0f, 1.0f
        )

        // VAO, VBO, ShaderProgram
        vao = VertexArrayObject().bind()
        vbo = VertexBufferObject().bind(GL_ARRAY_BUFFER).uploadData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        program = ShaderProgram().apply {
            attachShader(Shader(GL_VERTEX_SHADER, "../../test/test6_2.vert"))
            attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test6_2.frag"))
            link()
            use()
        }

        // 頂点 (location = 0) aPos
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        // 色 (location = 1) aColor
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)

        // uniform変数 (MVP行列) の設定
        setUniforms()
    }

    private fun setUniforms() {
        // TODO: scaling * rotation で出来るようにする (kotlinの機能で)
        program.setUniform("uModel",
            Matrix4f( // z軸回転の回転行列
                Vector4f(cos(angle), -sin(angle), 0.0f, 0.0f),
                Vector4f(sin(angle),  cos(angle), 0.0f, 0.0f),
                Vector4f(0.0f, 0.0f, 1.0f, 0.0f),
                Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
            )
        )

        program.setUniform("uView",
            View.getMatrix( // OpenGLは右手系座標
                Vector3f(0f, 3f, 2f), // Camera position
                Vector3f(0f, 0f, 0f), // Look at point
                Vector3f(0f, 1f, 0f)  // Up vector
            )
        )

        val aspect = window.getWidth() / window.getHeight().toFloat()
        program.setUniform("uProj", // 透視投影
            Projection.getPerspectiveMatrix(aspect)
        )
    }

    private fun loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

            angle += 0.02f
            setUniforms()

            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test6_2", true)
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
