package qorrnsmj.test.t3

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import kotlin.math.tan

/** Perspective projectionのテスト (OpenGLは右手系) (このテストは画面比率変更に対応してない)
 * https://yttm-work.jp/gmpg/gmpg_0003.html
 * */
object Test3 {
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
        /* Set the error callback */
        GLFW.glfwSetErrorCallback(errorCallback)

        /* Initialize GLFW */
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        /* Create window */
        val window = GLFW.glfwCreateWindow(640, 480, "Test3", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) {
            GLFW.glfwTerminate()
            throw RuntimeException("Failed to create the GLFW window")
        }

        /* Center the window on screen */
        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())!!
        GLFW.glfwSetWindowPos(
            window,
            (vidMode.width() - 640) / 2,
            (vidMode.height() - 480) / 2
        )

        /* Create OpenGL context */
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()

        /* Enable vertical synchronization */
        GLFW.glfwSwapInterval(1)

        /* Set the key callback */
        GLFW.glfwSetKeyCallback(window, keyCallback)

        /* Declare buffers for using inside the loop */
        val width = MemoryUtil.memAllocInt(1)
        val height = MemoryUtil.memAllocInt(1)

        /* Loop until window gets closed */
        while (!GLFW.glfwWindowShouldClose(window)) {
            /* Get width and height to calculate the ratio */
            GLFW.glfwGetFramebufferSize(window, width, height)
            val ratio = width.get() / height.get().toFloat()

            /* Rewind buffers for next get */
            width.rewind()
            height.rewind()

            /* Set viewport and clear screen */
            glViewport(0, 0, width.get(), height.get())
            glClear(GL_COLOR_BUFFER_BIT)

            /* Set orthographic projection */
            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()
            glOrtho(-ratio.toDouble(), ratio.toDouble(), -1.0, 1.0, 1.0, -1.0)

            render()

            /* Swap buffers and poll Events */
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()

            /* Flip buffers for next loop */
            width.flip()
            height.flip()
        }

        /* Free buffers */
        MemoryUtil.memFree(width)
        MemoryUtil.memFree(height)

        /* Release window and its callbacks */
        GLFW.glfwDestroyWindow(window)
        keyCallback.free()

        /* Terminate GLFW and release the error callback */
        GLFW.glfwTerminate()
        errorCallback.free()
    }

    /**
     * これで、描画される四角形はパースペクティブ投影でレンダリングされ、カメラは(0, 0, 5)の位置に配置され、原点(0, 0, 0)を見ている状態になる
     * -> shaderを使う場合は "mvp行列" = "projection行列" * "view行列" * "model行列" で計算する
     * */
    private fun render() {
        // Set the projection matrix
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(45.0, 800.0 / 600.0, 0.1, 100.0)

        // Set the model-view matrix
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        gluLookAt(
            Vector3f(0f, -2f, 0f), // Camera position
            Vector3f(0f, 0f, 0f), // Look at point
            Vector3f(0f, 1f, 0f) // Up vector
        )

        // Rotate the model
        glRotatef(GLFW.glfwGetTime().toFloat() * 50f, 0f, 0f, 1f)

        // Render shape
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

    /** 指定された視野角、アスペクト比、近クリッピング平面および遠クリッピング平面に基づいて投影行列を設定 */
    private fun gluPerspective(fovY: Double, aspect: Double, zNear: Double, zFar: Double) {
        val fH = tan(fovY / 360.0 * Math.PI) * zNear
        val fW = fH * aspect
        glFrustum(-fW, fW, -fH, fH, zNear, zFar)
    }

    /**
     * カメラの位置、注視点、およびアップベクトルを使用してビュー行列を設定。
     * (アップベクトルは、カメラの「上」を定義するベクトル。カメラがどの方向に向いていても、カメラの上方向を決定する。
     * これはカメラのロール角（傾き）を制御し、カメラの垂直方向を安定さる。)
     */
    private fun gluLookAt(eye: Vector3f, center: Vector3f, up: Vector3f) {
        // カメラの向いてる方向のベクトルと、上方向のベクトルを正規化したもの
        val forward = center.subtract(eye).normalize()
        var upside = up.normalize()

        // forwardとupの外積を求める。これはカメラの右方向へのベクトルを表す
        val side = forward.cross(upside).normalize()

        // sideとforwardの外積を求める。これはカメラの上方向へのベクトルを表す (upベクトルの再計算)
        // -> 入力として与えられたupベクトルは必ずしもカメラの視線（forwardベクトル）に対して完全に直交しているとは限らない
        // そのため、forwardベクトルと直交するようにupベクトルを再計算する必要がある
        upside = side.cross(forward)

        // カメラ座標変換用の回転行列、つまりview行列 (ワールド座標軸 -> カメラ座標軸)
        // | Xx Xy Xz 0 | (x軸)
        // | Yx Yy Yz 0 | (y軸)
        // | Zx Zy Zz 0 | (z軸)
        // | 0  0  0  1 |
        val viewMatrix = Matrix4f(
            Vector4f(side.x, upside.x, -forward.x, 0f),
            Vector4f(side.y, upside.y, -forward.y, 0f),
            Vector4f(side.z, upside.z, -forward.z, 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        // カメラの位置を原点に移動させる。(ビュー行列と平行移動行列を合成)
        val m = viewMatrix.multiply(
            Matrix4f(
                Vector4f(1f, 0f, 0f, 0f),
                Vector4f(0f, 1f, 0f, 0f),
                Vector4f(0f, 0f, 1f, 0f),
                Vector4f(-eye.x, -eye.y, -eye.z, 1f)
            )
        )

        // 適用
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4 * 4)
            m.toBuffer(buffer)
            glMultMatrixf(buffer)
        }
    }

    /*private fun gluLookAt(eyeX: Double, eyeY: Double, eyeZ: Double,
                  centerX: Double, centerY: Double, centerZ: Double,
                  upX: Double, upY: Double, upZ: Double) {

        // カメラの向きと上方向のベクトル
        val forward = floatArrayOf(
            (centerX - eyeX).toFloat(),
            (centerY - eyeY).toFloat(),
            (centerZ - eyeZ).toFloat()
        )
        val up = floatArrayOf(upX.toFloat(), upY.toFloat(), upZ.toFloat())

        // forwardを正規化
        val fLen = sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2].toDouble()).toFloat()
        forward[0] /= fLen
        forward[1] /= fLen
        forward[2] /= fLen

        // upを正規化
        val uLen = sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2].toDouble()).toFloat()
        up[0] /= uLen
        up[1] /= uLen
        up[2] /= uLen


        // forwardとupの外積を求める。これはカメラの右方向へのベクトルを表す
        val side = floatArrayOf(
            forward[1] * up[2] - forward[2] * up[1],
            forward[2] * up[0] - forward[0] * up[2],
            forward[0] * up[1] - forward[1] * up[0]
        )

        // sideとforwardの外積を求める。これはカメラの上方向へのベクトルを表す (upベクトルの再計算)
        // -> 入力として与えられたupベクトルは必ずしもカメラの視線（forwardベクトル）に対して完全に直交しているとは限らない
        // そのため、forwardベクトルと直交するようにupベクトルを再計算する必要がある
        up[0] = side[1] * forward[2] - side[2] * forward[1]
        up[1] = side[2] * forward[0] - side[0] * forward[2]
        up[2] = side[0] * forward[1] - side[1] * forward[0]

        // カメラ座標変換用の回転行列、つまりview行列 (ワールド座標軸 -> カメラ座標軸)
        // | Xx Xy Xz 0 | (x軸)
        // | Yx Yy Yz 0 | (y軸)
        // | Zx Zy Zz 0 | (z軸)
        // | 0  0  0  1 |
        val m = floatArrayOf(
            side[0], up[0], -forward[0], 0f,
            side[1], up[1], -forward[1], 0f,
            side[2], up[2], -forward[2], 0f,
            0f,      0f,    0f,          1f
        )

        // 乗算
        glMultMatrixf(m)

        // カメラの位置を原点に移動させる。カメラの位置に対して平行移動をする
        glTranslatef(-eyeX.toFloat(), -eyeY.toFloat(), -eyeZ.toFloat())
    }*/
}
