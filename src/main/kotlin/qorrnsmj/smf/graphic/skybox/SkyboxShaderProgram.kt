package qorrnsmj.smf.graphic.skybox

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class SkyboxShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "skybox.vert"),
    Shader(GL_FRAGMENT_SHADER, "skybox.frag")
)
