package qorrnsmj.smf.graphic.entity

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class EntityShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "entity.vert"),
    Shader(GL_FRAGMENT_SHADER, "entity.frag")
)
