package qorrnsmj.smf.graphic.billboard

import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector4f

data class Billboard(
    val texture: TextureBufferObject,
    var size: Vector2f,
    var tint: Vector4f = Vector4f(1f, 1f, 1f, 1f),
    var facing: BillboardFacing = BillboardFacing.CAMERA,
    var doubleSided: Boolean = false,
    var alphaSource: BillboardAlphaSource = BillboardAlphaSource.TEXTURE_ALPHA,
    var edgeFade: Float = 0f,
)

enum class BillboardFacing {
    CAMERA,
    CAMERA_Y_AXIS,
    HORIZONTAL,
}

enum class BillboardAlphaSource {
    TEXTURE_ALPHA,
    RED,
}
