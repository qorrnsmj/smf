package qorrnsmj.smf.math

import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * This class represents a 4x4-Matrix. GLSL equivalent to mat4.
 *
 * @author Heiko Brumme
 */
class Matrix4f {
    var m00: Float = 0f
    var m01: Float = 0f
    var m02: Float = 0f
    var m03: Float = 0f
    var m10: Float = 0f
    var m11: Float = 0f
    var m12: Float = 0f
    var m13: Float = 0f
    var m20: Float = 0f
    var m21: Float = 0f
    var m22: Float = 0f
    var m23: Float = 0f
    var m30: Float = 0f
    var m31: Float = 0f
    var m32: Float = 0f
    var m33: Float = 0f

    /**
     * Creates a 4x4 identity matrix.
     */
    constructor() {
        setIdentity()
    }

    /**
     * Creates a 4x4 matrix with specified columns.
     *
     * @param col1 Vector with values of the first column
     * @param col2 Vector with values of the second column
     * @param col3 Vector with values of the third column
     * @param col4 Vector with values of the fourth column
     */
    constructor(col1: Vector4f, col2: Vector4f, col3: Vector4f, col4: Vector4f) {
        m00 = col1.x
        m10 = col1.y
        m20 = col1.z
        m30 = col1.w

        m01 = col2.x
        m11 = col2.y
        m21 = col2.z
        m31 = col2.w

        m02 = col3.x
        m12 = col3.y
        m22 = col3.z
        m32 = col3.w

        m03 = col4.x
        m13 = col4.y
        m23 = col4.z
        m33 = col4.w
    }

    /**
     * Sets this matrix to the identity matrix.
     */
    fun setIdentity() {
        m00 = 1f
        m11 = 1f
        m22 = 1f
        m33 = 1f

        m01 = 0f
        m02 = 0f
        m03 = 0f
        m10 = 0f
        m12 = 0f
        m13 = 0f
        m20 = 0f
        m21 = 0f
        m23 = 0f
        m30 = 0f
        m31 = 0f
        m32 = 0f
    }

    /**
     * Adds this matrix to another matrix.
     *
     * @param other The other matrix
     *
     * @return Sum of this + other
     */
    fun add(other: Matrix4f): Matrix4f {
        val result = Matrix4f()

        result.m00 = this.m00 + other.m00
        result.m10 = this.m10 + other.m10
        result.m20 = this.m20 + other.m20
        result.m30 = this.m30 + other.m30

        result.m01 = this.m01 + other.m01
        result.m11 = this.m11 + other.m11
        result.m21 = this.m21 + other.m21
        result.m31 = this.m31 + other.m31

        result.m02 = this.m02 + other.m02
        result.m12 = this.m12 + other.m12
        result.m22 = this.m22 + other.m22
        result.m32 = this.m32 + other.m32

        result.m03 = this.m03 + other.m03
        result.m13 = this.m13 + other.m13
        result.m23 = this.m23 + other.m23
        result.m33 = this.m33 + other.m33

        return result
    }

    /**
     * Negates this matrix.
     *
     * @return Negated matrix
     */
    fun negate(): Matrix4f {
        return multiply(-1f)
    }

    /**
     * Subtracts this matrix from another matrix.
     *
     * @param other The other matrix
     *
     * @return Difference of this - other
     */
    fun subtract(other: Matrix4f): Matrix4f {
        return this.add(other.negate())
    }

