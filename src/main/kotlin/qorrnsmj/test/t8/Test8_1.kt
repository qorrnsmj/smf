package qorrnsmj.test.t8

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.graphic.shader.ShaderProgram

/** テクスチャをはりつける
 * - まずはRendererを使わずに
 */
object Test8_1 {
    private lateinit var window: Window
    private lateinit var program: ShaderProgram
//    private lateinit var vao: VertexArrayObject
//    private lateinit var vbo: VertexBufferObject
    private var textureID = 0

    private fun setShader() {
        // シェーダープログラムの作成
//        program = ShaderProgram().apply {
//            attachShader(Shader(GL_VERTEX_SHADER, "../../test/test8.vert"))
//            attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test8.frag"))
//            link()
//            use()
//        }

        // テクスチャ読み込み
        textureID = loadTexture("src/main/resources/test/test8_test.png")

        // 頂点データ (x, y, z, r, g, b, a, u, v)
        val vertices = floatArrayOf(
            -1.0f, -1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  0.0f, 0.0f, // 左下
             1.0f, -1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  1.0f, 0.0f, // 右下
             1.0f,  1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  1.0f, 1.0f, // 右上

            -1.0f, -1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  0.0f, 0.0f, // 左下
             1.0f,  1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  1.0f, 1.0f, // 右上
            -1.0f,  1.0f, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,  0.0f, 1.0f  // 左上
        )

        // VAO, VBOのバインド
//        vao = VertexArrayObject()
//        vbo = VertexBufferObject()
//        vao.bind()
//        vbo.bind(GL_ARRAY_BUFFER)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        // location = 0 (座標)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        // location = 1 (色)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 9 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)

        // location = 2 (テクスチャ座標)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 9 * Float.SIZE_BYTES, (7 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(2)

        // Uniform変数の設定
        program.apply {
//            setUniform("projection", ProjectionMatrix.getPerspectiveMatrix(1600f / 1600f))
//            setUniform("view",
//                ViewMatrix.getMatrix(
//                    eye = Vector3f(0f, 0f, 7f),
//                    center = Vector3f(0f, 0f, 0f),
//                    up = Vector3f(0f, 1f, 0f)
//                )
//            )
//            setUniform("model", Matrix4f())
        }
    }

    private fun loadTexture(filePath: String): Int {
        val textureID = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureID)

        // テクスチャのパラメータ設定
        // GL_TEXTURE_WRAP_S: 横方向のラップ方法
        // GL_TEXTURE_WRAP_T: 縦方向のラップ方法
        // GL_TEXTURE_MIN_FILTER: 縮小時のフィルタリング方法
        // GL_TEXTURE_MAG_FILTER: 拡大時のフィルタリング方法
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST) // GL_LINEAR
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST) // GL_LINEAR

        // テクスチャ画像読み込み
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            STBImage.stbi_set_flip_vertically_on_load(true)
            val image = STBImage.stbi_load(filePath, width, height, channels, 4)
                ?: throw RuntimeException("Failed to load texture: $filePath")

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, image)
            STBImage.stbi_image_free(image)
        }
        glBindTexture(GL_TEXTURE_2D, 0)

        return textureID
    }

    private fun loop() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (!window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // テクスチャのバインド
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, textureID)

            glDrawArrays(GL_TRIANGLES, 0, 6)
            window.update()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW" }
        window = Window(1600, 1600, "Test8_1", true)
        window.show()

        setShader()
        loop()

        program.delete()
//        vao.delete()
//        vbo.delete()
        window.cleanup()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }
}
