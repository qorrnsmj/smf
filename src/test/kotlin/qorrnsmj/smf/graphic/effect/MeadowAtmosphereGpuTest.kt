package qorrnsmj.smf.graphic.effect

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.GL_LINK_STATUS
import org.lwjgl.opengl.GL33C.GL_NO_ERROR
import org.lwjgl.opengl.GL33C.GL_TRUE
import org.lwjgl.opengl.GL33C.glGetError
import org.lwjgl.opengl.GL33C.glGetProgrami
import qorrnsmj.smf.graphic.effect.ColorEffect
import qorrnsmj.smf.graphic.effect.ContrastEffect
import qorrnsmj.smf.math.Vector3f
import kotlin.test.Test
import kotlin.test.assertEquals

class GenericPostEffectGpuTest {
    @Test
    fun reusableColorAndContrastEffectsLinkAndAcceptPresets() {
        assumeTrue(System.getenv("SMF_RUN_GL_TESTS") == "1", "Requires a local OpenGL driver")
        check(glfwInit())
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        val window = glfwCreateWindow(16, 16, "Generic post-process regression", 0, 0)
        check(window != 0L)
        try {
            glfwMakeContextCurrent(window)
            GL.createCapabilities()
            val color = ColorEffect().apply {
                exposure = 1.1f
                saturation = 0.8f
                lift = Vector3f(0.02f, 0.03f, 0.04f)
                tint = Vector3f(0.9f, 1f, 1.1f)
            }
            val contrast = ContrastEffect().apply { this.contrast = 0.9f }
            listOf(color, contrast).forEach { effect ->
                assertEquals(GL_TRUE, glGetProgrami(effect.program.id, GL_LINK_STATUS))
                effect.use()
                assertEquals(GL_NO_ERROR, glGetError())
            }
        } finally {
            GL.setCapabilities(null)
            glfwDestroyWindow(window)
            glfwTerminate()
        }
    }
}
