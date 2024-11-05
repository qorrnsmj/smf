package qorrnsmj.smf.math

import java.nio.FloatBuffer

/**
 * This class represents a 2x2-Matrix. GLSL equivalent to mat2.
 *
 * @author Heiko Brumme
 */
class Matrix2f {
    private var m00 = 0f
    private var m01 = 0f
    private var m10 = 0f
    private var m11 = 0f

    /**
     * Create a 2x2 identity matrix.
     */
    constructor() {
        setIdentity()
    }

    /**
     * Creates a 2x2 matrix with specified columns.
     *
     * @param col1 Vector with values of the first column
     * @param col2 Vector with values of the second column
     */
    constructor(col1: Vector2f, col2: Vector2f) {
        m00 = col1.x
        m10 = col1.y

        m01 = col2.x
        m11 = col2.y
    }

    /**
     * Sets this matrix to the identity matrix.
     */
    fun setIdentity() {
        m00 = 1f
        m11 = 1f

        m01 = 0f
        m10 = 0f
    }

    /**
     * Adds this matrix to another matrix.
     *
     * @param other The other matrix
     *
     * @return Sum of this + other
     */
    fun add(other: Matrix2f): Matrix2f {
        val result = Matrix2f()

        result.m00 = this.m00 + other.m00
        result.m10 = this.m10 + other.m10

        result.m01 = this.m01 + other.m01
        result.m11 = this.m11 + other.m11

        return result
    }

    /**
     * Negates this matrix.
     *
     * @return Negated matrix
     */
    fun negate(): Matrix2f {
        return multiply(-1f)
    }

    /**
     * Subtracts this matrix from another matrix.
     *
     * @param other The other matrix
     *
     * @return Difference of this - other
     */
    fun subtract(other: Matrix2f): Matrix2f {
        return this.add(other.negate())
    }

    /**
     * Multiplies this matrix with a scalar.
     *
     * @param scalar The scalar
     *
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Matrix2f {
        val result = Matrix2f()

        result.m00 = this.m00 * scalar
        result.m10 = this.m10 * scalar

        result.m01 = this.m01 * scalar
        result.m11 = this.m11 * scalar

        return result
    }

    /**
     * Multiplies this matrix to a vector.
     *
     * @param vector The vector
     *
     * @return Vector product of this * other
     */
    fun multiply(vector: Vector2f): Vector2f {
        val x = this.m00 * vector.x + this.m01 * vector.y
        val y = this.m10 * vector.x + this.m11 * vector.y
        return Vector2f(x, y)
    }

    /**
     * Multiplies this matrix to another matrix.
     *
     * @param other The other matrix
     *
     * @return Matrix product of this * other
     */
    fun multiply(other: Matrix2f): Matrix2f {
        val result = Matrix2f()

        result.m00 = this.m00 * other.m00 + this.m01 * other.m10
        result.m10 = this.m10 * other.m00 + this.m11 * other.m10

        result.m01 = this.m00 * other.m01 + this.m01 * other.m11
        result.m11 = this.m10 * other.m01 + this.m11 * other.m11

        return result
    }

    /**
     * Transposes this matrix.
     *
     * @return Transposed matrix
     */
    fun transpose(): Matrix2f {
        val result = Matrix2f()

        result.m00 = this.m00
        result.m10 = this.m01

        result.m01 = this.m10
        result.m11 = this.m11

        return result
    }

    /**
     * Stores the matrix in a given Buffer.
     *
     * @param buffer The buffer to store the matrix data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(m00).put(m10)
        buffer.put(m01).put(m11)
        buffer.flip()
    }
}
