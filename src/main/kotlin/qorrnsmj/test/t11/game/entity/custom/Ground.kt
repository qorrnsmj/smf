package qorrnsmj.test.t11.game.entity.custom

import qorrnsmj.test.t11.core.model.Texture

class Ground(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f,
    val scale: Float = 1.0f
) {
    private val texture = Texture("../../test/test10_white.png")
    private var data = floatArrayOf()
    private val vertices = floatArrayOf(
        -scale + x, y, scale + z,
        scale + x, y, scale + z,
        -scale + x, y, -scale + z,
        -scale + x, y, -scale + z,
        scale + x, y, scale + z,
        scale + x, y, -scale + z
    )
    //private val color = floatArrayOf(0.4f, 0.8f, 0.9f, 1.0f)
    private val color = floatArrayOf(0.678f, 1.0f, 0.184f, 1.0f)
    private val uv = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )
    private val normal = floatArrayOf(0.0f, 1.0f, 0.0f)

    init {
        for (i in 0 until vertices.size / 3) {
            data += vertices[i * 3]
            data += vertices[i * 3 + 1]
            data += vertices[i * 3 + 2]
            data += color
            data += uv[i * 2]
            data += uv[i * 2 + 1]
            data += normal
        }
    }
}
