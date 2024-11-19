package qorrnsmj.test.t6

import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL33.*
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import kotlin.math.cos
import kotlin.math.sin

// rotateするならshaderに渡す前にクラス内で回転させとくべき？
class Cube(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var angle: Float = 0.0f,
    var scale: Float = 1.0f
) {
//    private val vao = VertexArrayObject()
//    private val vbo = VertexBufferObject()
//    private val program = ShaderProgram().apply {
//        attachShader(Shader(GL_VERTEX_SHADER, "../../test/test6_2.vert"))
//        attachShader(Shader(GL_FRAGMENT_SHADER, "../../test/test6_2.frag"))
//        link()
//    }

    fun draw() {
        setShader()
        setUniforms()

        // Uniform変数(MVP)は使わないで、ここで回転とかさせたほうがいい？
        val vertices = arrayOf(
            // front
            // 0 1
            // 2 3
            floatArrayOf(-scale + x, scale + y, scale + z),
            floatArrayOf(scale + x, scale + y, scale + z),
            floatArrayOf(-scale + x, -scale + y, scale + z),
            floatArrayOf(scale + x, -scale + y, scale + z),

            // back (裏からみたとき)
            // 4 5
            // 6 7
            floatArrayOf(scale + x, scale + y, -scale + z),
            floatArrayOf(-scale + x, scale + y, -scale + z),
            floatArrayOf(scale + x, -scale + y, -scale + z),
            floatArrayOf(-scale + x, -scale + y, -scale + z)
        )

        var data = floatArrayOf()
        for (i in 0 until 6) { // 6面
            for (j in 0 until 4) { // 4頂点
                val index = indices[i][j]
                data += vertices[index] + colors[i]
            }
        }
//        vbo.uploadData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4 * 6) // 4頂点 * 6面

        unsetShader()
    }

    private fun setShader() {
//        vao.bind()
//        vbo.bind(GL_ARRAY_BUFFER)
//        program.use()

        // 頂点 (location = 0) aPos
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        // 色 (location = 1) aColor
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)
    }

    private fun unsetShader() {
//        program.unuse()
    }

    private fun setUniforms() {
//        program.setUniform("uModel",
//            Matrix4f( // z軸回転の回転行列
//                Vector4f(cos(angle), -sin(angle), 0.0f, 0.0f),
//                Vector4f(sin(angle),  cos(angle), 0.0f, 0.0f),
//                Vector4f(0.0f, 0.0f, 1.0f, 0.0f),
//                Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//            ).multiply(
//                Matrix4f( // x軸回転の回転行列
//                    Vector4f(1.0f, 0.0f, 0.0f, 0.0f),
//                    Vector4f(0.0f, cos(angle), -sin(angle), 0.0f),
//                    Vector4f(0.0f, sin(angle),  cos(angle), 0.0f),
//                    Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//                )
//            ).multiply(
//                Matrix4f( // y軸回転の回転行列
//                    Vector4f( cos(angle), 0.0f, sin(angle), 0.0f),
//                    Vector4f(0.0f, 1.0f, 0.0f, 0.0f),
//                    Vector4f(-sin(angle), 0.0f, cos(angle), 0.0f),
//                    Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
//                )
//            )
//        )

//        program.setUniform("uView",
//            ViewMatrix.getMatrix( // OpenGLは右手系座標
//                Vector3f(0f, 0f, 7f), // Camera position
//                Vector3f(0f, 0f, 0f), // Look at point
//                Vector3f(0f, 1f, 0f)  // Up vector
//            )
//        )
//
//        program.setUniform("uProj", // 透視投影
//            ProjectionMatrix.getPerspectiveMatrix(1600f / 1600f)
//        )
    }
    
    companion object {
        // 順番変えたらtri-stripだからか描画がおかしくなる
        // -> 普通の三角形で描画すべき？
        private val indices = arrayOf(
            intArrayOf(2, 3, 0, 1), // front
            intArrayOf(3, 6, 1, 4), // right
            intArrayOf(6, 7, 4, 5), // back
            intArrayOf(7, 2, 5, 0), // left
            intArrayOf(7, 6, 2, 3), // bottom
            intArrayOf(0, 1, 5, 4)  // top
        )
        private val colors = arrayOf(
            floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), // front (red)
            floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f), // right (cyan)
            floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f), // back (green)
            floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f), // left (magenta)
            floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f), // bottom (yellow)
            floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)  // top (blue)
        )
    }
}
