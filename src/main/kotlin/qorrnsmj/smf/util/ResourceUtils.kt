package qorrnsmj.smf.util

import java.io.InputStream
import java.lang.Exception

// TODO: Loader.loadと被るからこっちは消す
// TODO: Assimpに変えて、ResourceUtil消す
object ResourceUtils {
    fun getShader(file: String): InputStream {
        return getResourceAsStream("assets/shader/$file")
    }

    fun getModel(file: String): InputStream {
        return getResourceAsStream("assets/model/$file")
    }

    fun loadTexture(file: String): InputStream {
        return getResourceAsStream("assets/texture/$file")
    }

    private fun getResource(path: String): String {
        return ClassLoader.getSystemResource(path).path.drop(1)
    }

    private fun getResourceAsStream(path: String): InputStream {
        try {
            return ClassLoader.getSystemResourceAsStream(path)
                ?: throw IllegalArgumentException("Resource not found: $path")
        } catch (e: Exception) {
            e.printStackTrace()
            return InputStream.nullInputStream()
        }
    }
}
