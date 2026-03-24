package qorrnsmj.smf.math

import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Immutable 4x4 matrix for 3D graphics transformations. GLSL equivalent to mat4.
 * All operations return new instances, ensuring immutability.
 *
 * @property m00-m33 Matrix elements in column-major order (OpenGL compatible)
 */
data class Matrix4(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f, val m03: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f, val m13: Float = 0f,
    val m20: Float = 0f, val m21: Float = 0f, val m22: Float = 1f, val m23: Float = 0f,
    val m30: Float = 0f, val m31: Float = 0f, val m32: Float = 0f, val m33: Float = 1f
) {
    /**
     * Creates a 4x4 matrix with specified columns.
     *
     * @param col1 Vector with values of the first column
     * @param col2 Vector with values of the second column
     * @param col3 Vector with values of the third column
     * @param col4 Vector with values of the fourth column
     */
    constructor(col1: Vector4f, col2: Vector4f, col3: Vector4f, col4: Vector4f) : this(
        m00 = col1.x, m10 = col1.y, m20 = col1.z, m30 = col1.w,
        m01 = col2.x, m11 = col2.y, m21 = col2.z, m31 = col2.w,
        m02 = col3.x, m12 = col3.y, m22 = col3.z, m32 = col3.w,
        m03 = col4.x, m13 = col4.y, m23 = col4.z, m33 = col4.w
    )

    // Basic matrix operations

    /**
     * Adds this matrix to another matrix.
     *
     * @param other The other matrix
     * @return Sum of this + other
     */
    fun add(other: Matrix4): Matrix4 = Matrix4(
        m00 = this.m00 + other.m00, m01 = this.m01 + other.m01, m02 = this.m02 + other.m02, m03 = this.m03 + other.m03,
        m10 = this.m10 + other.m10, m11 = this.m11 + other.m11, m12 = this.m12 + other.m12, m13 = this.m13 + other.m13,
        m20 = this.m20 + other.m20, m21 = this.m21 + other.m21, m22 = this.m22 + other.m22, m23 = this.m23 + other.m23,
        m30 = this.m30 + other.m30, m31 = this.m31 + other.m31, m32 = this.m32 + other.m32, m33 = this.m33 + other.m33
    )

    /**
     * Negates this matrix.
     *
     * @return Negated matrix
     */
    fun negate(): Matrix4 = this * -1f

    /**
     * Subtracts another matrix from this matrix.
     *
     * @param other The other matrix
     * @return Difference of this - other
     */
    fun subtract(other: Matrix4): Matrix4 = this + other.negate()

    /**
     * Multiplies this matrix with a scalar.
     *
     * @param scalar The scalar
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Matrix4 = Matrix4(
        m00 = this.m00 * scalar, m01 = this.m01 * scalar, m02 = this.m02 * scalar, m03 = this.m03 * scalar,
        m10 = this.m10 * scalar, m11 = this.m11 * scalar, m12 = this.m12 * scalar, m13 = this.m13 * scalar,
        m20 = this.m20 * scalar, m21 = this.m21 * scalar, m22 = this.m22 * scalar, m23 = this.m23 * scalar,
        m30 = this.m30 * scalar, m31 = this.m31 * scalar, m32 = this.m32 * scalar, m33 = this.m33 * scalar
    )

    /**
     * Multiplies this matrix to a vector.
     *
     * @param vector The vector
     * @return Vector product of this * vector
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
     * @return Matrix product of this * other
     */
    fun multiply(other: Matrix4): Matrix4 = Matrix4(
        m00 = this.m00 * other.m00 + this.m01 * other.m10 + this.m02 * other.m20 + this.m03 * other.m30,
        m10 = this.m10 * other.m00 + this.m11 * other.m10 + this.m12 * other.m20 + this.m13 * other.m30,
        m20 = this.m20 * other.m00 + this.m21 * other.m10 + this.m22 * other.m20 + this.m23 * other.m30,
        m30 = this.m30 * other.m00 + this.m31 * other.m10 + this.m32 * other.m20 + this.m33 * other.m30,
        
        m01 = this.m00 * other.m01 + this.m01 * other.m11 + this.m02 * other.m21 + this.m03 * other.m31,
        m11 = this.m10 * other.m01 + this.m11 * other.m11 + this.m12 * other.m21 + this.m13 * other.m31,
        m21 = this.m20 * other.m01 + this.m21 * other.m11 + this.m22 * other.m21 + this.m23 * other.m31,
        m31 = this.m30 * other.m01 + this.m31 * other.m11 + this.m32 * other.m21 + this.m33 * other.m31,
        
        m02 = this.m00 * other.m02 + this.m01 * other.m12 + this.m02 * other.m22 + this.m03 * other.m32,
        m12 = this.m10 * other.m02 + this.m11 * other.m12 + this.m12 * other.m22 + this.m13 * other.m32,
        m22 = this.m20 * other.m02 + this.m21 * other.m12 + this.m22 * other.m22 + this.m23 * other.m32,
        m32 = this.m30 * other.m02 + this.m31 * other.m12 + this.m32 * other.m22 + this.m33 * other.m32,
        
        m03 = this.m00 * other.m03 + this.m01 * other.m13 + this.m02 * other.m23 + this.m03 * other.m33,
        m13 = this.m10 * other.m03 + this.m11 * other.m13 + this.m12 * other.m23 + this.m13 * other.m33,
        m23 = this.m20 * other.m03 + this.m21 * other.m13 + this.m22 * other.m23 + this.m23 * other.m33,
        m33 = this.m30 * other.m03 + this.m31 * other.m13 + this.m32 * other.m23 + this.m33 * other.m33
    )

    /**
     * Transposes this matrix.
     *
     * @return Transposed matrix
     */
    fun transpose(): Matrix4 = Matrix4(
        m00 = this.m00, m01 = this.m10, m02 = this.m20, m03 = this.m30,
        m10 = this.m01, m11 = this.m11, m12 = this.m21, m13 = this.m31,
        m20 = this.m02, m21 = this.m12, m22 = this.m22, m23 = this.m32,
        m30 = this.m03, m31 = this.m13, m32 = this.m23, m33 = this.m33
    )

    /**
     * Inverts this matrix.
     *
     * @return Inverted matrix
     * @throws IllegalStateException if matrix is not invertible
     */
    fun invert(): Matrix4 {
        val det = 
            m00 * m11 * m22 * m33 + m00 * m12 * m23 * m31 + m00 * m13 * m21 * m32 +
            m01 * m10 * m23 * m32 + m01 * m12 * m20 * m33 + m01 * m13 * m22 * m30 +
            m02 * m10 * m21 * m33 + m02 * m11 * m23 * m30 + m02 * m13 * m20 * m31 +
            m03 * m10 * m22 * m31 + m03 * m11 * m20 * m32 + m03 * m12 * m21 * m30 -
            m00 * m11 * m23 * m32 - m00 * m12 * m21 * m33 - m00 * m13 * m22 * m31 -
            m01 * m10 * m22 * m33 - m01 * m12 * m23 * m30 - m01 * m13 * m20 * m32 -
            m02 * m10 * m23 * m31 - m02 * m11 * m20 * m33 - m02 * m13 * m21 * m30 -
            m03 * m10 * m21 * m32 - m03 * m11 * m22 * m30 - m03 * m12 * m20 * m31
        
        if (det == 0f) throw IllegalStateException("Matrix is not invertible (determinant is 0)")
        
        val invDet = 1f / det
        
        return Matrix4(
            m00 = (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32 - m11 * m23 * m32 - m12 * m21 * m33 - m13 * m22 * m31) * invDet,
            m01 = (m01 * m23 * m32 + m02 * m21 * m33 + m03 * m22 * m31 - m01 * m22 * m33 - m02 * m23 * m31 - m03 * m21 * m32) * invDet,
            m02 = (m01 * m12 * m33 + m02 * m13 * m31 + m03 * m11 * m32 - m01 * m13 * m32 - m02 * m11 * m33 - m03 * m12 * m31) * invDet,
            m03 = (m01 * m13 * m22 + m02 * m11 * m23 + m03 * m12 * m21 - m01 * m12 * m23 - m02 * m13 * m21 - m03 * m11 * m22) * invDet,
            
            m10 = (m10 * m23 * m32 + m12 * m20 * m33 + m13 * m22 * m30 - m10 * m22 * m33 - m12 * m23 * m30 - m13 * m20 * m32) * invDet,
            m11 = (m00 * m22 * m33 + m02 * m23 * m30 + m03 * m20 * m32 - m00 * m23 * m32 - m02 * m20 * m33 - m03 * m22 * m30) * invDet,
            m12 = (m00 * m13 * m32 + m02 * m10 * m33 + m03 * m12 * m30 - m00 * m12 * m33 - m02 * m13 * m30 - m03 * m10 * m32) * invDet,
            m13 = (m00 * m12 * m23 + m02 * m13 * m20 + m03 * m10 * m22 - m00 * m13 * m22 - m02 * m10 * m23 - m03 * m12 * m20) * invDet,
            
            m20 = (m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31 - m10 * m23 * m31 - m11 * m20 * m33 - m13 * m21 * m30) * invDet,
            m21 = (m00 * m23 * m31 + m01 * m20 * m33 + m03 * m21 * m30 - m00 * m21 * m33 - m01 * m23 * m30 - m03 * m20 * m31) * invDet,
            m22 = (m00 * m11 * m33 + m01 * m13 * m30 + m03 * m10 * m31 - m00 * m13 * m31 - m01 * m10 * m33 - m03 * m11 * m30) * invDet,
            m23 = (m00 * m13 * m21 + m01 * m10 * m23 + m03 * m11 * m20 - m00 * m11 * m23 - m01 * m13 * m20 - m03 * m10 * m21) * invDet,
            
            m30 = (m10 * m22 * m31 + m11 * m20 * m32 + m12 * m21 * m30 - m10 * m21 * m32 - m11 * m22 * m30 - m12 * m20 * m31) * invDet,
            m31 = (m00 * m21 * m32 + m01 * m22 * m30 + m02 * m20 * m31 - m00 * m22 * m31 - m01 * m20 * m32 - m02 * m21 * m30) * invDet,
            m32 = (m00 * m12 * m31 + m01 * m10 * m32 + m02 * m11 * m30 - m00 * m11 * m32 - m01 * m12 * m30 - m02 * m10 * m31) * invDet,
            m33 = (m00 * m11 * m22 + m01 * m12 * m20 + m02 * m10 * m21 - m00 * m12 * m21 - m01 * m10 * m22 - m02 * m11 * m20) * invDet
        )
    }

    // Operator overloading

    /**
     * Matrix addition operator.
     */
    operator fun plus(other: Matrix4): Matrix4 = add(other)

    /**
     * Matrix subtraction operator.
     */
    operator fun minus(other: Matrix4): Matrix4 = subtract(other)

    /**
     * Matrix multiplication operators.
     */
    operator fun times(scalar: Float): Matrix4 = multiply(scalar)
    operator fun times(vector: Vector4f): Vector4f = multiply(vector)
    operator fun times(other: Matrix4): Matrix4 = multiply(other)

    /**
     * Unary minus operator.
     */
    operator fun unaryMinus(): Matrix4 = negate()

    // OpenGL integration

    /**
     * Writes matrix data to a FloatBuffer in column-major order for OpenGL.
     * Buffer position is reset to 0 after writing (flipped).
     *
     * @param buffer The buffer to write to
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(m00).put(m10).put(m20).put(m30)
        buffer.put(m01).put(m11).put(m21).put(m31)
        buffer.put(m02).put(m12).put(m22).put(m32)
        buffer.put(m03).put(m13).put(m23).put(m33)
        buffer.flip()
    }

    /**
     * Creates a new FloatBuffer with matrix data in column-major order for OpenGL.
     * @deprecated Use toBuffer() with MemoryStack for better memory management
     */
    @Deprecated("Use toBuffer() with MemoryStack for better memory management")
    fun getBuffer(): FloatBuffer {
        val buffer = BufferUtils.createFloatBuffer(16)
        toBuffer(buffer)
        return buffer
    }

    companion object {
        /**
         * Identity matrix.
         */
        val IDENTITY = Matrix4()

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
        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {
            val tx = -(right + left) / (right - left)
            val ty = -(top + bottom) / (top - bottom)
            val tz = -(far + near) / (far - near)

            return Matrix4(
                m00 = 2f / (right - left),
                m11 = 2f / (top - bottom),
                m22 = -2f / (far - near),
                m03 = tx,
                m13 = ty,
                m23 = tz
            )
        }

        /**
         * Creates a perspective projection matrix. Similar to
         * `glFrustum(left, right, bottom, top, near, far)`.
         *
         * @param left   Coordinate for the left vertical clipping pane
         * @param right  Coordinate for the right vertical clipping pane
         * @param bottom Coordinate for the bottom horizontal clipping pane
         * @param top    Coordinate for the bottom horizontal clipping pane
         * @param near   Coordinate for the near depth clipping pane, must be positive
         * @param far    Coordinate for the far depth clipping pane, must be positive
         *
         * @return Perspective matrix
         */
        fun frustum(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {
            val a = (right + left) / (right - left)
            val b = (top + bottom) / (top - bottom)
            val c = -(far + near) / (far - near)
            val d = -(2f * far * near) / (far - near)

            return Matrix4(
                m00 = (2f * near) / (right - left),
                m11 = (2f * near) / (top - bottom),
                m02 = a,
                m12 = b,
                m22 = c,
                m32 = -1f,
                m23 = d,
                m33 = 0f
            )
        }

        /**
         * Creates a perspective projection matrix. Similar to
         * `gluPerspective(fov, aspect, zNear, zFar)`.
         *
         * @param fov    Field of view angle in degrees
         * @param aspect The aspect ratio is the ratio of width to height
         * @param near   Distance from the viewer to the near clipping plane, must be positive
         * @param far    Distance from the viewer to the far clipping plane, must be positive
         *
         * @return Perspective matrix
         */
        fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Matrix4 {
            val f = (1f / tan(Math.toRadians(fov.toDouble()) / 2f)).toFloat()

            return Matrix4(
                m00 = f / aspect,
                m11 = f,
                m22 = (far + near) / (near - far),
                m32 = -1f,
                m23 = (2f * far * near) / (near - far),
                m33 = 0f
            )
        }

        /**
         * Creates a translation matrix. Similar to `glTranslate(x, y, z)`.
         *
         * @param x x coordinate of translation vector
         * @param y y coordinate of translation vector
         * @param z z coordinate of translation vector
         *
         * @return Translation matrix
         */
        fun translate(x: Float, y: Float, z: Float): Matrix4 = Matrix4(
            m03 = x,
            m13 = y,
            m23 = z
        )

        /**
         * Creates a translation matrix from a Vector3f.
         *
         * @param translation Translation vector
         *
         * @return Translation matrix
         */
        fun translate(translation: Vector3f): Matrix4 = translate(translation.x, translation.y, translation.z)

        /**
         * Creates a rotation matrix. Similar to `glRotate(angle, x, y, z)`.
         *
         * @param angle Angle of rotation in degrees
         * @param x     x coordinate of the rotation vector
         * @param y     y coordinate of the rotation vector
         * @param z     z coordinate of the rotation vector
         *
         * @return Rotation matrix
         */
        fun rotate(angle: Float, x: Float, y: Float, z: Float): Matrix4 {
            val c = cos(Math.toRadians(angle.toDouble())).toFloat()
            val s = sin(Math.toRadians(angle.toDouble())).toFloat()
            
            val vec = Vector3f(x, y, z).let { v ->
                if (v.length() != 1f) v.normalize() else v
            }
            
            val nx = vec.x
            val ny = vec.y
            val nz = vec.z

            return Matrix4(
                m00 = nx * nx * (1f - c) + c,
                m01 = nx * ny * (1f - c) - nz * s,
                m02 = nx * nz * (1f - c) + ny * s,
                m10 = ny * nx * (1f - c) + nz * s,
                m11 = ny * ny * (1f - c) + c,
                m12 = ny * nz * (1f - c) - nx * s,
                m20 = nx * nz * (1f - c) - ny * s,
                m21 = ny * nz * (1f - c) + nx * s,
                m22 = nz * nz * (1f - c) + c
            )
        }

        /**
         * Creates a rotation matrix from a Vector3f axis.
         *
         * @param angle Angle of rotation in degrees
         * @param axis  Rotation axis vector
         *
         * @return Rotation matrix
         */
        fun rotate(angle: Float, axis: Vector3f): Matrix4 = rotate(angle, axis.x, axis.y, axis.z)

        /**
         * Creates a scaling matrix. Similar to `glScale(x, y, z)`.
         *
         * @param x Scale factor along the x coordinate
         * @param y Scale factor along the y coordinate
         * @param z Scale factor along the z coordinate
         *
         * @return Scaling matrix
         */
        fun scale(x: Float, y: Float, z: Float): Matrix4 = Matrix4(
            m00 = x,
            m11 = y,
            m22 = z
        )

        /**
         * Creates a scaling matrix from a Vector3f.
         *
         * @param scale Scale vector
         *
         * @return Scaling matrix
         */
        fun scale(scale: Vector3f): Matrix4 = scale(scale.x, scale.y, scale.z)

        /**
         * Creates a uniform scaling matrix.
         *
         * @param scale Uniform scale factor
         *
         * @return Scaling matrix
         */
        fun scale(scale: Float): Matrix4 = scale(scale, scale, scale)
    }
}