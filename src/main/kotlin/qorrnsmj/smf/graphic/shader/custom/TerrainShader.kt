package qorrnsmj.smf.graphic.shader.custom

import org.lwjgl.opengl.GL33C.glBindAttribLocation
import org.lwjgl.opengl.GL33C.glDisableVertexAttribArray
import org.lwjgl.opengl.GL33C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram

class TerrainShader : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "terrain.vert"),
    Shader(GL_FRAGMENT_SHADER, "terrain.frag")
) {
    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        glBindAttribLocation(id, 1, "texCoord")
    }

    override fun enableAttributes() {
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
    }

    override fun disableAttributes() {
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
    }
}
