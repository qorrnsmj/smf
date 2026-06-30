package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.GL_BLEND
import org.lwjgl.opengl.GL33C.GL_CULL_FACE
import org.lwjgl.opengl.GL33C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL33C.GL_FALSE
import org.lwjgl.opengl.GL33C.GL_FLOAT
import org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL33C.GL_LINK_STATUS
import org.lwjgl.opengl.GL33C.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL33C.GL_SRC_ALPHA
import org.lwjgl.opengl.GL33C.GL_TRIANGLES
import org.lwjgl.opengl.GL33C.GL_TRUE
import org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL33C.glAttachShader
import org.lwjgl.opengl.GL33C.glBindVertexArray
import org.lwjgl.opengl.GL33C.glBlendFunc
import org.lwjgl.opengl.GL33C.glCompileShader
import org.lwjgl.opengl.GL33C.glCreateProgram
import org.lwjgl.opengl.GL33C.glCreateShader
import org.lwjgl.opengl.GL33C.glDisable
import org.lwjgl.opengl.GL33C.glDrawArrays
import org.lwjgl.opengl.GL33C.glEnable
import org.lwjgl.opengl.GL33C.glGenVertexArrays
import org.lwjgl.opengl.GL33C.glGetProgramInfoLog
import org.lwjgl.opengl.GL33C.glGetProgrami
import org.lwjgl.opengl.GL33C.glGetShaderInfoLog
import org.lwjgl.opengl.GL33C.glGetShaderi
import org.lwjgl.opengl.GL33C.glGetUniformLocation
import org.lwjgl.opengl.GL33C.glIsEnabled
import org.lwjgl.opengl.GL33C.glLinkProgram
import org.lwjgl.opengl.GL33C.glShaderSource
import org.lwjgl.opengl.GL33C.glUniform1f
import org.lwjgl.opengl.GL33C.glUniform3f
import org.lwjgl.opengl.GL33C.glUseProgram
import qorrnsmj.smf.graphic.Scene

class CinematicOverlayRenderer : SceneRenderer {
    private val program = createProgram()
    private val vao = glGenVertexArrays()
    private val fadeAlphaLocation = glGetUniformLocation(program, "u_fadeAlpha")
    private val fadeColorLocation = glGetUniformLocation(program, "u_fadeColor")
    private val letterboxRatioLocation = glGetUniformLocation(program, "u_letterboxRatio")

    override fun render(scene: Scene) {
        val overlay = scene.cinematicOverlay
        if (overlay.fadeAlpha <= 0f && overlay.letterboxRatio <= 0f) return

        val depthWasEnabled = glIsEnabled(GL_DEPTH_TEST)
        val cullWasEnabled = glIsEnabled(GL_CULL_FACE)
        val blendWasEnabled = glIsEnabled(GL_BLEND)

        glDisable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glUseProgram(program)
        glUniform1f(fadeAlphaLocation, overlay.fadeAlpha.coerceIn(0f, 1f))
        glUniform3f(fadeColorLocation, overlay.fadeColor.x, overlay.fadeColor.y, overlay.fadeColor.z)
        glUniform1f(letterboxRatioLocation, overlay.letterboxRatio.coerceIn(0f, 0.45f))
        glBindVertexArray(vao)
        glDrawArrays(GL_TRIANGLES, 0, 3)
        glBindVertexArray(0)
        glUseProgram(0)

        if (depthWasEnabled) glEnable(GL_DEPTH_TEST)
        if (cullWasEnabled) glEnable(GL_CULL_FACE)
        if (!blendWasEnabled) glDisable(GL_BLEND)
    }

    private fun createProgram(): Int {
        val vertexShader = compileShader(
            GL_VERTEX_SHADER,
            """
            #version 330 core
            out vec2 v_uv;
            void main() {
                vec2 positions[3] = vec2[](
                    vec2(-1.0, -1.0),
                    vec2( 3.0, -1.0),
                    vec2(-1.0,  3.0)
                );
                vec2 position = positions[gl_VertexID];
                v_uv = position * 0.5 + 0.5;
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """.trimIndent(),
        )
        val fragmentShader = compileShader(
            GL_FRAGMENT_SHADER,
            """
            #version 330 core
            in vec2 v_uv;
            out vec4 fragColor;
            uniform float u_fadeAlpha;
            uniform vec3 u_fadeColor;
            uniform float u_letterboxRatio;
            void main() {
                bool isBar = v_uv.y < u_letterboxRatio || v_uv.y > 1.0 - u_letterboxRatio;
                if (isBar) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                } else {
                    fragColor = vec4(u_fadeColor, u_fadeAlpha);
                }
            }
            """.trimIndent(),
        )

        val result = glCreateProgram()
        glAttachShader(result, vertexShader)
        glAttachShader(result, fragmentShader)
        glLinkProgram(result)
        check(glGetProgrami(result, GL_LINK_STATUS) == GL_TRUE) {
            glGetProgramInfoLog(result)
        }
        return result
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        check(glGetShaderi(shader, org.lwjgl.opengl.GL33C.GL_COMPILE_STATUS) == GL_TRUE) {
            glGetShaderInfoLog(shader)
        }
        return shader
    }
}
