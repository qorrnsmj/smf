package qorrnsmj.smf.util

import org.lwjgl.BufferUtils
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

object ResourceUtils {
    fun getResourceAsStream(path: String): InputStream {
        return ClassLoader.getSystemResourceAsStream(path)
            ?: error("Resource not found: $path")
    }

    fun getResourceAsDirectBuffer(path: String): ByteBuffer {
        val bytes = getResourceAsStream(path).readAllBytes()
        return BufferUtils.createByteBuffer(bytes.size)
            .put(bytes)
            .flip()
    }

    fun getFileAsDirectBuffer(path: Path): ByteBuffer {
        val bytes = Files.readAllBytes(path)
        return BufferUtils.createByteBuffer(bytes.size)
            .put(bytes)
            .flip()
    }

    fun getResourceAsDirectBuffer(byteBuffer: ByteBuffer): ByteBuffer {
        return if (byteBuffer.isDirect) {
            byteBuffer
        } else {
            BufferUtils.createByteBuffer(byteBuffer.remaining())
                .put(byteBuffer)
                .flip()
        }
    }
}
