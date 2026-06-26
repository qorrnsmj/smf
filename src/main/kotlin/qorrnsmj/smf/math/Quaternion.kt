package qorrnsmj.smf.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    fun normalize(): Quaternion {
        val length = sqrt((x * x + y * y + z * z + w * w).toDouble()).toFloat()
        if (length == 0f) return identity()

        return Quaternion(
            x = x / length,
            y = y / length,
            z = z / length,
            w = w / length,
        )
    }

    fun multiply(other: Quaternion): Quaternion {
        return Quaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        )
    }

    fun conjugate(): Quaternion {
        return Quaternion(-x, -y, -z, w)
    }

    fun rotate(vector: Vector3f): Vector3f {
        val q = normalize()
        val result = q.multiply(Quaternion(vector.x, vector.y, vector.z, 0f)).multiply(q.conjugate())
        return Vector3f(result.x, result.y, result.z)
    }

    fun toMatrix(): Matrix4f {
        val q = normalize()
        val xx = q.x * q.x
        val yy = q.y * q.y
        val zz = q.z * q.z
        val xy = q.x * q.y
        val xz = q.x * q.z
        val yz = q.y * q.z
        val wx = q.w * q.x
        val wy = q.w * q.y
        val wz = q.w * q.z

        return Matrix4f(
            Vector4f(1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy), 0f),
            Vector4f(2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx), 0f),
            Vector4f(2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy), 0f),
            Vector4f(0f, 0f, 0f, 1f),
        )
    }

    companion object {
        fun identity(): Quaternion = Quaternion(0f, 0f, 0f, 1f)

        fun fromEulerDegrees(euler: Vector3f): Quaternion {
            val halfX = Math.toRadians(euler.x.toDouble()).toFloat() * 0.5f
            val halfY = Math.toRadians(euler.y.toDouble()).toFloat() * 0.5f
            val halfZ = Math.toRadians(euler.z.toDouble()).toFloat() * 0.5f

            val qx = fromAxisAngleRad(Vector3f(1f, 0f, 0f), halfX)
            val qy = fromAxisAngleRad(Vector3f(0f, 1f, 0f), halfY)
            val qz = fromAxisAngleRad(Vector3f(0f, 0f, 1f), halfZ)

            return qx.multiply(qy).multiply(qz).normalize()
        }

        private fun fromAxisAngleRad(axis: Vector3f, halfAngle: Float): Quaternion {
            val s = sin(halfAngle)
            val c = cos(halfAngle)
            return Quaternion(axis.x * s, axis.y * s, axis.z * s, c).normalize()
        }
    }
}
