package qorrnsmj.smf.core.window

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL11.glGetString
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger

class Window(
    width: Int,
    height: Int,
    title: CharSequence,
    val isVSyncEnabled: Boolean = true
) {
    val id: Long
    private val keyCallback = KeyCallback()
    private val widthBuffer = MemoryUtil.memAllocInt(1)
    private val heightBuffer = MemoryUtil.memAllocInt(1)

    init {
        Logger.info("Window initializing")

        // TODO: これいる？ 直接id = glfwCreateWindow()でいい？
        // Creates a temporary window for getting the available OpenGL version
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        val temp = glfwCreateWindow(1, 1, "", MemoryUtil.NULL, MemoryUtil.NULL)
        glfwMakeContextCurrent(temp)
        GL.createCapabilities()
        glfwDestroyWindow(temp)

        // Checks version
        check (GL.getCapabilities().OpenGL33) {
            "OpenGL 3.3 is not supported, you may want to update your graphics driver."
        }

        // Sets window hints for OpenGL 3.3 core profile
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        // FIXME: Projectionとかも画面サイズ変更に対応させる
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        // Creates window with specified OpenGL context
        id = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        //id = glfwCreateWindow(width, height, title, glfwGetPrimaryMonitor(), MemoryUtil.NULL)
        check(id != MemoryUtil.NULL) {
            glfwTerminate() // TODO: これはGameクラスでcatchしてそこに書いていいかも
            "Failed to create the GLFW window!"
        }
        glfwMakeContextCurrent(id)
        GL.createCapabilities()

        // Enables v-sync
        if (isVSyncEnabled) {
            glfwSwapInterval(1)
        }

        // Sets key callback
        glfwSetKeyCallback(id, keyCallback)

        // Centers window on screen
        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
        glfwSetWindowPos(id, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2)

        // TODO: Rendererに書く
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glEnable(GL_BLEND)

        Logger.info("Created window with OpenGL version: {}", glGetString(GL_VERSION))
        Logger.info("Window initialized")
    }

    /**
     * Shows the window.
     */
    fun show() {
        glfwShowWindow(id)
    }

    /**
     * Hides the window.
     */
    fun hide() {
        glfwHideWindow(id)
    }

    /**
     * Updates the screen.
     */
    fun update() {
        glfwSwapBuffers(id)
        glfwPollEvents()
    }

    /**
     * Destroys the window and releases its callbacks.
     */
    fun destroy() {
        glfwDestroyWindow(id)
        keyCallback.free()
    }

    fun shouldClose(): Boolean {
        return glfwWindowShouldClose(id)
    }

    // FIXME
    fun toggleFullscreen() {
        glfwSetWindowMonitor(id,
            if (glfwGetWindowMonitor(id) == MemoryUtil.NULL) glfwGetPrimaryMonitor() else MemoryUtil.NULL,
            0, 0, getWidth(), getHeight(), GLFW_DONT_CARE
        )
    }

    // TODO: これいる？
    // TODO: MemoryUtil.memFree()
    fun getWidth(): Int {
        glfwGetFramebufferSize(id, widthBuffer, heightBuffer)
        val width = widthBuffer.get(0)
        widthBuffer.rewind()

        return width
    }

    fun getHeight(): Int {
        glfwGetFramebufferSize(id, widthBuffer, heightBuffer)
        val height = heightBuffer.get(0)
        heightBuffer.rewind()

        return height
    }
}
