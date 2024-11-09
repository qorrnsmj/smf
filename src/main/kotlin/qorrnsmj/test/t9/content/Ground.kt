package qorrnsmj.test.t9.content

import qorrnsmj.test.t9.render.Renderer
import qorrnsmj.test.t9.render.Texture

class Ground(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var scale: Float = 1.0f
) {
    private val texture = Texture("../../test/test8_stone.png")

    fun draw() {
        var data = floatArrayOf()

        data += floatArrayOf(-scale + x, y, scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f)
        data += floatArrayOf(scale + x, y, scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)
        data += floatArrayOf(-scale + x, y, -scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f)

        data += floatArrayOf(-scale + x, y, -scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f)
        data += floatArrayOf(+scale + x, y, +scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)
        data += floatArrayOf(scale + x, y, -scale + z, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

        texture.bind()
        Renderer.begin()
        Renderer.draw(data)
        Renderer.end()
        texture.unbind()
    }
}
