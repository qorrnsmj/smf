package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.util.ResourceUtils

class Shader(type: Int, file: String) {
    val id = glCreateShader(type)

    init {
        // Compiles shader from file
        val inputStream = ResourceUtils.getShader(file)
        glShaderSource(id, inputStream.readAllBytes().toString(Charsets.UTF_8))
        glCompileShader(id)
        inputStream.close()

        // Checks if compilation was successful
        val status = glGetShaderi(id, GL_COMPILE_STATUS)
        check(status == GL_TRUE) { glGetShaderInfoLog(id) }
    }

    fun delete() {
        glDeleteShader(id)
    }
}
