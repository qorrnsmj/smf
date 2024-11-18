package qorrnsmj.test.t11.core.render.shader

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.test.t11.core.render.UniformType

class EntityShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "../../test/test11.vert"),
    Shader(GL_FRAGMENT_SHADER, "../../test/test11.frag")
) {
    val uniformLocationMap = mapOf(
        UniformType.PROJECTION to glGetUniformLocation(id, "projection"),
        UniformType.VIEW to glGetUniformLocation(id, "view"),
        UniformType.MODEL to glGetUniformLocation(id, "model"),

        UniformType.AMBIENT_COLOR to glGetUniformLocation(id, "ambientColor"),
        UniformType.SPECULAR_STRENGTH to glGetUniformLocation(id, "specularStrength"),
        UniformType.SHININESS to glGetUniformLocation(id, "shininess"),

        UniformType.LIGHT_POSITION to glGetUniformLocation(id, "lightPos"),
        UniformType.CONSTANT to glGetUniformLocation(id, "constant"),
        UniformType.LINEAR to glGetUniformLocation(id, "linear"),
        UniformType.QUADRATIC to glGetUniformLocation(id, "quadratic")
    )

    override fun bindAttributes() {
        glBindAttribLocation(id, 0, "position")
        glBindAttribLocation(id, 1, "texCoords")
        glBindAttribLocation(id, 2, "normal")
    }
}
