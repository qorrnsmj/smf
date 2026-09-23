package qorrnsmj.smf.graphic.skydome

import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class SkydomeShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "skydome.vert"),
    Shader(GL_FRAGMENT_SHADER, "skydome.frag"),
)
