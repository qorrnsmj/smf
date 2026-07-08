package qorrnsmj.smf.graphic.text

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class TextShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "text.vert"),
    Shader(GL_FRAGMENT_SHADER, "text.frag")
) {
    fun getUniformLocation(uniformName: String): Int {
        return glGetUniformLocation(id, uniformName)
    }
}
