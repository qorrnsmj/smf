package qorrnsmj.smf.graphic.`object`

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger

abstract class ShaderProgram(
    private val vertexShader: Shader,
    private val fragmentShader: Shader
) {
    val id = glCreateProgram()

    init {
        try {
            // Attach shaders and link program
            glAttachShader(id, vertexShader.id)
            glAttachShader(id, fragmentShader.id)
            bindAttributes()
            glLinkProgram(id)

            // Check if linking was successful
            val status = glGetProgrami(id, GL_LINK_STATUS)
            check(status == GL_TRUE) { glGetProgramInfoLog(id) }
            glValidateProgram(id)
        } catch (e: Exception) {
            Logger.error(e)
            delete()
        }
    }

    // TODO: useに全部変える (glUseProgram使ってるところ)
    fun use() {
        glUseProgram(id)
    }

    fun unuse() {
        glUseProgram(0)
    }

    protected abstract fun bindAttributes()

    fun delete() {
        vertexShader.delete()
        fragmentShader.delete()
        glDeleteProgram(id)
    }
}
