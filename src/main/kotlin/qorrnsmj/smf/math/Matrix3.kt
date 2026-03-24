package qorrnsmj.smf.math

import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import kotlin.math.abs

/**
 * Immutable 3x3 matrix class for the SMF math library.
 * GLSL equivalent to mat3.
 * 
 * All operations return new Matrix3 instances, maintaining immutability.
 * Supports operator overloading for intuitive mathematical operations.
 *
 * Matrix layout (column-major order):
 * | m00  m01  m02 |
 * | m10  m11  m12 |
 * | m20  m21  m22 |
 */
data class Matrix3(
    val m00: Float = 0f, val m01: Float = 0f, val m02: Float = 0f,
    val m10: Float = 0f, val m11: Float = 0f, val m12: Float = 0f,
    val m20: Float = 0f, val m21: Float = 0f, val m22: Float = 0f
) {

    companion object {
        /**
         * Creates a 3x3 identity matrix.
         */
        val IDENTITY = Matrix3(
            m00 = 1f, m01 = 0f, m02 = 0f,
            m10 = 0f, m11 = 1f, m12 = 0f,
            m20 = 0f, m21 = 0f, m22 = 1f
        )

        /**
         * Creates a zero matrix.
         */
        val ZERO = Matrix3()

        /**
         * Creates a matrix from three column vectors.
         *
         * @param col1 First column vector
         * @param col2 Second column vector
         * @param col3 Third column vector
         */
        fun fromColumns(col1: Vector3f, col2: Vector3f, col3: Vector3f): Matrix3 {
            return Matrix3(
                m00 = col1.x, m01 = col2.x, m02 = col3.x,
                m10 = col1.y, m11 = col2.y, m12 = col3.y,
                m20 = col1.z, m21 = col2.z, m22 = col3.z
            )
        }

        /**
         * Creates a rotation matrix around the X-axis.
         */
        fun rotationX(angle: Float): Matrix3 {
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return Matrix3(
                m00 = 1f, m01 = 0f,  m02 = 0f,
                m10 = 0f, m11 = cos, m12 = -sin,
                m20 = 0f, m21 = sin, m22 = cos
            )
        }

        /**
         * Creates a rotation matrix around the Y-axis.
         */
        fun rotationY(angle: Float): Matrix3 {
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return Matrix3(
                m00 = cos,  m01 = 0f, m02 = sin,
                m10 = 0f,   m11 = 1f, m12 = 0f,
                m20 = -sin, m21 = 0f, m22 = cos
            )
        }

        /**
         * Creates a rotation matrix around the Z-axis.
         */
        fun rotationZ(angle: Float): Matrix3 {
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return Matrix3(
                m00 = cos, m01 = -sin, m02 = 0f,
                m10 = sin, m11 = cos,  m12 = 0f,
                m20 = 0f,  m21 = 0f,   m22 = 1f
            )
        }

        /**
         * Creates a scaling matrix.
         */
        fun scaling(x: Float, y: Float, z: Float): Matrix3 {
            return Matrix3(
                m00 = x, m01 = 0f, m02 = 0f,
                m10 = 0f, m11 = y, m12 = 0f,
                m20 = 0f, m21 = 0f, m22 = z
            )
        }

        /**
         * Creates a uniform scaling matrix.
         */
        fun scaling(scale: Float): Matrix3 = scaling(scale, scale, scale)
    }

    // ============ OPERATOR OVERLOADING ============

    /**
     * Matrix addition operator.
     */
    operator fun plus(other: Matrix3): Matrix3 {
        return Matrix3(
            m00 = this.m00 + other.m00, m01 = this.m01 + other.m01, m02 = this.m02 + other.m02,
            m10 = this.m10 + other.m10, m11 = this.m11 + other.m11, m12 = this.m12 + other.m12,
            m20 = this.m20 + other.m20, m21 = this.m21 + other.m21, m22 = this.m22 + other.m22
        )
    }

    /**
     * Matrix subtraction operator.
     */
    operator fun minus(other: Matrix3): Matrix3 {
        return Matrix3(
            m00 = this.m00 - other.m00, m01 = this.m01 - other.m01, m02 = this.m02 - other.m02,
            m10 = this.m10 - other.m10, m11 = this.m11 - other.m11, m12 = this.m12 - other.m12,
            m20 = this.m20 - other.m20, m21 = this.m21 - other.m21, m22 = this.m22 - other.m22
        )
    }

    /**
     * Matrix negation operator.
     */
    operator fun unaryMinus(): Matrix3 {
        return Matrix3(
            m00 = -this.m00, m01 = -this.m01, m02 = -this.m02,
            m10 = -this.m10, m11 = -this.m11, m12 = -this.m12,
            m20 = -this.m20, m21 = -this.m21, m22 = -this.m22
        )
    }

    /**
     * Matrix-scalar multiplication operator.
     */
    operator fun times(scalar: Float): Matrix3 {
        return Matrix3(
            m00 = this.m00 * scalar, m01 = this.m01 * scalar, m02 = this.m02 * scalar,
            m10 = this.m10 * scalar, m11 = this.m11 * scalar, m12 = this.m12 * scalar,
            m20 = this.m20 * scalar, m21 = this.m21 * scalar, m22 = this.m22 * scalar
        )
    }

    /**
     * Matrix-matrix multiplication operator.
     */
    operator fun times(other: Matrix3): Matrix3 {
        return Matrix3(
            m00 = this.m00 * other.m00 + this.m01 * other.m10 + this.m02 * other.m20,
            m10 = this.m10 * other.m00 + this.m11 * other.m10 + this.m12 * other.m20,
            m20 = this.m20 * other.m00 + this.m21 * other.m10 + this.m22 * other.m20,

            m01 = this.m00 * other.m01 + this.m01 * other.m11 + this.m02 * other.m21,
            m11 = this.m10 * other.m01 + this.m11 * other.m11 + this.m12 * other.m21,
            m21 = this.m20 * other.m01 + this.m21 * other.m11 + this.m22 * other.m21,

            m02 = this.m00 * other.m02 + this.m01 * other.m12 + this.m02 * other.m22,
            m12 = this.m10 * other.m02 + this.m11 * other.m12 + this.m12 * other.m22,
            m22 = this.m20 * other.m02 + this.m21 * other.m12 + this.m22 * other.m22
        )
    }

    /**
     * Matrix-vector multiplication operator.
     */
    operator fun times(vector: Vector3f): Vector3f {
        return Vector3f(
            x = this.m00 * vector.x + this.m01 * vector.y + this.m02 * vector.z,
            y = this.m10 * vector.x + this.m11 * vector.y + this.m12 * vector.z,
            z = this.m20 * vector.x + this.m21 * vector.y + this.m22 * vector.z
        )
    }

    /**
     * Matrix division by scalar operator.
     */
    operator fun div(scalar: Float): Matrix3 {
        return times(1f / scalar)
    }

    // ============ MATHEMATICAL OPERATIONS ============

    /**
     * Returns the transpose of this matrix.
     */
    fun transpose(): Matrix3 {
        return Matrix3(
            m00 = this.m00, m01 = this.m10, m02 = this.m20,
            m10 = this.m01, m11 = this.m11, m12 = this.m21,
            m20 = this.m02, m21 = this.m12, m22 = this.m22
        )
    }

    /**
     * Calculates the determinant of this matrix.
     */
    fun determinant(): Float {
        return m00 * (m11 * m22 - m12 * m21) -
               m01 * (m10 * m22 - m12 * m20) +
               m02 * (m10 * m21 - m11 * m20)
    }

    /**
     * Returns the inverse of this matrix, or null if the matrix is not invertible.
     */
    fun inverse(): Matrix3? {
        val det = determinant()
        if (abs(det) < 1e-6f) return null

        val invDet = 1f / det

        return Matrix3(
            m00 = (m11 * m22 - m12 * m21) * invDet,
            m01 = (m02 * m21 - m01 * m22) * invDet,
            m02 = (m01 * m12 - m02 * m11) * invDet,

            m10 = (m12 * m20 - m10 * m22) * invDet,
            m11 = (m00 * m22 - m02 * m20) * invDet,
            m12 = (m02 * m10 - m00 * m12) * invDet,

            m20 = (m10 * m21 - m11 * m20) * invDet,
            m21 = (m01 * m20 - m00 * m21) * invDet,
            m22 = (m00 * m11 - m01 * m10) * invDet
        )
    }

    /**
     * Returns the trace (sum of diagonal elements) of this matrix.
     */
    fun trace(): Float = m00 + m11 + m22

    /**
     * Checks if this matrix is approximately equal to another matrix within the given tolerance.
     */
    fun isApproximatelyEqual(other: Matrix3, tolerance: Float = 1e-6f): Boolean {
        return abs(m00 - other.m00) < tolerance &&
               abs(m01 - other.m01) < tolerance &&
               abs(m02 - other.m02) < tolerance &&
               abs(m10 - other.m10) < tolerance &&
               abs(m11 - other.m11) < tolerance &&
               abs(m12 - other.m12) < tolerance &&
               abs(m20 - other.m20) < tolerance &&
               abs(m21 - other.m21) < tolerance &&
               abs(m22 - other.m22) < tolerance
    }

    // ============ COMPATIBILITY WITH EXISTING API ============

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun add(other: Matrix3): Matrix3 = this + other

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun subtract(other: Matrix3): Matrix3 = this - other

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun multiply(scalar: Float): Matrix3 = this * scalar

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun multiply(other: Matrix3): Matrix3 = this * other

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun multiply(vector: Vector3f): Vector3f = this * vector

    /**
     * Legacy method for compatibility with Matrix3f API.
     */
    fun negate(): Matrix3 = -this

    // ============ OPENGL INTEGRATION ============

    /**
     * Stores this matrix in the given FloatBuffer in column-major order.
     * 
     * @param buffer The buffer to store the matrix data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(m00).put(m10).put(m20)
        buffer.put(m01).put(m11).put(m21)
        buffer.put(m02).put(m12).put(m22)
        buffer.flip()
    }

    /**
     * Returns a new FloatBuffer containing this matrix in column-major order.
     * Compatible with OpenGL expectations.
     */
    fun toBuffer(): FloatBuffer {
        val buffer = BufferUtils.createFloatBuffer(9)
        buffer.put(m00).put(m10).put(m20)
        buffer.put(m01).put(m11).put(m21)
        buffer.put(m02).put(m12).put(m22)
        buffer.flip()
        return buffer
    }

    /**
     * Returns this matrix as a FloatArray in column-major order.
     */
    fun toArray(): FloatArray = floatArrayOf(
        m00, m10, m20,
        m01, m11, m21,
        m02, m12, m22
    )

    // ============ UTILITY METHODS ============

    /**
     * Gets the specified column as a Vector3f.
     */
    fun getColumn(index: Int): Vector3f = when (index) {
        0 -> Vector3f(m00, m10, m20)
        1 -> Vector3f(m01, m11, m21)
        2 -> Vector3f(m02, m12, m22)
        else -> throw IndexOutOfBoundsException("Column index must be 0, 1, or 2")
    }

    /**
     * Gets the specified row as a Vector3f.
     */
    fun getRow(index: Int): Vector3f = when (index) {
        0 -> Vector3f(m00, m01, m02)
        1 -> Vector3f(m10, m11, m12)
        2 -> Vector3f(m20, m21, m22)
        else -> throw IndexOutOfBoundsException("Row index must be 0, 1, or 2")
    }

    /**
     * Returns a string representation of this matrix.
     */
    override fun toString(): String {
        return """
            |Matrix3:
            |[${String.format("%8.3f", m00)} ${String.format("%8.3f", m01)} ${String.format("%8.3f", m02)}]
            |[${String.format("%8.3f", m10)} ${String.format("%8.3f", m11)} ${String.format("%8.3f", m12)}]
            |[${String.format("%8.3f", m20)} ${String.format("%8.3f", m21)} ${String.format("%8.3f", m22)}]
        """.trimMargin()
    }
}

// ============ EXTENSION FUNCTIONS FOR SCALAR OPERATIONS ============

/**
 * Scalar-matrix multiplication.
 */
operator fun Float.times(matrix: Matrix3): Matrix3 = matrix * this