package qorrnsmj.smf.math

import java.nio.FloatBuffer

/**
 * Immutable 2x2 Matrix data class. GLSL equivalent to mat2.
 * All operations return new instances, maintaining immutability.
 *
 * @property m00 Element at row 0, column 0
 * @property m01 Element at row 0, column 1
 * @property m10 Element at row 1, column 0
 * @property m11 Element at row 1, column 1
 */
data class Matrix2(
    val m00: Float,
    val m01: Float,
    val m10: Float,
    val m11: Float
) {

    companion object {
        /**
         * Creates a 2x2 identity matrix.
         */
        fun identity() = Matrix2(
            1f, 0f,
            0f, 1f
        )

        /**
         * Creates a 2x2 zero matrix.
         */
        fun zero() = Matrix2(
            0f, 0f,
            0f, 0f
        )

        /**
         * Creates a 2x2 matrix with specified columns.
         *
         * @param col1 Vector with values of the first column
         * @param col2 Vector with values of the second column
         */
        fun fromColumns(col1: Vector2, col2: Vector2) = Matrix2(
            col1.x, col2.x,
            col1.z, col2.z
        )
    }

    /**
     * Default constructor creates an identity matrix.
     */
    constructor() : this(
        1f, 0f,
        0f, 1f
    )

    /**
     * Creates a 2x2 matrix with specified columns.
     *
     * @param col1 Vector with values of the first column
     * @param col2 Vector with values of the second column
     */
    constructor(col1: Vector2, col2: Vector2) : this(
        col1.x, col2.x,
        col1.z, col2.z
    )

    // Operator overloading for addition
    operator fun plus(other: Matrix2): Matrix2 = Matrix2(
        this.m00 + other.m00, this.m01 + other.m01,
        this.m10 + other.m10, this.m11 + other.m11
    )

    // Operator overloading for subtraction
    operator fun minus(other: Matrix2): Matrix2 = Matrix2(
        this.m00 - other.m00, this.m01 - other.m01,
        this.m10 - other.m10, this.m11 - other.m11
    )

    // Operator overloading for scalar multiplication
    operator fun times(scalar: Float): Matrix2 = Matrix2(
        this.m00 * scalar, this.m01 * scalar,
        this.m10 * scalar, this.m11 * scalar
    )

    // Operator overloading for matrix multiplication
    operator fun times(other: Matrix2): Matrix2 = Matrix2(
        this.m00 * other.m00 + this.m01 * other.m10, this.m00 * other.m01 + this.m01 * other.m11,
        this.m10 * other.m00 + this.m11 * other.m10, this.m10 * other.m01 + this.m11 * other.m11
    )

    // Operator overloading for vector multiplication
    operator fun times(vector: Vector2): Vector2 {
        val x = this.m00 * vector.x + this.m01 * vector.z
        val z = this.m10 * vector.x + this.m11 * vector.z
        return Vector2(x, z)
    }

    // Operator overloading for unary minus (negation)
    operator fun unaryMinus(): Matrix2 = Matrix2(
        -this.m00, -this.m01,
        -this.m10, -this.m11
    )

    /**
     * Adds this matrix to another matrix.
     *
     * @param other The other matrix
     * @return Sum of this + other
     */
    fun add(other: Matrix2): Matrix2 = this + other

    /**
     * Negates this matrix.
     *
     * @return Negated matrix
     */
    fun negate(): Matrix2 = -this

    /**
     * Subtracts another matrix from this matrix.
     *
     * @param other The other matrix
     * @return Difference of this - other
     */
    fun subtract(other: Matrix2): Matrix2 = this - other

    /**
     * Multiplies this matrix with a scalar.
     *
     * @param scalar The scalar
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Matrix2 = this * scalar

    /**
     * Multiplies this matrix with a vector.
     *
     * @param vector The vector
     * @return Vector product of this * vector
     */
    fun multiply(vector: Vector2): Vector2 = this * vector

    /**
     * Multiplies this matrix with another matrix.
     *
     * @param other The other matrix
     * @return Matrix product of this * other
     */
    fun multiply(other: Matrix2): Matrix2 = this * other

    /**
     * Transposes this matrix.
     *
     * @return Transposed matrix
     */
    fun transpose(): Matrix2 = Matrix2(
        this.m00, this.m10,
        this.m01, this.m11
    )

    /**
     * Calculates the determinant of this matrix.
     *
     * @return Determinant of this matrix
     */
    fun determinant(): Float = m00 * m11 - m01 * m10

    /**
     * Calculates the inverse of this matrix if it exists.
     *
     * @return Inverse matrix, or null if matrix is not invertible
     */
    fun inverse(): Matrix2? {
        val det = determinant()
        if (det == 0f) return null

        val invDet = 1f / det
        return Matrix2(
            m11 * invDet, -m01 * invDet,
            -m10 * invDet, m00 * invDet
        )
    }

    /**
     * Gets the first column as a Vector2.
     */
    fun getColumn1(): Vector2 = Vector2(m00, m10)

    /**
     * Gets the second column as a Vector2.
     */
    fun getColumn2(): Vector2 = Vector2(m01, m11)

    /**
     * Gets the first row as a Vector2.
     */
    fun getRow1(): Vector2 = Vector2(m00, m01)

    /**
     * Gets the second row as a Vector2.
     */
    fun getRow2(): Vector2 = Vector2(m10, m11)

    /**
     * Stores the matrix in a given FloatBuffer in column-major order (OpenGL format).
     *
     * @param buffer The buffer to store the matrix data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(m00).put(m10)
        buffer.put(m01).put(m11)
        buffer.flip()
    }

    /**
     * Creates a new FloatBuffer with the matrix data in column-major order (OpenGL format).
     *
     * @return FloatBuffer containing matrix data
     */
    fun toBuffer(): FloatBuffer {
        val buffer = FloatBuffer.allocate(4)
        buffer.put(m00).put(m10)
        buffer.put(m01).put(m11)
        buffer.flip()
        return buffer
    }

    /**
     * Gets the matrix as a FloatArray in column-major order (OpenGL format).
     *
     * @return FloatArray containing matrix data
     */
    fun toFloatArray(): FloatArray = floatArrayOf(m00, m10, m01, m11)

    /**
     * Checks if this matrix is approximately equal to another matrix within epsilon tolerance.
     *
     * @param other The other matrix
     * @param epsilon The tolerance for comparison
     * @return True if matrices are approximately equal
     */
    fun equals(other: Matrix2, epsilon: Float = 1e-6f): Boolean {
        return kotlin.math.abs(m00 - other.m00) < epsilon &&
               kotlin.math.abs(m01 - other.m01) < epsilon &&
               kotlin.math.abs(m10 - other.m10) < epsilon &&
               kotlin.math.abs(m11 - other.m11) < epsilon
    }

    /**
     * Returns a formatted string representation of the matrix.
     */
    override fun toString(): String {
        return "Matrix2(\n" +
               "  [${String.format("%.3f", m00)}, ${String.format("%.3f", m01)}]\n" +
               "  [${String.format("%.3f", m10)}, ${String.format("%.3f", m11)}]\n" +
               ")"
    }
}

// Extension functions for scalar multiplication on the left side
operator fun Float.times(matrix: Matrix2): Matrix2 = matrix * this