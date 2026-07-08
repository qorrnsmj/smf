package qorrnsmj.smf.graphic.shadow

import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class ShadowShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "shadow.vert"),
    Shader(GL_FRAGMENT_SHADER, "shadow.frag")
)
