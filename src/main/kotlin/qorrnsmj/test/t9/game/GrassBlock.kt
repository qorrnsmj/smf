package qorrnsmj.test.t9.game

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.test.t9.render.Renderer
import qorrnsmj.test.t9.render.Texture
import kotlin.math.cos
import kotlin.math.sin

class GrassBlock(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var scale: Float = 1.0f,
    var angle: Float = 0.0f
) {
    private var modelMatrix = Matrix4f().apply { setIdentity() }
    private val texture = Texture("../../test/test8_grass.png")
    // TODO: scale, rotate translate それぞれ行列の変数にまとめる
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
                val transformed = modelMatrix
                    .multiply(Vector4f(vertices[index][0], vertices[index][1], vertices[index][2], 1.0f))

                data += floatArrayOf(transformed.x, transformed.y, transformed.z)
                data += colors[i / 2]
                data += uv[i * 3 + j]
            }
        }

        // TODO: テクスチャが違うと、同じbegin, endの間に描画できない
        //  -> アトラス使う？
        texture.bind()
        Renderer.begin()
        Renderer.draw(data)
        Renderer.end()
        texture.unbind()
    }

    // TODO: scale, rotate translate それぞれ行列の変数にまとめる
    fun updateAngle(angle: Float) {
        this.angle = angle

        modelMatrix = Matrix4f( // z軸回転の回転行列
            Vector4f(cos(angle), -sin(angle), 0.0f, 0.0f),
            Vector4f(sin(angle),  cos(angle), 0.0f, 0.0f),
            Vector4f(0.0f, 0.0f, 1.0f, 0.0f),
            Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        ).multiply(Matrix4f( // x軸回転の回転行列
            Vector4f(1.0f, 0.0f, 0.0f, 0.0f),
            Vector4f(0.0f, cos(angle), -sin(angle), 0.0f),
            Vector4f(0.0f, sin(angle),  cos(angle), 0.0f),
            Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        ))
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
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // front
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // right
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // back
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // left
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // bottom
            floatArrayOf(0.7f, 1.0f, 0.3f, 1.0f)  // top (黄緑)
        )

        private val uv = arrayOf(
            // front
            floatArrayOf(0.5f, 0.5f), floatArrayOf(1.0f, 0.5f), floatArrayOf(0.5f, 1.0f),
            floatArrayOf(0.5f, 1.0f), floatArrayOf(1.0f, 0.5f), floatArrayOf(1.0f, 1.0f),

            // right
            floatArrayOf(0.5f, 0.5f), floatArrayOf(1.0f, 0.5f), floatArrayOf(0.5f, 1.0f),
            floatArrayOf(0.5f, 1.0f), floatArrayOf(1.0f, 0.5f), floatArrayOf(1.0f, 1.0f),

            // back
            floatArrayOf(0.5f, 0.5f), floatArrayOf(1.0f, 0.5f), floatArrayOf(0.5f, 1.0f),
            floatArrayOf(0.5f, 1.0f), floatArrayOf(1.0f, 0.5f), floatArrayOf(1.0f, 1.0f),

            // left
            floatArrayOf(0.5f, 0.5f), floatArrayOf(1.0f, 0.5f), floatArrayOf(0.5f, 1.0f),
            floatArrayOf(0.5f, 1.0f), floatArrayOf(1.0f, 0.5f), floatArrayOf(1.0f, 1.0f),

            // bottom
            floatArrayOf(0.0f, 0.0f), floatArrayOf(0.5f, 0.0f), floatArrayOf(0.0f, 0.5f),
            floatArrayOf(0.0f, 0.5f), floatArrayOf(0.5f, 0.0f), floatArrayOf(0.5f, 0.5f),

            // top
            floatArrayOf(0.0f, 0.5f), floatArrayOf(0.5f, 0.5f), floatArrayOf(0.0f, 1.0f),
            floatArrayOf(0.0f, 1.0f), floatArrayOf(0.5f, 0.5f), floatArrayOf(0.5f, 1.0f)
        )
    }
}
