package qorrnsmj.smf.graphic.debug

import org.lwjgl.opengl.GL33C.*
import java.nio.FloatBuffer
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class LineShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "line.vert"),
    Shader(GL_FRAGMENT_SHADER, "line.frag")
) {
    
    private val mvpMatrixLocation: Int by lazy {
        glGetUniformLocation(id, "u_mvpMatrix")
    }
    
    fun loadMvpMatrix(mvpMatrix: FloatBuffer) {
        glUniformMatrix4fv(mvpMatrixLocation, false, mvpMatrix)
    }
}
