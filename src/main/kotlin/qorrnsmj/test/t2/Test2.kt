package qorrnsmj.test.t2

import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil

/**
 * ShaderProgramのテスト
 * @see <a href="https://camo.qiitausercontent.com/868873738db43d5bb31bc37657bc0b874e7046bd/68747470733a2f2f71696974612d696d6167652d73746f72652e73332e616d617a6f6e6177732e636f6d2f302f3136363330392f66306665323761322d343362662d393863312d353739332d3438626433333330353733662e706e67
 * ">image!</a>
 */
object Test2 {
    private var window = MemoryUtil.NULL

    private fun init() {
        GLFWErrorCallback.createPrint(System.err).set()
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)

        window = GLFW.glfwCreateWindow(800, 600, "Test2", MemoryUtil.NULL, MemoryUtil.NULL)
        check(window != MemoryUtil.NULL) { "Failed to create the GLFW window" }

        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)

        GL.createCapabilities()
    }

    private fun loop() {
//        ShaderProgram().apply {
//            attachShader(Shader(GL_VERTEX_SHADER, "../../test/test2.vert"))
//            attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test2.frag"))
//            link()
//            use()
//        }
//        val vao = VertexArrayObject()
//        val vbo = VertexBufferObject()
        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.0f, 0.5f, 0.0f
        )

        // VAO, VBOを生成
//        vao.bind() // vaoを現在のコンテキストにバインド。以降の操作は、このバインドされたVAOに対して行われる
//        vbo.bind(GL_ARRAY_BUFFER) // vboをGL_ARRAY_BUFFER用に現在のコンテキストにバインド
//        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW) // 現在バインドされているバッファに対してデータを格納する

        // 頂点属性ポインタを設定 (どのように頂点バッファ内のデータをシェーダーに渡すかを設定)
        // 0        : layout (location = 0)のようにシェーダー内で指定した位置に対応
        // 3        : 1頂点あたりの要素数
        // GL_FLOAT : 要素の型
        // false    : 正規化するかどうか
        // 3 * 4    : 1頂点あたりのバイト数 (ストライド)
        // 0        : バッファの先頭からのオフセット
//        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4, 0)
//        glEnableVertexAttribArray(0) // 有効化

        while (!GLFW.glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // Render
            glDrawArrays(GL_TRIANGLES, 0, 3) // 3つの頂点を描画

            // Update
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }

//        vao.delete()
//        vbo.delete()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        init()
        loop()
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
