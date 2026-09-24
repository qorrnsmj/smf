package qorrnsmj.smf.graphic.capture

import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.imageio.ImageIO

class ScreenshotCapture(private val directory: Path = Path.of("screenshots")) : AutoCloseable {
    data class Result(val path: Path, val width: Int, val height: Int)
    private data class Request(val label: String, val result: CompletableFuture<Result>)
    private val requests = ArrayBlockingQueue<Request>(4)
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "screenshot-writer").apply { isDaemon = true } }
    @Volatile private var writing = false
    val hasPending: Boolean get() = !writing && requests.isNotEmpty()

    fun request(label: String = "game"): CompletableFuture<Result> {
        val result = CompletableFuture<Result>()
        if (!label.matches(Regex("[A-Za-z0-9_-]{1,64}"))) {
            result.completeExceptionally(IllegalArgumentException("Screenshot label must be 1-64 letters, digits, '_' or '-'."))
        } else if (!requests.offer(Request(label, result))) {
            result.completeExceptionally(IllegalStateException("Screenshot queue is full."))
        }
        return result
    }

    // Called on the render thread after UI/post-processing and before swapping buffers.
    fun capturePending(windowId: Long, metadata: String) {
        if (writing) return
        val request = requests.poll() ?: return
        if (request.result.isCancelled) return
        try {
            val width = IntArray(1)
            val height = IntArray(1)
            glfwGetFramebufferSize(windowId, width, height)
            val image = readBackBuffer(width[0], height[0])
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))
            val file = directory.toAbsolutePath().normalize()
                .resolve("${request.label}-$stamp-${UUID.randomUUID().toString().take(8)}.png")
            writing = true
            writer.execute {
                try {
                    Files.createDirectories(file.parent)
                    check(ImageIO.write(image, "png", file.toFile())) { "PNG encoder unavailable" }
                    Files.writeString(file.resolveSibling(file.fileName.toString().removeSuffix(".png") + ".json"), metadata)
                    request.result.complete(Result(file, image.width, image.height))
                } catch (exception: Exception) {
                    request.result.completeExceptionally(exception)
                } finally {
                    writing = false
                }
            }
        } catch (exception: Exception) {
            request.result.completeExceptionally(exception)
        }
    }

    internal fun readBackBuffer(width: Int, height: Int): BufferedImage {
        require(width > 0 && height > 0) { "Cannot capture a minimized or zero-sized framebuffer." }
        val bytes = Math.multiplyExact(Math.multiplyExact(width, height), 4)
        val pixels = MemoryUtil.memAlloc(bytes)
        val previousFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val previousPackBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING)
        val packParameters = intArrayOf(GL_PACK_ALIGNMENT, GL_PACK_ROW_LENGTH, GL_PACK_SKIP_ROWS, GL_PACK_SKIP_PIXELS)
        val previousPack = packParameters.map { glGetInteger(it) }
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        val previousReadBuffer = glGetInteger(GL_READ_BUFFER)
        try {
            glReadBuffer(GL_BACK)
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            packParameters.forEach { glPixelStorei(it, if (it == GL_PACK_ALIGNMENT) 1 else 0) }
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val row = IntArray(width)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val offset = ((height - y - 1) * width + x) * 4
                    row[x] = ((pixels.get(offset).toInt() and 255) shl 16) or
                        ((pixels.get(offset + 1).toInt() and 255) shl 8) or (pixels.get(offset + 2).toInt() and 255)
                }
                image.setRGB(0, y, width, 1, row, 0, width)
            }
            return image
        } finally {
            glReadBuffer(previousReadBuffer)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, previousFramebuffer)
            glBindBuffer(GL_PIXEL_PACK_BUFFER, previousPackBuffer)
            packParameters.forEachIndexed { index, parameter -> glPixelStorei(parameter, previousPack[index]) }
            MemoryUtil.memFree(pixels)
        }
    }

    override fun close() {
        while (true) {
            val request = requests.poll() ?: break
            request.result.completeExceptionally(IllegalStateException("Game closed."))
        }
        writer.shutdown()
        writer.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
    }
}
