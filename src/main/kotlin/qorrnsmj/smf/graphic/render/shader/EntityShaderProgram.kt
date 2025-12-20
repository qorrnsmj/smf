package qorrnsmj.smf.graphic.render.shader

import org.lwjgl.opengl.GL33C.*
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class EntityShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "entity.vert"),
    Shader(GL_FRAGMENT_SHADER, "entity.frag")
)
