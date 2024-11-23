package qorrnsmj.smf.graphic.shader.custom

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram
import qorrnsmj.smf.graphic.shader.custom.DefaultShader.Uniform.*

// TODO: Objectにすべきじゃないかも、同じシェーダーで別のuniformを使いたい場合とか
object DefaultShader : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "default.vert"),
    Shader(GL_FRAGMENT_SHADER, "default.frag")
) {
    init {
    }

    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        glBindAttribLocation(id, 1, "texCoords")
        glBindAttribLocation(id, 2, "normal")
    }

    // TODO: DefaultShaderをクラスにするなら、enumのlocationは消して、uniformLocationMapをつくる
    enum class Uniform(val location: Int) {
        MODEL(glGetUniformLocation(id, "model")),
        VIEW(glGetUniformLocation(id, "view")),
        PROJECTION(glGetUniformLocation(id, "projection"))
    }
}
