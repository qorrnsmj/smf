package qorrnsmj.smf.debug

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.capture.ScreenshotCapture
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenshotGpuTest {
    @Test
    fun capturesCompletedFrameWithCorrectOrientationAndRestoresPackState() {
        assumeTrue(System.getenv("SMF_RUN_GL_TESTS") == "1", "Requires a local OpenGL driver")
        check(glfwInit())
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        val window = glfwCreateWindow(33, 25, "Screenshot regression", 0, 0)
        check(window != 0L)
        try {
            glfwMakeContextCurrent(window)
            GL.createCapabilities()
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glDrawBuffer(GL_BACK)
            glClearColor(1f, 0f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glEnable(GL_SCISSOR_TEST)
            glScissor(0, 12, 33, 13)
            glClearColor(0f, 1f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glDisable(GL_SCISSOR_TEST)

            val packBuffer = glGenBuffers()
            glBindBuffer(GL_PIXEL_PACK_BUFFER, packBuffer)
            glBufferData(GL_PIXEL_PACK_BUFFER, 16L, GL_STREAM_READ)
            glPixelStorei(GL_PACK_ALIGNMENT, 8)
            glPixelStorei(GL_PACK_ROW_LENGTH, 99)
            glPixelStorei(GL_PACK_SKIP_ROWS, 2)
            glPixelStorei(GL_PACK_SKIP_PIXELS, 3)
            val directory = Files.createTempDirectory("smf-screenshot-test-")
            ScreenshotCapture(directory).use { capture ->
                assertTrue(capture.request("../escape").isCompletedExceptionally)
                val future = capture.request("orientation")
                capture.capturePending(window, "{\"frame\":42}")
                val result = future.get(10, TimeUnit.SECONDS)
                val image = ImageIO.read(result.path.toFile())
                assertEquals(33, image.width)
                assertEquals(25, image.height)
                assertEquals(0x00FF00, image.getRGB(10, 0) and 0xFFFFFF)
                assertEquals(0xFF0000, image.getRGB(10, 24) and 0xFFFFFF)
                assertEquals("{\"frame\":42}", Files.readString(result.path.resolveSibling(
                    result.path.fileName.toString().removeSuffix(".png") + ".json")))
            }
            assertEquals(packBuffer, glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING))
            assertEquals(8, glGetInteger(GL_PACK_ALIGNMENT))
            assertEquals(99, glGetInteger(GL_PACK_ROW_LENGTH))
            assertEquals(2, glGetInteger(GL_PACK_SKIP_ROWS))
            assertEquals(3, glGetInteger(GL_PACK_SKIP_PIXELS))
            assertEquals(GL_NO_ERROR, glGetError())
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            glDeleteBuffers(packBuffer)
        } finally {
            GL.setCapabilities(null)
            glfwDestroyWindow(window)
            glfwTerminate()
        }
    }
}
