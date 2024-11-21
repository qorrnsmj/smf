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
        MODEL(glGetUniformLocation(DefaultShader.id, "model")),
        VIEW(glGetUniformLocation(DefaultShader.id, "view")),
        PROJECTION(glGetUniformLocation(DefaultShader.id, "projection")),

        LIGHT_POSITION(glGetUniformLocation(DefaultShader.id, "lightPos")),
        AMBIENT_COLOR(glGetUniformLocation(DefaultShader.id, "ambientColor")),
        SPECULAR_STRENGTH(glGetUniformLocation(DefaultShader.id, "specularStrength")),
        SHININESS(glGetUniformLocation(DefaultShader.id, "shininess")),

        CONSTANT(glGetUniformLocation(DefaultShader.id, "constant")),
        LINEAR(glGetUniformLocation(DefaultShader.id, "linear")),
        QUADRATIC(glGetUniformLocation(DefaultShader.id, "quadratic"))
    }
}