    /**
     * Multiplies this matrix with a scalar.
     *
     * @param scalar The scalar
     *
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Matrix4f {
        val result = Matrix4f()

        result.m00 = this.m00 * scalar
        result.m10 = this.m10 * scalar
        result.m20 = this.m20 * scalar
        result.m30 = this.m30 * scalar

        result.m01 = this.m01 * scalar
        result.m11 = this.m11 * scalar
        result.m21 = this.m21 * scalar
        result.m31 = this.m31 * scalar

        result.m02 = this.m02 * scalar
        result.m12 = this.m12 * scalar
        result.m22 = this.m22 * scalar
        result.m32 = this.m32 * scalar

        result.m03 = this.m03 * scalar
        result.m13 = this.m13 * scalar
        result.m23 = this.m23 * scalar
        result.m33 = this.m33 * scalar

        return result
    }

    /**
     * Multiplies this matrix to a vector.
     *
     * @param vector The vector
     *
     * @return Vector product of this * other
     */
    fun multiply(vector: Vector4f): Vector4f {
        val x = this.m00 * vector.x + this.m01 * vector.y + this.m02 * vector.z + this.m03 * vector.w
        val y = this.m10 * vector.x + this.m11 * vector.y + this.m12 * vector.z + this.m13 * vector.w
        val z = this.m20 * vector.x + this.m21 * vector.y + this.m22 * vector.z + this.m23 * vector.w
        val w = this.m30 * vector.x + this.m31 * vector.y + this.m32 * vector.z + this.m33 * vector.w
        return Vector4f(x, y, z, w)
    }

    /**
     * Multiplies this matrix to another matrix.
     *
     * @param other The other matrix
     *
     * @return Matrix product of this * other
     */
    fun multiply(other: Matrix4f): Matrix4f {
        val result = Matrix4f()

        result.m00 = this.m00 * other.m00 + this.m01 * other.m10 + this.m02 * other.m20 + this.m03 * other.m30
        result.m10 = this.m10 * other.m00 + this.m11 * other.m10 + this.m12 * other.m20 + this.m13 * other.m30
        result.m20 = this.m20 * other.m00 + this.m21 * other.m10 + this.m22 * other.m20 + this.m23 * other.m30
        result.m30 = this.m30 * other.m00 + this.m31 * other.m10 + this.m32 * other.m20 + this.m33 * other.m30

        result.m01 = this.m00 * other.m01 + this.m01 * other.m11 + this.m02 * other.m21 + this.m03 * other.m31
        result.m11 = this.m10 * other.m01 + this.m11 * other.m11 + this.m12 * other.m21 + this.m13 * other.m31
        result.m21 = this.m20 * other.m01 + this.m21 * other.m11 + this.m22 * other.m21 + this.m23 * other.m31
        result.m31 = this.m30 * other.m01 + this.m31 * other.m11 + this.m32 * other.m21 + this.m33 * other.m31

        result.m02 = this.m00 * other.m02 + this.m01 * other.m12 + this.m02 * other.m22 + this.m03 * other.m32
        result.m12 = this.m10 * other.m02 + this.m11 * other.m12 + this.m12 * other.m22 + this.m13 * other.m32
        result.m22 = this.m20 * other.m02 + this.m21 * other.m12 + this.m22 * other.m22 + this.m23 * other.m32
        result.m32 = this.m30 * other.m02 + this.m31 * other.m12 + this.m32 * other.m22 + this.m33 * other.m32

        result.m03 = this.m00 * other.m03 + this.m01 * other.m13 + this.m02 * other.m23 + this.m03 * other.m33
        result.m13 = this.m10 * other.m03 + this.m11 * other.m13 + this.m12 * other.m23 + this.m13 * other.m33
        result.m23 = this.m20 * other.m03 + this.m21 * other.m13 + this.m22 * other.m23 + this.m23 * other.m33
        result.m33 = this.m30 * other.m03 + this.m31 * other.m13 + this.m32 * other.m23 + this.m33 * other.m33

        return result
    }

