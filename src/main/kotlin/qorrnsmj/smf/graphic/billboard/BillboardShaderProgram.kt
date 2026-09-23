package qorrnsmj.smf.graphic.billboard

import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class BillboardShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "billboard.vert"),
    Shader(GL_FRAGMENT_SHADER, "billboard.frag"),
)
