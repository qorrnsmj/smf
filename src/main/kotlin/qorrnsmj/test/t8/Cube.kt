package qorrnsmj.test.t8

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector4f
import kotlin.math.cos
import kotlin.math.sin

class Cube(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var scale: Float = 1.0f,
    var angle: Float = 0.0f
) {
    private val vertices = arrayOf(
        // front
        floatArrayOf(-scale + x, scale + y, scale + z),
        floatArrayOf(scale + x, scale + y, scale + z),
        floatArrayOf(-scale + x, -scale + y, scale + z),
        floatArrayOf(scale + x, -scale + y, scale + z),

        // back (裏からみたとき)
        floatArrayOf(scale + x, scale + y, -scale + z),
        floatArrayOf(-scale + x, scale + y, -scale + z),
        floatArrayOf(scale + x, -scale + y, -scale + z),
        floatArrayOf(-scale + x, -scale + y, -scale + z)
    )

    fun draw() {
        var data = floatArrayOf()

        for (i in 0 until 6 * 2) { // 三角形2つの1面が6つ
            for (j in 0 until 3) { // 三角形は3頂点
                val index = indices[i][j]
                data += vertices[index] + colors[i / 2]
            }
        }

        Renderer.draw(data)
    }

    fun updateAngle(angle: Float) {
        this.angle = angle

        val model = Matrix4f( // z軸回転の回転行列
            Vector4f(cos(angle), -sin(angle), 0.0f, 0.0f),
            Vector4f(sin(angle),  cos(angle), 0.0f, 0.0f),
            Vector4f(0.0f, 0.0f, 1.0f, 0.0f),
            Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        ).multiply(
            Matrix4f( // x軸回転の回転行列
                Vector4f(1.0f, 0.0f, 0.0f, 0.0f),
                Vector4f(0.0f, cos(angle), -sin(angle), 0.0f),
                Vector4f(0.0f, sin(angle),  cos(angle), 0.0f),
                Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
            )
        )

        Renderer.setModel(model)
    }

    companion object {
        private val indices = arrayOf(
            intArrayOf(2, 3, 0), intArrayOf(0, 3, 1), // front
            intArrayOf(3, 6, 1), intArrayOf(1, 6, 4), // right
            intArrayOf(6, 7, 4), intArrayOf(4, 7, 5), // back
            intArrayOf(7, 2, 5), intArrayOf(5, 2, 0), // left
            intArrayOf(7, 6, 2), intArrayOf(2, 6, 3), // bottom
            intArrayOf(0, 1, 5), intArrayOf(5, 1, 4)  // top
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
