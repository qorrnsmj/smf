package qorrnsmj.smf.util

import org.lwjgl.BufferUtils
import java.io.InputStream
import java.nio.ByteBuffer

object ResourceUtils {
    fun getResourceAsStream(path: String): InputStream {
        return ClassLoader.getSystemResourceAsStream(path)
            ?: error("Resource not found: $path")
    }

    fun getResourceAsDirectBuffer(filePath: String): ByteBuffer {
        val bytes = getResourceAsStream(filePath).readAllBytes()
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
