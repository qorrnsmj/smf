package qorrnsmj.test.t1

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

/** 基本的な図形描画テスト */
object Test1 {
    /**
     * This error callback will simply print the error to
     * `System.err`.
     */
    private val errorCallback = GLFWErrorCallback.createPrint(System.err)

    /**
     * This key callback will check if ESC is pressed and will close the window
     * if it is pressed.
     */
    private val keyCallback = object : GLFWKeyCallback() {
        override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(window, true)
            }
        }
    }

    /**
     * The main function will create a 640x480 window and renders a rotating
     * triangle until the window gets closed.
     *
     * @param args the command line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        /* Set the error callback */
        GLFW.glfwSetErrorCallback(errorCallback)

        /* Initialize GLFW */
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        /* Create window */
        val window = GLFW.glfwCreateWindow(640, 480, "Simple example", MemoryUtil.NULL, MemoryUtil.NULL)
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

    private fun render() {
        glMatrixMode(GL_MODELVIEW)

        // Rotate matrix
        glLoadIdentity()
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
}
