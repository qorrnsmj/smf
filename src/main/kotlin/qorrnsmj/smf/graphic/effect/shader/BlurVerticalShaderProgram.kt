package qorrnsmj.smf.graphic.effect.shader

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class BlurVerticalShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/blur_vertical.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/blur_vertical.frag")
)
