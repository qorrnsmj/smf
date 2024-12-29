package qorrnsmj.smf.graphic.effect.shader

import org.lwjgl.opengl.GL33C.glBindAttribLocation
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class SepiaShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/sepia.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/sepia.frag")
) {
    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        glBindAttribLocation(id, 1, "texCoord")
    }
}
