package qorrnsmj.test.t5

import org.lwjgl.opengl.GL11.*

class Cube(
    val x: Float,
    val y: Float,
    val z: Float
) {
    // vertex coordinates
    private val vertices = floatArrayOf(
        // front
        -1.0f + x, -1.0f + y, 1.0f + z,
        1.0f + x, -1.0f + y, 1.0f + z,
        1.0f + x, 1.0f + y, 1.0f + z,
        -1.0f + x, 1.0f + y, 1.0f + z,

        // back
        -1.0f + x, -1.0f + y, -1.0f + z,
        1.0f + x, -1.0f + y, -1.0f + z,
        1.0f + x, 1.0f + y, -1.0f + z,
        -1.0f + x, 1.0f + y, -1.0f + z
    )

    // index to draw triangles
    private val indices = intArrayOf(
        // front
        0, 1, 2, 2, 3, 0,
        // back
        4, 5, 6, 6, 7, 4,
        // top
        3, 2, 6, 6, 7, 3,
        // bottom
        4, 5, 1, 1, 0, 4,
        // left
        4, 0, 3, 3, 7, 4,
        // right
        1, 5, 6, 6, 2, 1
    )

    private val colors = arrayOf(
        floatArrayOf(1.0f, 0.0f, 0.0f), // red
        floatArrayOf(0.0f, 1.0f, 0.0f), // green
        floatArrayOf(0.0f, 0.0f, 1.0f), // blue
        floatArrayOf(1.0f, 1.0f, 0.0f), // yellow
        floatArrayOf(1.0f, 0.0f, 1.0f), // magenta
        floatArrayOf(0.0f, 1.0f, 1.0f)  // cyan
    )

    fun draw() {
        // TODO: GL_TRIANGLE_STRIPにする？
        glBegin(GL_TRIANGLES)

        // 6面それぞれの色を設定
        for (i in 0 until indices.size / 6) {
            val color = colors[i]
            glColor3f(color[0], color[1], color[2])

            // 三角形で面を描画
            for (j in 0 until 6) {
                val vertexIndex = indices[i * 6 + j]
                glVertex3f(
                    vertices[vertexIndex * 3],
                    vertices[vertexIndex * 3 + 1],
                    vertices[vertexIndex * 3 + 2]
                )
            }
        }

        glEnd()
    }
}
