package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.math.Matrix4f
import kotlin.math.tan

object Projection {
    private const val FOV = 45f
    private const val Z_NEAR = 0.1f
    private const val Z_FAR = 100f

    fun getOrthographicMatrix(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4f {
        return Matrix4f().apply {
            m00 = 2f / (right - left)
            m11 = 2f / (top - bottom)
            m22 = -2f / (far - near)
            m30 = -(right + left) / (right - left)
            m31 = -(top + bottom) / (top - bottom)
            m32 = -(far + near) / (far - near)
        }
    }

    fun getPerspectiveMatrix(aspect: Float, fov: Float = FOV, zNear: Float = Z_NEAR, zFar: Float = Z_FAR): Matrix4f {
        val rad = fov * (Math.PI / 180.0).toFloat()
        val yScale = 1f / tan(rad / 2f) // 半視野角のタンジェント
        val xScale = yScale / aspect
        val frustumLength = zFar - zNear

        return Matrix4f().apply {
            m00 = xScale
            m11 = yScale
            m22 = -((zFar + zNear) / frustumLength)
            m23 = -((2 * zNear * zFar) / frustumLength)
            m32 = -1f
            m33 = 0f
        }
    }
}