    /**
     * Transposes this matrix.
     *
     * @return Transposed matrix
     */
    fun transpose(): Matrix4f {
        val result = Matrix4f()

        result.m00 = this.m00
        result.m10 = this.m01
        result.m20 = this.m02
        result.m30 = this.m03

        result.m01 = this.m10
        result.m11 = this.m11
        result.m21 = this.m12
        result.m31 = this.m13

        result.m02 = this.m20
        result.m12 = this.m21
        result.m22 = this.m22
        result.m32 = this.m23

        result.m03 = this.m30
        result.m13 = this.m31
        result.m23 = this.m32
        result.m33 = this.m33

        return result
    }

    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(m00).put(m10).put(m20).put(m30)
        buffer.put(m01).put(m11).put(m21).put(m31)
        buffer.put(m02).put(m12).put(m22).put(m32)
        buffer.put(m03).put(m13).put(m23).put(m33)
        buffer.flip()
    }

    // TODO: 消す (toBufferをMemoryStackで使う)
    fun getBuffer(): FloatBuffer {
        val buffer = BufferUtils.createFloatBuffer(4 * 4)

        buffer.put(m00).put(m10).put(m20).put(m30)
        buffer.put(m01).put(m11).put(m21).put(m31)
        buffer.put(m02).put(m12).put(m22).put(m32)
        buffer.put(m03).put(m13).put(m23).put(m33)
        buffer.flip()

        return buffer
    }

    fun invert(): Matrix4f {
        val inv = Matrix4f()
        val m = this
        val invOut = inv
        val det =
            m.m00 * m.m11 * m.m22 * m.m33 + m.m00 * m.m12 * m.m23 * m.m31 + m.m00 * m.m13 * m.m21 * m.m32 + m.m01 * m.m10 * m.m23 * m.m32 + m.m01 * m.m12 * m.m20 * m.m33 + m.m01 * m.m13 * m.m22 * m.m30 + m.m02 * m.m10 * m.m21 * m.m33 + m.m02 * m.m11 * m.m23 * m.m30 + m.m02 * m.m13 * m.m20 * m.m31 + m.m03 * m.m10 * m.m22 * m.m31 + m.m03 * m.m11 * m.m20 * m.m32 + m.m03 * m.m12 * m.m21 * m.m30 - m.m00 * m.m11 * m.m23 * m.m32 - m.m00 * m.m12 * m.m21 * m.m33 - m.m00 * m.m13 * m.m22 * m.m31 - m.m01 * m.m10 * m.m22 * m.m33 - m.m01 * m.m12 * m.m23 * m.m30 - m.m01 * m.m13 * m.m20 * m.m32 - m.m02 * m.m10 * m.m23 * m.m31 - m.m02 * m.m11 * m.m20 * m.m33 - m.m02 * m.m13 * m.m21 * m.m30 - m.m03 * m.m10 * m.m21 * m.m32 - m.m03 * m.m11 * m.m22 * m.m30 - m.m03 * m.m12 * m.m20 * m.m31
        val invDet = 1.0f / det
        invOut.m00 =
            (m.m11 * m.m22 * m.m33 + m.m12 * m.m23 * m.m31 + m.m13 * m.m21 * m.m32 - m.m11 * m.m23 * m.m32 - m.m12 * m.m21 * m.m33 - m.m13 * m.m22 * m.m31) * invDet
        invOut.m01 =
            (m.m01 * m.m23 * m.m32 + m.m02 * m.m21 * m.m33 + m.m03 * m.m22 * m.m31 - m.m01 * m.m22 * m.m33 - m.m02 * m.m23 * m.m31 - m.m03 * m.m21 * m.m32) * invDet
        invOut.m02 =
            (m.m01 * m.m12 * m.m33 + m.m02 * m.m13 * m.m31 + m.m03 * m.m11 * m.m32 - m.m01 * m.m13 * m.m32 - m.m02 * m.m11 * m.m33 - m.m03 * m.m12 * m.m31) * invDet
        invOut.m03 =
            (m.m01 * m.m13 * m.m22 + m.m02 * m.m11 * m.m23 + m.m03 * m.m12 * m.m21 - m.m01 * m.m12 * m.m23 - m.m02 * m.m13 * m.m21 - m.m03 * m.m11 * m.m22) * invDet
        invOut.m10 =
            (m.m10 * m.m23 * m.m32 + m.m12 * m.m20 * m.m33 + m.m13 * m.m22 * m.m30 - m.m10 * m.m22 * m.m33 - m.m12 * m.m23 * m.m30 - m.m13 * m.m20 * m.m32) * invDet
        invOut.m11 =
            (m.m00 * m.m22 * m.m33 + m.m02 * m.m23 * m.m30 + m.m03 * m.m20 * m.m32 - m.m00 * m.m23 * m.m32 - m.m02 * m.m20 * m.m33 - m.m03 * m.m22 * m.m30) * invDet
        invOut.m12 =
            (m.m00 * m.m13 * m.m32 + m.m02 * m.m10 * m.m33 + m.m03 * m.m12 * m.m30 - m.m00 * m.m12 * m.m33 - m.m02 * m.m13 * m.m30 - m.m03 * m.m10 * m.m32) * invDet
        invOut.m13 =
            (m.m00 * m.m12 * m.m23 + m.m02 * m.m13 * m.m20 + m.m03 * m.m10 * m.m22 - m.m00 * m.m13 * m.m22 - m.m02 * m.m10 * m.m23 - m.m03 * m.m12 * m.m20) * invDet
        invOut.m20 =
            (m.m10 * m.m21 * m.m33 + m.m11 * m.m23 * m.m30 + m.m13 * m.m20 * m.m31 - m.m10 * m.m23 * m.m31 - m.m11 * m.m20 * m.m33 - m.m13 * m.m21 * m.m30) * invDet
        invOut.m21 =
            (m.m00 * m.m23 * m.m31 + m.m01 * m.m20 * m.m33 + m.m03 * m.m21 * m.m30 - m.m00 * m.m21 * m.m33 - m.m01 * m.m23 * m.m30 - m.m03 * m.m20 * m.m31) * invDet
        invOut.m22 =
            (m.m00 * m.m11 * m.m33 + m.m01 * m.m13 * m.m30 + m.m03 * m.m10 * m.m31 - m.m00 * m.m13 * m.m31 - m.m01 * m.m10 * m.m33 - m.m03 * m.m11 * m.m30) * invDet
        invOut.m23 =
            (m.m00 * m.m13 * m.m21 + m.m01 * m.m10 * m.m23 + m.m03 * m.m11 * m.m20 - m.m00 * m.m11 * m.m23 - m.m01 * m.m13 * m.m20 - m.m03 * m.m10 * m.m21) * invDet
        invOut.m30 =
            (m.m10 * m.m22 * m.m31 + m.m11 * m.m20 * m.m32 + m.m12 * m.m21 * m.m30 - m.m10 * m.m21 * m.m32 - m.m11 * m.m22 * m.m30 - m.m12 * m.m20 * m.m31) * invDet
        invOut.m31 =
            (m.m00 * m.m21 * m.m32 + m.m01 * m.m22 * m.m30 + m.m02 * m.m20 * m.m31 - m.m00 * m.m22 * m.m31 - m.m01 * m.m20 * m.m32 - m.m02 * m.m21 * m.m30) * invDet
        invOut.m32 =
            (m.m00 * m.m12 * m.m31 + m.m01 * m.m10 * m.m32 + m.m02 * m.m11 * m.m30 - m.m00 * m.m11 * m.m32 - m.m01 * m.m12 * m.m30 - m.m02 * m.m10 * m.m31) * invDet
        invOut.m33 =
            (m.m00 * m.m11 * m.m22 + m.m01 * m.m12 * m.m20 + m.m02 * m.m10 * m.m21 - m.m00 * m.m12 * m.m21 - m.m01 * m.m10 * m.m22 - m.m02 * m.m11 * m.m20) * invDet
        return inv
    }


    // TODO: perspectiveとかの例ある
    companion object {
        /**
         * Creates an orthographic projection matrix. Similar to
         * `glOrtho(left, right, bottom, top, near, far)`.
         *
         * @param left   Coordinate for the left vertical clipping pane
         * @param right  Coordinate for the right vertical clipping pane
         * @param bottom Coordinate for the bottom horizontal clipping pane
         * @param top    Coordinate for the bottom horizontal clipping pane
         * @param near   Coordinate for the near depth clipping pane
         * @param far    Coordinate for the far depth clipping pane
         *
         * @return Orthographic matrix
         */
        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4f {
            val ortho = Matrix4f()

            val tx = -(right + left) / (right - left)
            val ty = -(top + bottom) / (top - bottom)
            val tz = -(far + near) / (far - near)

            ortho.m00 = 2f / (right - left)
            ortho.m11 = 2f / (top - bottom)
            ortho.m22 = -2f / (far - near)
            ortho.m03 = tx
            ortho.m13 = ty
            ortho.m23 = tz

            return ortho
        }

        /**
         * Creates a perspective projection matrix. Similar to
         * `glFrustum(left, right, bottom, top, near, far)`.
         *
         * @param left   Coordinate for the left vertical clipping pane
         * @param right  Coordinate for the right vertical clipping pane
         * @param bottom Coordinate for the bottom horizontal clipping pane
         * @param top    Coordinate for the bottom horizontal clipping pane
         * @param near   Coordinate for the near depth clipping pane, must be
         * positive
         * @param far    Coordinate for the far depth clipping pane, must be
         * positive
         *
         * @return Perspective matrix
         */
        fun frustum(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4f {
            val frustum = Matrix4f()

            val a = (right + left) / (right - left)
            val b = (top + bottom) / (top - bottom)
            val c = -(far + near) / (far - near)
            val d = -(2f * far * near) / (far - near)

            frustum.m00 = (2f * near) / (right - left)
            frustum.m11 = (2f * near) / (top - bottom)
            frustum.m02 = a
            frustum.m12 = b
            frustum.m22 = c
            frustum.m32 = -1f
            frustum.m23 = d
            frustum.m33 = 0f

            return frustum
        }

        /**
         * Creates a perspective projection matrix. Similar to
         * `gluPerspective(fov, aspect, zNear, zFar)`.
         *
         * @param fov   Field of view angle in degrees
         * @param aspect The aspect ratio is the ratio of width to height
         * @param near   Distance from the viewer to the near clipping plane, must
         * be positive
         * @param far    Distance from the viewer to the far clipping plane, must be
         * positive
         *
         * @return Perspective matrix
         */
        fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Matrix4f {
            val perspective = Matrix4f()

            val f = (1f / tan(Math.toRadians(fov.toDouble()) / 2f)).toFloat()

            perspective.m00 = f / aspect
            perspective.m11 = f
            perspective.m22 = (far + near) / (near - far)
            perspective.m32 = -1f
            perspective.m23 = (2f * far * near) / (near - far)
            perspective.m33 = 0f

            return perspective
        }

        /**
         * Creates a translation matrix. Similar to
         * `glTranslate(x, y, z)`.
         *
         * @param x x coordinate of translation vector
         * @param y y coordinate of translation vector
         * @param z z coordinate of translation vector
         *
         * @return Translation matrix
         */
        fun translate(x: Float, y: Float, z: Float): Matrix4f {
            val translation = Matrix4f()

            translation.m03 = x
            translation.m13 = y
            translation.m23 = z

            return translation
        }

        /**
         * Creates a rotation matrix. Similar to
         * `glRotate(angle, x, y, z)`.
         *
         * @param angle Angle of rotation in degrees
         * @param x     x coordinate of the rotation vector
         * @param y     y coordinate of the rotation vector
         * @param z     z coordinate of the rotation vector
         *
         * @return Rotation matrix
         */
        fun rotate(angle: Float, x: Float, y: Float, z: Float): Matrix4f {
            var x = x
            var y = y
            var z = z
            val rotation = Matrix4f()

            val c = cos(Math.toRadians(angle.toDouble())).toFloat()
            val s = sin(Math.toRadians(angle.toDouble())).toFloat()
            var vec = Vector3f(x, y, z)
            if (vec.length() != 1f) {
                vec = vec.normalize()
                x = vec.x
                y = vec.y
                z = vec.z
            }

            rotation.m00 = x * x * (1f - c) + c
            rotation.m10 = y * x * (1f - c) + z * s
            rotation.m20 = x * z * (1f - c) - y * s
            rotation.m01 = x * y * (1f - c) - z * s
            rotation.m11 = y * y * (1f - c) + c
            rotation.m21 = y * z * (1f - c) + x * s
            rotation.m02 = x * z * (1f - c) + y * s
            rotation.m12 = y * z * (1f - c) - x * s
            rotation.m22 = z * z * (1f - c) + c

            return rotation
        }

        /**
         * Creates a scaling matrix. Similar to `glScale(x, y, z)`.
         *
         * @param x Scale factor along the x coordinate
         * @param y Scale factor along the y coordinate
         * @param z Scale factor along the z coordinate
         *
         * @return Scaling matrix
         */
        fun scale(x: Float, y: Float, z: Float): Matrix4f {
            val scaling = Matrix4f()

            scaling.m00 = x
            scaling.m11 = y
            scaling.m22 = z

            return scaling
        }
    }
}
