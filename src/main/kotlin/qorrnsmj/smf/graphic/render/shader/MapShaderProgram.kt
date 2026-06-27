package qorrnsmj.smf.graphic.render.shader

import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import qorrnsmj.smf.graphic.`object`.Shader
import qorrnsmj.smf.graphic.`object`.ShaderProgram

class MapShaderProgram : ShaderProgram(
    Shader(GL_VERTEX_SHADER, "map.vert"),
    Shader(GL_FRAGMENT_SHADER, "map.frag")
)
