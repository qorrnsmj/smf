package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33
import java.nio.file.Files
import java.nio.file.Paths

class Shader(type: Int, file: String) {
    val id = GL33.glCreateShader(type)
    private val path = Paths.get("src/main/resources/assets/shader/$file")

    init {
        GL33.glShaderSource(id, String(Files.readAllBytes(path)))
        GL33.glCompileShader(id)

        // check if compilation was successful
        val status = GL33.glGetShaderi(id, GL33.GL_COMPILE_STATUS)
        check(status == GL33.GL_TRUE) { GL33.glGetShaderInfoLog(id) }
    }

    fun delete() {
        GL33.glDeleteShader(id)
    }
}
