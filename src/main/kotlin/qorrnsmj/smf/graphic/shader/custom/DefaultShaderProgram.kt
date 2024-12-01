package qorrnsmj.smf.graphic.shader.custom

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram

class DefaultShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "default.vert"),
    Shader(GL_FRAGMENT_SHADER, "default.frag")
) {
    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        glBindAttribLocation(id, 1, "texCoords")
        glBindAttribLocation(id, 2, "normal")
        glBindAttribLocation(id, 3, "tangent")
    }
}
