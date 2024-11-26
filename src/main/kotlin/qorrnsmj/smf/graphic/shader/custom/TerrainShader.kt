package qorrnsmj.smf.graphic.shader.custom

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram

object TerrainShader : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "terrain.vert"),
    Shader(GL_FRAGMENT_SHADER, "terrain.frag")
) {
    override fun bindAttributes() {
    }

    override fun enableAttributes() {
    }

    override fun disableAttributes() {
    }
}
