package qorrnsmj.smf.graphic.shader.custom

import org.lwjgl.opengl.GL33C.glBindAttribLocation
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram

class BlurVerticalShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/blur_vertical.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/blur_vertical.frag")
) {
    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        //glBindAttribLocation(id, 1, "texCoord") // TODO
    }
}
