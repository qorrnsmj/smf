package qorrnsmj.smf.math

import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Immutable 4D vector class representing a (x,y,z,w) vector.
 * GLSL equivalent to vec4. Used for homogeneous coordinates and RGBA colors.
 * All operations return new instances, maintaining immutability.
 */
data class Vector4(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 0f
) {
    // Companion object for common vectors
    companion object {
        val ZERO = Vector4(0f, 0f, 0f, 0f)
        val ONE = Vector4(1f, 1f, 1f, 1f)
        val UNIT_X = Vector4(1f, 0f, 0f, 0f)
        val UNIT_Y = Vector4(0f, 1f, 0f, 0f)
        val UNIT_Z = Vector4(0f, 0f, 1f, 0f)
        val UNIT_W = Vector4(0f, 0f, 0f, 1f)
    }

    // Properties for future RGBA swizzling support
    val r: Float get() = x
    val g: Float get() = y
    val b: Float get() = z
    val a: Float get() = w

    /**
     * Calculates the squared length of the vector.
     * More efficient than length() when only comparing magnitudes.
     *
     * @return Squared length of this vector
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z + w * w
    }

    /**
     * Calculates the length (magnitude) of the vector.
     *
     * @return Length of this vector
     */
    fun length(): Float {
        return sqrt(lengthSquared())
    }

    /**
     * Returns a normalized version of this vector.
     * Handles zero-length vector edge case by returning ZERO.
     *
     * @return Normalized vector, or ZERO if this vector has zero length
     */
    fun normalize(): Vector4 {
        val len = length()
        return if (len == 0f) ZERO else this / len
    }

    /**
     * Adds this vector to another vector.
     *
     * @param other The other vector
     * @return Sum of this + other
     */
    fun add(other: Vector4): Vector4 {
        return Vector4(x + other.x, y + other.y, z + other.z, w + other.w)
    }

    /**
     * Subtracts another vector from this vector.
     *
     * @param other The other vector
     * @return Difference of this - other
     */
    fun subtract(other: Vector4): Vector4 {
        return Vector4(x - other.x, y - other.y, z - other.z, w - other.w)
    }

    /**
     * Multiplies this vector by a scalar.
     *
     * @param scalar Scalar to multiply by
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Vector4 {
        return Vector4(x * scalar, y * scalar, z * scalar, w * scalar)
    }

    /**
     * Component-wise multiplication with another vector.
     *
     * @param other The other vector
     * @return Component-wise product
     */
    fun multiply(other: Vector4): Vector4 {
        return Vector4(x * other.x, y * other.y, z * other.z, w * other.w)
    }

    /**
     * Divides this vector by a scalar.
     * Handles division by zero by returning ZERO.
     *
     * @param scalar Scalar to divide by
     * @return Scalar quotient of this / scalar, or ZERO if scalar is zero
     */
    fun divide(scalar: Float): Vector4 {
        return if (scalar == 0f) ZERO else Vector4(x / scalar, y / scalar, z / scalar, w / scalar)
    }

    /**
     * Component-wise division by another vector.
     * Handles division by zero by using 0f for that component.
     *
     * @param other The other vector
     * @return Component-wise quotient
     */
    fun divide(other: Vector4): Vector4 {
        return Vector4(
            if (other.x == 0f) 0f else x / other.x,
            if (other.y == 0f) 0f else y / other.y,
            if (other.z == 0f) 0f else z / other.z,
            if (other.w == 0f) 0f else w / other.w
        )
    }

    /**
     * Negates this vector.
     *
     * @return Negated vector
     */
    fun negate(): Vector4 {
        return Vector4(-x, -y, -z, -w)
    }

    /**
     * Calculates the dot product of this vector with another vector.
     *
     * @param other The other vector
     * @return Dot product of this · other
     */
    fun dot(other: Vector4): Float {
        return x * other.x + y * other.y + z * other.z + w * other.w
    }

    /**
     * Calculates linear interpolation between this vector and another vector.
     *
     * @param other The target vector
     * @param alpha The interpolation factor, should be between 0.0 and 1.0
     * @return Linear interpolated vector
     */
    fun lerp(other: Vector4, alpha: Float): Vector4 {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        return this * (1f - clampedAlpha) + other * clampedAlpha
    }

    /**
     * Stores the vector components in a FloatBuffer for OpenGL integration.
     *
     * @param buffer The buffer to store the vector data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(x).put(y).put(z).put(w)
        buffer.flip()
    }

    // Operator overloading for arithmetic operations

    /**
     * Addition operator overload.
     */
    operator fun plus(other: Vector4): Vector4 = add(other)

    /**
     * Subtraction operator overload.
     */
    operator fun minus(other: Vector4): Vector4 = subtract(other)

    /**
     * Scalar multiplication operator overload.
     */
    operator fun times(scalar: Float): Vector4 = multiply(scalar)

    /**
     * Component-wise multiplication operator overload.
     */
    operator fun times(other: Vector4): Vector4 = multiply(other)

    /**
     * Scalar division operator overload.
     */
    operator fun div(scalar: Float): Vector4 = divide(scalar)

    /**
     * Component-wise division operator overload.
     */
    operator fun div(other: Vector4): Vector4 = divide(other)

    /**
     * Unary minus operator overload.
     */
    operator fun unaryMinus(): Vector4 = negate()

    override fun toString(): String {
        return "($x, $y, $z, $w)"
    }
}

/**
 * Extension function for scalar multiplication from the left side.
 */
operator fun Float.times(vector: Vector4): Vector4 = vector * this