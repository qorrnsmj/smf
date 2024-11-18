package qorrnsmj.test.t11.core.render.shader

import org.lwjgl.opengl.GL33C.*
import java.nio.file.Files
import java.nio.file.Paths

class Shader(type: Int, file: String) {
    val id = glCreateShader(type)

    init {
        // compile shader from file
        val path = Paths.get("src/main/resources/assets/shader/$file")
        glShaderSource(id, String(Files.readAllBytes(path)))
        glCompileShader(id)

        // check if compilation was successful
        val status = glGetShaderi(id, GL_COMPILE_STATUS)
        check(status == GL_TRUE) { glGetShaderInfoLog(id) }
    }

    fun delete() {
        glDeleteShader(id)
    }
}
