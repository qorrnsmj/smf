package qorrnsmj.test.t1

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil

/** 基本的な図形描画テスト
 * - Primitive Draw Call
 */
object Test1 {
    private val errorCallback = GLFWErrorCallback.createPrint(System.err)
    private val keyCallback = object : GLFWKeyCallback() {
        override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(window, true)
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        GLFW.glfwSetErrorCallback(errorCallback)
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        val window = GLFW.glfwCreateWindow(640, 480, "Test1", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) {
            GLFW.glfwTerminate()
            throw RuntimeException("Failed to create the GLFW window")
        }

        // ウィンドウ位置をセンターに設定
        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())!!
        GLFW.glfwSetWindowPos(
            window,
            (vidMode.width() - 640) / 2,
            (vidMode.height() - 480) / 2
        )

        // OpenGLのコンテキストを作成
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()

        // v-syncを有効化
        GLFW.glfwSwapInterval(1)

        // キーコールバックを設定
        GLFW.glfwSetKeyCallback(window, keyCallback)

        // ループで使うwidth, heightバッファー
        val width = MemoryUtil.memAllocInt(1)
        val height = MemoryUtil.memAllocInt(1)

        // メインループ
        while (!GLFW.glfwWindowShouldClose(window)) {
            // フレームサイズを取得
            GLFW.glfwGetFramebufferSize(window, width, height)
            val ratio = width.get() / height.get().toFloat()

            // 次の書き込みの為にリセット
            width.rewind()
            height.rewind()

            // ビューポートを設定 && 画面クリア
            glViewport(0, 0, width.get(), height.get())
            glClear(GL_COLOR_BUFFER_BIT)

            // 透視投影行列を設定
            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()
            glOrtho(-ratio.toDouble(), ratio.toDouble(), -1.0, 1.0, 1.0, -1.0)

            // 描画 (バッファする)
            render()

            // バッファーをスワップして、描画内容を画面に表示
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()

            // 次の読み込みの為にフリップ
            // 書き込みから読み込みに切り替えるために使う
            // 制限（limit）を現在のポジションに設定
            width.flip()
            height.flip()
        }

        MemoryUtil.memFree(width)
        MemoryUtil.memFree(height)
        GLFW.glfwDestroyWindow(window)
        keyCallback.free()

        GLFW.glfwTerminate()
        errorCallback.free()
    }

    private fun render() {
        glMatrixMode(GL_MODELVIEW)

        // 回転行列を設定
        glLoadIdentity()
        glRotatef(GLFW.glfwGetTime().toFloat() * 50f, 0f, 0f, 1f)

        // 四角形を描画
        glBegin(GL_QUADS)

        glColor3f(1f, 0f, 0f)
        glVertex3f(0.5f, -0.5f, 0f)

        glColor3f(0f, 1f, 0f)
        glVertex3f(0.5f, 0.5f, 0f)

        glColor3f(0f, 0f, 1f)
        glVertex3f(-0.5f, 0.5f, 0f)

        glColor3f(1f, 1f, 1f)
        glVertex3f(-0.5f, -0.5f, 0f)

        glEnd()
    }
}
