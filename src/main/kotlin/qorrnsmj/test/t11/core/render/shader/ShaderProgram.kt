package qorrnsmj.test.t11.core.render.shader

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.math.*

// TODO: シェーダーはこの2つ以外も設定できるからattachメソッドに変える
// TODO: SMFのShaderProgramと合わせる (向こうはSetUniformを持ってる)
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
        check(status == GL_TRUE) { glGetProgramInfoLog(id) }
        glValidateProgram(id)
    }

    protected abstract fun bindAttributes()

    fun delete() {
        vertexShader.delete()
        fragmentShader.delete()
        glDeleteProgram(id)
    }
}
