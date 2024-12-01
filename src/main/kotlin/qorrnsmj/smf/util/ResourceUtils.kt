package qorrnsmj.smf.util

import org.lwjgl.BufferUtils
import org.tinylog.kotlin.Logger
import java.io.InputStream
import java.lang.Exception
import java.nio.ByteBuffer

// TODO: Loader.loadと被るからこっちは消す
// TODO: Assimpに変えて、ResourceUtil消す
object ResourceUtils {
    fun getResourceAsStream(path: String): InputStream {
        try {
            return ClassLoader.getSystemResourceAsStream(path)
                ?: throw IllegalArgumentException("Resource not found: $path")
        } catch (e: Exception) {
            Logger.error(e)
            return InputStream.nullInputStream()
        }
    }

    fun getResourceAsByteBuffer(path: String): ByteBuffer {
        val bytes = getResourceAsStream(path).readAllBytes()
        val buffer = BufferUtils.createByteBuffer(bytes.size).apply {
            put(bytes)
            flip()
        }

        return buffer
    }
}
