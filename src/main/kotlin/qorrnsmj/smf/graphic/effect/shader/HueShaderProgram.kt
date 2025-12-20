package qorrnsmj.smf.graphic.effect.shader

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class HueShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "effect/hue.vert"),
    Shader(GL_FRAGMENT_SHADER, "effect/hue.frag")
)
