package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.shader.Shader

abstract class ShaderProgram(
    private val vertexShader: Shader,
    private val fragmentShader: Shader
) {
    val id = glCreateProgram()

    init {
        glAttachShader(id, vertexShader.id)
        glAttachShader(id, fragmentShader.id)
        bindAttributes()
        glLinkProgram(id)

        val status = glGetProgrami(id, GL_LINK_STATUS)
        check(status == GL_TRUE) { Logger.error(glGetProgramInfoLog(id)) }
        glValidateProgram(id)
    }

    protected abstract fun bindAttributes()

    abstract fun enableAttributes()

    abstract fun disableAttributes()

    fun delete() {
        vertexShader.delete()
        fragmentShader.delete()
        glDeleteProgram(id)
    }
}
