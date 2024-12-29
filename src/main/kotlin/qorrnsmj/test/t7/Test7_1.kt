package qorrnsmj.test.t7

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.graphic.`object`.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * FloatArrayじゃなくてFloatBufferでできるか試す
 * - FloatBufferを直接使わず、ByteBufferを介してFloatBufferに変換する (それかMemoryUtil)
 * - 展開するメモリ領域が違うので、この方が効率的になる
 * - https://stackoverflow.com/questions/10697161/why-floatbuffer-instead-of-float
 */
object Test7_1 {
    private lateinit var window: Window
//    private lateinit var vao: VertexArrayObject
//    private lateinit var vbo: VertexBufferObject
    private lateinit var program: ShaderProgram
    private var angle = 0f

    private fun init() {
//        vao = VertexArrayObject().bind()
//        vbo = VertexBufferObject().bind(GL_ARRAY_BUFFER)
//        program = ShaderProgram().apply {
//            attachShader(Shader(GL_VERTEX_SHADER, "../../test/test7.vert"))
//            attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test7.frag"))
//            link()
//            use()
//
//            // 頂点 (location = 0) pos
//            glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
//            glEnableVertexAttribArray(0)
//
//            // 色 (location = 1) color
//            glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
//            glEnableVertexAttribArray(1)
//
//            // uniform変数の設定
//            setUniform("projection", ProjectionMatrix.getPerspectiveMatrix(1600f / 1600f))
//            setUniform("view", ViewMatrix.getMatrix(
//                eye = Vector3f(0f, 0f, 7f),
//                center = Vector3f(0f, 0f, 0f),
//                up = Vector3f(0f, 1f, 0f)
//            ))
//            setUniform("model",
//                Matrix4f( // z軸回転の回転行列
//                    Vector4f(cos(angle), -sin(angle), 0.0f, 0.0f),
//                    Vector4f(sin(angle),  cos(angle), 0.0f, 0.0f),
//                    Vector4f(0.0f, 0.0f, 1.0f, 0.0f),
//                    Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//                ).multiply(
//                    Matrix4f( // x軸回転の回転行列
//                        Vector4f(1.0f, 0.0f, 0.0f, 0.0f),
//                        Vector4f(0.0f, cos(angle), -sin(angle), 0.0f),
//                        Vector4f(0.0f, sin(angle),  cos(angle), 0.0f),
//                        Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//                    )
//                ).multiply(
//                    Matrix4f( // y軸回転の回転行列
//                        Vector4f( cos(angle), 0.0f, sin(angle), 0.0f),
//                        Vector4f(0.0f, 1.0f, 0.0f, 0.0f),
//                        Vector4f(-sin(angle), 0.0f, cos(angle), 0.0f),
//                        Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//                    )
//                )
//            )
//        }

        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glEnable(GL_DEPTH_TEST)
        glCullFace(GL_BACK)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun setUniforms() {
//        program.setUniform("projection", ProjectionMatrix.getPerspectiveMatrix(1600f / 1600f))
//        program.setUniform("view", ViewMatrix.getMatrix(
//            eye = Vector3f(0f, 0f, 7f),
//            center = Vector3f(0f, 0f, 0f),
//            up = Vector3f(0f, 1f, 0f)
//        ))
//        program.setUniform("model",
//            Matrix4f( // y軸回転の回転行列
//                Vector4f( cos(angle), 0.0f, sin(angle), 0.0f),
//                Vector4f(0.0f, 1.0f, 0.0f, 0.0f),
//                Vector4f(-sin(angle), 0.0f, cos(angle), 0.0f),
//                Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//            )
//        )
    }

    private fun loop() {
        val vertices = floatArrayOf(
            -1f, -1f, 0f,  1f, 0f, 0f, 1f,
            1f, -1f, 0f,  0f, 1f, 0f, 1f,
            0f,  1f, 0f,  0f, 0f, 1f, 1f
        )

        // MemoryUtilを使ったっていい
        val array = ByteBuffer.allocateDirect(1024)
            .order(ByteOrder.nativeOrder()) // 動作環境のエンディアンに合わせる
            .asFloatBuffer()
            .put(vertices)
            .flip() // バッファの読み取り位置を先頭に戻す

//        vbo.uploadData(GL_ARRAY_BUFFER, array, GL_STATIC_DRAW)

        while (!window.shouldClose()) {
            setUniforms()

            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glDrawArrays(GL_TRIANGLES, 0, 3)

            angle += 0.02f
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test7_1", true)
        window.show()

        init()
        loop()

        window.cleanup()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
