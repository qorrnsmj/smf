package qorrnsmj.smf.graphic.effect.shader

import org.lwjgl.opengl.GL33C.glBindAttribLocation
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class BlurHorizontalShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/blur_horizontal.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/blur_horizontal.frag")
) {
    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
    }
}
