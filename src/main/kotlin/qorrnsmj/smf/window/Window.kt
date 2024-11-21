package qorrnsmj.smf.window

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL11.glGetString
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.util.Resizable

class Window(width: Int, height: Int, title: String, vsync: Boolean = true) : Resizable {
    val id: Long
    var width = 0; private set
    var height = 0; private set
    var title = ""; private set
    var vsync = true; private set
    // TODO: widthBufferいらなくない？
    private val widthBuffer = MemoryUtil.memAllocInt(1)
    private val heightBuffer = MemoryUtil.memAllocInt(1)

    init {
        Logger.info("Window initializing...")

        this.width = width
        this.height = height
        this.title = title
        this.vsync = vsync

        // Creates a temporary window for getting the available OpenGL version
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        val tmpWindow = glfwCreateWindow(1, 1, "", MemoryUtil.NULL, MemoryUtil.NULL)
        glfwMakeContextCurrent(tmpWindow)
        GL.createCapabilities()
        glfwDestroyWindow(tmpWindow)

        // Checks version
        check(GL.getCapabilities().OpenGL33) {
            Logger.error("OpenGL 3.3 is not supported, you may want to update your graphics driver.")
        }

        // Sets window hints for OpenGL 3.3 core profile
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        // Creates window with specified OpenGL context
        id = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        check(id != MemoryUtil.NULL) {
            glfwTerminate()
            Logger.error("Failed to create the GLFW window!")
        }
        glfwMakeContextCurrent(id)
        GL.createCapabilities()

        // Enables v-sync
        if (vsync) {
            glfwSwapInterval(1)
        }

        // Centers window on screen
        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
        glfwSetWindowPos(id, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2)

        Logger.info("Created window with OpenGL version: ${glGetString(GL_VERSION)}")
        Logger.info("Window initialized!")
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
    fun cleanup() {
        glfwDestroyWindow(id)
        MemoryUtil.memFree(widthBuffer)
        MemoryUtil.memFree(heightBuffer)

        Logger.info("Window cleaned up!")
    }

    fun shouldClose(): Boolean {
        return glfwWindowShouldClose(id)
    }

    // TODO: Gameの方で処理する

    /* Setter */


    fun setInputMode(mode: Int, value: Int) {
        glfwSetInputMode(id, mode, value)
    }

    override fun resize(width: Int, height: Int) {
        glfwSetWindowSize(id, width, height)
    }

    fun toggleFullscreen() {
//        val monitor = if (glfwGetWindowMonitor(id) == MemoryUtil.NULL) glfwGetPrimaryMonitor() else MemoryUtil.NULL
//        glfwSetWindowMonitor(id, monitor, 0, 0, getBufferedWidth(), getBufferedHeight(), GLFW_DONT_CARE)

        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
        val width = videoMode.width()
        val height = videoMode.height()

        val monitor = if (glfwGetWindowMonitor(id) == MemoryUtil.NULL) glfwGetPrimaryMonitor() else MemoryUtil.NULL
        glfwSetWindowMonitor(id, monitor, 0, 0, width, height, GLFW_DONT_CARE)
    }

    // TODO: これいる？
    // TODO: MemoryUtil.memFree()
    // TODO: これはresizeしたときに、var width, heightを更新する時に使う
    fun getBufferedWidth(): Int {
        glfwGetFramebufferSize(id, widthBuffer, heightBuffer)
        val width = widthBuffer.get(0)
        widthBuffer.rewind()

        return width
    }

    fun getBufferedHeight(): Int {
        glfwGetFramebufferSize(id, widthBuffer, heightBuffer)
        val height = heightBuffer.get(0)
        heightBuffer.rewind()

        return height
    }
}
