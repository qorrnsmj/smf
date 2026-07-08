package qorrnsmj.smf.graphic.effect.shader

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class ContrastShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/contrast.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/contrast.frag")
)
