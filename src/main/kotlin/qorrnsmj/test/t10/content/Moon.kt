package qorrnsmj.test.t10.content

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.test.t10.render.Renderer
import qorrnsmj.test.t10.render.Texture
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// OpenGL Sphere
// https://www.songho.ca/opengl/gl_sphere.html

// 法線を正しい向きにするときなぜ逆転置行列なのか
// https://qiita.com/ktanoooo/items/7da443e7bc38f7ff6734
class Moon(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var angle: Float = 0.0f,
    var radius: Float = 1.0f,
    var slices: Int = 16,
    var stacks: Int = 16
) {
    private var modelMatrix = Matrix4f().apply { setIdentity() }
    private val texture = Texture("../../test/test10_white.png")
    private var vertices = FloatArray(0)
    private var indices = IntArray(0)

    init {
        updateSphere()
    }

    fun updateSphere() {
        updateModelMatrix()

        vertices = FloatArray(0) // 既存の頂点データをクリア
        for (i in 0..stacks) {
            val stackAngle = i * PI / stacks - PI / 2 // スタックの角度を調整して縦方向に
            val xy = radius * cos(stackAngle)
            val y = radius * sin(stackAngle) // y座標をstackに基づいて計算

            for (j in 0..slices) {
                val sectorAngle = j * 2 * PI / slices
                val x = xy * cos(sectorAngle) + this.x // x座標を加算
                val z = xy * sin(sectorAngle) + this.z // z座標を加算

                vertices += floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())
            }
        }

        indices = IntArray(0) // インデックスも再計算
        for (i in 0 until stacks) {
            val k1 = i * (slices + 1)
            val k2 = k1 + slices + 1

            for (j in 0 until slices) {
                if (i != 0) {
                    indices += intArrayOf(k1 + j, k2 + j, k1 + j + 1)
                }
                if (i != (stacks - 1)) {
                    indices += intArrayOf(k1 + j + 1, k2 + j, k2 + j + 1)
                }
            }
        }
    }

    private fun getNormal(i1: Int, i2: Int, i3: Int): FloatArray {
        // 三角形の頂点座標を取得
        val v0 = Vector3f(vertices[i1 * 3], vertices[i1 * 3 + 1], vertices[i1 * 3 + 2])
        val v1 = Vector3f(vertices[i2 * 3], vertices[i2 * 3 + 1], vertices[i2 * 3 + 2])
        val v2 = Vector3f(vertices[i3 * 3], vertices[i3 * 3 + 1], vertices[i3 * 3 + 2])

        // 外積を使って法線を計算
        val edge1 = v1.subtract(v0) // v1 - v0
        val edge2 = v2.subtract(v0) // v2 - v0
        val normal = edge1.cross(edge2).normalize() // edge1とedge2の外積を正規化 (法線)

        return floatArrayOf(normal.x, normal.y, normal.z)
    }

    fun draw() {
        var data = floatArrayOf()
        val normalMatrix = modelMatrix.invert().transpose() // 法線の変換行列 (逆転置行列)

        // 法線を正しい向きにするときなぜ逆転置行列なのか
        // https://qiita.com/ktanoooo/items/7da443e7bc38f7ff6734
        //
        // 逆行列を取る：
        //    位置ベクトルはモデル行列をそのまま使って変換すればいいけど、
        //    法線は逆行列を取らないといけない。これで、法線の方向が適切に変換される。
        // 転置する：
        //    逆行列を取っただけだと、回転やスケーリングに対して正しく法線が変換されない
        //    場合があるから、転置することで法線の方向が修正される。

        // 各三角形ごとに処理するために step 3 で回す
        for (i in indices.indices step 3) {
            val i1 = indices[i]
            val i2 = indices[i + 1]
            val i3 = indices[i + 2]

            // 法線を計算
            val normal = getNormal(i1, i2, i3)
            val transformedNormal = normalMatrix.multiply(Vector4f(normal[0], normal[1], normal[2], 0.0f))

            // 各頂点のデータを追加
            for (index in listOf(i1, i2, i3)) {
                val vertex = Vector4f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2], 1.0f)
                val transformedVertex = modelMatrix.multiply(vertex)

                // vertex, color, uv, normal
                data += floatArrayOf(transformedVertex.x, transformedVertex.y, transformedVertex.z)
                data += floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
                data += floatArrayOf(0.0f, 0.0f)
                data += floatArrayOf(transformedNormal.x, transformedNormal.y, transformedNormal.z) // 変換後の法線
            }
        }

        Logger.debug("(${2 * slices * stacks - (slices * 2)}) ${indices.size / 3}")

        texture.bind()
        Renderer.begin()
        Renderer.draw(data)
        Renderer.end()
        texture.unbind()
    }

    fun drawWithVertexNormals() {
        var data = floatArrayOf()
        val normalMatrix = modelMatrix.invert().transpose() // 法線の変換行列 (逆転置行列)
        val vertexNormals = Array(vertices.size / 3) { Vector3f(0.0f, 0.0f, 0.0f) }

        // 各三角形ごとに法線を計算して頂点に加算
        for (i in indices.indices step 3) {
            val i1 = indices[i]
            val i2 = indices[i + 1]
            val i3 = indices[i + 2]

            // 法線を計算
            val normal = getNormal(i1, i2, i3)

            // 各頂点の法線に加算
            vertexNormals[i1] = vertexNormals[i1].add(Vector3f(normal[0], normal[1], normal[2]))
            vertexNormals[i2] = vertexNormals[i2].add(Vector3f(normal[0], normal[1], normal[2]))
            vertexNormals[i3] = vertexNormals[i3].add(Vector3f(normal[0], normal[1], normal[2]))
        }

        // 各頂点のデータを追加
        for (i in indices.indices) {
            val index = indices[i]
            val vertex = Vector4f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2], 1.0f)
            val transformedVertex = modelMatrix.multiply(vertex)
            val normal = vertexNormals[index]
            val transformedNormal = normalMatrix.multiply(Vector4f(normal.x, normal.y, normal.z, 0.0f))

            // vertex, color, uv, normal
            data += floatArrayOf(transformedVertex.x, transformedVertex.y, transformedVertex.z)
            data += floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
            data += floatArrayOf(0.0f, 0.0f)
            data += floatArrayOf(transformedNormal.x, transformedNormal.y, transformedNormal.z) // 変換後の法線
        }

        Logger.debug("triangle-polygon: ${indices.size / 3}")

        texture.bind()
        Renderer.begin()
        Renderer.draw(data)
        Renderer.end()
        texture.unbind()
    }

    private fun updateModelMatrix() {
        modelMatrix = Matrix4f(
            Vector4f(cos(angle), 0.0f, sin(angle), 0.0f),
            Vector4f(0.0f, 1.0f, 0.0f, 0.0f),
            Vector4f(-sin(angle), 0.0f, cos(angle), 0.0f),
            Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        )
    }
}
