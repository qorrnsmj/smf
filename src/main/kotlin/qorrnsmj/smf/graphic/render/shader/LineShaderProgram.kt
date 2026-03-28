package qorrnsmj.smf.graphic.render.shader

import org.lwjgl.opengl.GL33C.*
import java.nio.FloatBuffer
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

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
