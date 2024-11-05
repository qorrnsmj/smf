package qorrnsmj.test.t4

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import qorrnsmj.smf.graphic.render.View
import qorrnsmj.smf.graphic.render.Projection
import qorrnsmj.smf.math.Vector3f

/** Test3をsmfのクラスで再現できてるか確認 -> 成功！！ */
object Test4 {
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
        val window = GLFW.glfwCreateWindow(640, 480, "Test4", MemoryUtil.NULL, MemoryUtil.NULL)
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

            /* Rewind buffers for next get */
            width.rewind()
            height.rewind()

            /* Set viewport and clear screen */
            glViewport(0, 0, width.get(), height.get())
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

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
        // Set the projection matrix
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(45.0, 800.0 / 600.0, 0.1, 100.0)

        // Set the model-view matrix
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        gluLookAt(
            Vector3f(0f, -2f, 2f), // Camera position
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

    private fun gluPerspective(fovY: Double, aspect: Double, zNear: Double, zFar: Double) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4 * 4)
            val m = Projection.getPerspectiveMatrix(aspect.toFloat())

            m.toBuffer(buffer)
            glMultMatrixf(buffer)
        }
    }

    private fun gluLookAt(eye: Vector3f, center: Vector3f, up: Vector3f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4 * 4)
            val m = View.getMatrix(eye, center, up)

            m.toBuffer(buffer)
            glMultMatrixf(buffer)
        }
    }
}
