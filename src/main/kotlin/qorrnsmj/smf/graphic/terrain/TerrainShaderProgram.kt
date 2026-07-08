package qorrnsmj.smf.graphic.terrain

import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import qorrnsmj.smf.graphic.resource.shader.Shader
import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class TerrainShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "terrain.vert"),
    Shader(GL_FRAGMENT_SHADER, "terrain.frag")
)
