package qorrnsmj.smf.graphic

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object MVP {
    /* Model */

    fun getModelMatrix(position: Vector3f, rotation: Vector3f, scale: Vector3f): Matrix4f {
        return Matrix4f()
            .multiply(getTranslationMatrix(position))
            .multiply(getRotationMatrix(rotation))
            .multiply(getScaleMatrix(scale))
    }

    fun getTranslationMatrix(position: Vector3f): Matrix4f {
        return Matrix4f(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(position.x, position.y, position.z, 1f)
        )
    }

    fun getRotationMatrix(rotation: Vector3f): Matrix4f {
        val radX = toRadians(rotation.x.toDouble()).toFloat()
        val radY = toRadians(rotation.y.toDouble()).toFloat()
        val radZ = toRadians(rotation.z.toDouble()).toFloat()

        val xRotation = Matrix4f(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, cos(radX), -sin(radX), 0f),
            Vector4f(0f, sin(radX), cos(radX), 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        val yRotation = Matrix4f(
            Vector4f(cos(radY), 0f, sin(radY), 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(-sin(radY), 0f, cos(radY), 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        val zRotation = Matrix4f(
            Vector4f(cos(radZ), -sin(radZ), 0f, 0f),
            Vector4f(sin(radZ), cos(radZ), 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        return xRotation.multiply(yRotation).multiply(zRotation)
    }

    fun getScaleMatrix(scale: Vector3f): Matrix4f {
        return Matrix4f(
            Vector4f(scale.x, 0f, 0f, 0f),
            Vector4f(0f, scale.y, 0f, 0f),
            Vector4f(0f, 0f, scale.z, 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )
    }

    /* View */

    // TODO:
    //  - もうちょっと綺麗に
    //  - カメラも渡せるように
    fun getViewMatrix(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        val forward = center.subtract(eye).normalize()
        var upside = up.normalize()
        val side = forward.cross(upside).normalize()
        upside = side.cross(forward)

        val viewMatrix = Matrix4f(
            Vector4f(side.x, upside.x, -forward.x, 0f),
            Vector4f(side.y, upside.y, -forward.y, 0f),
            Vector4f(side.z, upside.z, -forward.z, 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        return viewMatrix.multiply(
            Matrix4f(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(-eye.x, -eye.y, -eye.z, 1f)
            )
        )
    }

    /* Projection */

    fun getOrthographicMatrix(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4f {
        return Matrix4f(
            Vector4f(2f / (right - left), 0f, 0f, 0f),
            Vector4f(0f, 2f / (top - bottom), 0f, 0f),
            Vector4f(0f, 0f, -2f / (far - near), 0f),
            Vector4f(-(right + left) / (right - left), -(top + bottom) / (top - bottom), -(far + near) / (far - near), 1f)
        )
    }

    fun getPerspectiveMatrix(aspect: Float, fov: Float = 90f, zNear: Float = 0.1f, zFar: Float = 100f): Matrix4f {
        val rad = fov * (PI / 180f)
        val yScale = 1f / tan(rad / 2f)
        val xScale = yScale / aspect
        val frustumLength = zFar - zNear

        return Matrix4f(
            Vector4f(xScale.toFloat(), 0f, 0f, 0f),
            Vector4f(0f, yScale.toFloat(), 0f, 0f),
            Vector4f(0f, 0f, -((zFar + zNear) / frustumLength), -1f),
            Vector4f(0f, 0f, -((2f * zNear * zFar) / frustumLength), 0f)
        )
    }
}