package qorrnsmj.smf.graphic.render.shader

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class TextShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "text.vert"),
    Shader(GL_FRAGMENT_SHADER, "text.frag")
) {
    fun getUniformLocation(uniformName: String): Int {
        return glGetUniformLocation(id, uniformName)
    }
}