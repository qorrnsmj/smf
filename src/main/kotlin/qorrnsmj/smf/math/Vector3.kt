package qorrnsmj.smf.math

import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Immutable 3D vector class with comprehensive vector operations.
 * This class represents a (x,y,z)-Vector. GLSL equivalent to vec3.
 * 
 * All operations return new instances to maintain immutability.
 * 
 * @property x The x component
 * @property y The y component  
 * @property z The z component
 */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UNIT_X = Vector3(1f, 0f, 0f)
        val UNIT_Y = Vector3(0f, 1f, 0f)
        val UNIT_Z = Vector3(0f, 0f, 1f)
    }

    /**
     * Calculates the squared length of the vector.
     * Useful for distance comparisons without expensive sqrt operation.
     * 
     * @return Squared length of this vector
     */
    fun lengthSquared(): Float = x * x + y * y + z * z

    /**
     * Calculates the length (magnitude) of the vector.
     * 
     * @return Length of this vector
     */
    fun length(): Float = sqrt(lengthSquared())

    /**
     * Returns a normalized copy of this vector.
     * If the vector has zero length, returns zero vector to avoid division by zero.
     * 
     * @return Normalized vector or zero vector if this has zero length
     */
    fun normalize(): Vector3 {
        val len = length()
        return if (len == 0f) ZERO else this / len
    }

    /**
     * Returns true if this vector has approximately zero length.
     * 
     * @param epsilon The tolerance for zero comparison
     * @return True if length is less than epsilon
     */
    fun isZero(epsilon: Float = 0.000001f): Boolean = length() < epsilon

    /**
     * Adds this vector to another vector.
     * 
     * @param other The other vector
     * @return Sum of this + other
     */
    fun add(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

    /**
     * Subtracts another vector from this vector.
     * 
     * @param other The other vector
     * @return Difference of this - other
     */
    fun subtract(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)

    /**
     * Multiplies this vector by a scalar.
     * 
     * @param scalar Scalar to multiply by
     * @return Scalar product of this * scalar
     */
    fun multiply(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)

    /**
     * Divides this vector by a scalar.
     * If scalar is zero, returns zero vector to avoid division by zero.
     * 
     * @param scalar Scalar to divide by
     * @return Scalar quotient of this / scalar
     */
    fun divide(scalar: Float): Vector3 {
        return if (scalar == 0f) ZERO else Vector3(x / scalar, y / scalar, z / scalar)
    }

    /**
     * Component-wise multiplication with another vector.
     * 
     * @param other The other vector
     * @return Component-wise product
     */
    fun multiply(other: Vector3): Vector3 = Vector3(x * other.x, y * other.y, z * other.z)

    /**
     * Component-wise division with another vector.
     * Zero components in other vector result in zero in the corresponding result component.
     * 
     * @param other The other vector
     * @return Component-wise quotient
     */
    fun divide(other: Vector3): Vector3 = Vector3(
        if (other.x == 0f) 0f else x / other.x,
        if (other.y == 0f) 0f else y / other.y,
        if (other.z == 0f) 0f else z / other.z
    )

    /**
     * Returns the negated version of this vector.
     * 
     * @return Negated vector
     */
    fun negate(): Vector3 = Vector3(-x, -y, -z)

    /**
     * Calculates the dot product of this vector with another vector.
     * 
     * @param other The other vector
     * @return Dot product of this · other
     */
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    /**
     * Calculates the cross product of this vector with another vector.
     * The cross product is perpendicular to both input vectors.
     * 
     * @param other The other vector
     * @return Cross product of this × other
     */
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    /**
     * Calculates a linear interpolation between this vector and another vector.
     * 
     * @param other The other vector
     * @param alpha The interpolation factor, should be between 0.0 and 1.0
     * @return Linearly interpolated vector
     */
    fun lerp(other: Vector3, alpha: Float): Vector3 {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        return this * (1f - clampedAlpha) + other * clampedAlpha
    }

    /**
     * Returns the distance between this vector and another vector.
     * 
     * @param other The other vector
     * @return Distance between the two points
     */
    fun distance(other: Vector3): Float = (this - other).length()

    /**
     * Returns the squared distance between this vector and another vector.
     * Useful for distance comparisons without expensive sqrt operation.
     * 
     * @param other The other vector
     * @return Squared distance between the two points
     */
    fun distanceSquared(other: Vector3): Float = (this - other).lengthSquared()

    /**
     * Stores the vector components in a FloatBuffer for OpenGL usage.
     * The buffer is flipped after writing.
     * 
     * @param buffer The buffer to store the vector data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(x).put(y).put(z)
        buffer.flip()
    }

    /**
     * Returns an array containing the vector components [x, y, z].
     * 
     * @return Float array with vector components
     */
    fun toArray(): FloatArray = floatArrayOf(x, y, z)

    // Operator overloading for mathematical operations
    
    /**
     * Addition operator overload.
     */
    operator fun plus(other: Vector3): Vector3 = add(other)

    /**
     * Subtraction operator overload.
     */
    operator fun minus(other: Vector3): Vector3 = subtract(other)

    /**
     * Scalar multiplication operator overload.
     */
    operator fun times(scalar: Float): Vector3 = multiply(scalar)

    /**
     * Component-wise multiplication operator overload.
     */
    operator fun times(other: Vector3): Vector3 = multiply(other)

    /**
     * Scalar division operator overload.
     */
    operator fun div(scalar: Float): Vector3 = divide(scalar)

    /**
     * Component-wise division operator overload.
     */
    operator fun div(other: Vector3): Vector3 = divide(other)

    /**
     * Unary minus operator overload.
     */
    operator fun unaryMinus(): Vector3 = negate()

    /**
     * Index access operator overload.
     * 
     * @param index Component index (0=x, 1=y, 2=z)
     * @return Component value
     * @throws IndexOutOfBoundsException if index is not 0, 1, or 2
     */
    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException("Vector3 index must be 0, 1, or 2")
    }

    override fun toString(): String = "($x, $y, $z)"
}

/**
 * Extension function to allow scalar * vector multiplication.
 */
operator fun Float.times(vector: Vector3): Vector3 = vector * this

/**
 * Extension function to allow Int * vector multiplication.
 */
operator fun Int.times(vector: Vector3): Vector3 = vector * this.toFloat()