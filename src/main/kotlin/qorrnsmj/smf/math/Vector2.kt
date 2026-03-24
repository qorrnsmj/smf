package qorrnsmj.smf.math

import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Immutable 2D vector class representing coordinates on a 3D horizontal plane (x, z).
 * All operations return new instances to maintain immutability.
 */
data class Vector2(val x: Float, val z: Float) {
    
    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UNIT_X = Vector2(1f, 0f)
        val UNIT_Z = Vector2(0f, 1f)
    }
    
    // Basic arithmetic operations with operator overloading
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, z + other.z)
    
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, z - other.z)
    
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, z * scalar)
    
    operator fun div(scalar: Float): Vector2 {
        if (scalar == 0f) {
            throw IllegalArgumentException("Cannot divide vector by zero")
        }
        return Vector2(x / scalar, z / scalar)
    }
    
    // Allow scalar to be on the left side of multiplication
    operator fun Float.times(vector: Vector2): Vector2 = vector * this
    
    // Unary operators
    operator fun unaryMinus(): Vector2 = Vector2(-x, -z)
    
    operator fun unaryPlus(): Vector2 = this
    
    // Mathematical operations
    
    /**
     * Calculates the dot product with another vector.
     */
    infix fun dot(other: Vector2): Float = x * other.x + z * other.z
    
    /**
     * Calculates the squared length of this vector.
     * Useful for performance when only comparing lengths.
     */
    fun lengthSquared(): Float = x * x + z * z
    
    /**
     * Calculates the length (magnitude) of this vector.
     */
    fun length(): Float = sqrt(lengthSquared())
    
    /**
     * Returns a normalized version of this vector.
     * Throws exception if the vector has zero length.
     */
    fun normalize(): Vector2 {
        val len = length()
        if (len == 0f) {
            throw IllegalStateException("Cannot normalize zero-length vector")
        }
        return this / len
    }
    
    /**
     * Returns a normalized version of this vector, or zero vector if length is zero.
     */
    fun normalizeOrZero(): Vector2 {
        val len = length()
        return if (len == 0f) ZERO else this / len
    }
    
    /**
     * Linear interpolation between this vector and another.
     * @param other The target vector
     * @param t Interpolation factor (0.0 = this vector, 1.0 = other vector)
     */
    fun lerp(other: Vector2, t: Float): Vector2 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector2(
            x + (other.x - x) * clampedT,
            z + (other.z - z) * clampedT
        )
    }
    
    /**
     * Calculates the distance between this vector and another.
     */
    fun distanceTo(other: Vector2): Float = (this - other).length()
    
    /**
     * Calculates the squared distance between this vector and another.
     * Useful for performance when only comparing distances.
     */
    fun distanceSquaredTo(other: Vector2): Float = (this - other).lengthSquared()
    
    /**
     * Creates a FloatBuffer containing the vector components for OpenGL integration.
     * Buffer contains [x, z] in that order.
     */
    fun toBuffer(): FloatBuffer {
        val buffer = FloatBuffer.allocate(2)
        buffer.put(x)
        buffer.put(z)
        buffer.flip()
        return buffer
    }
    
    /**
     * Adds the vector components to an existing FloatBuffer.
     * Useful for batching multiple vectors into a single buffer.
     */
    fun addToBuffer(buffer: FloatBuffer) {
        buffer.put(x)
        buffer.put(z)
    }
    
    /**
     * Returns true if this vector is approximately equal to another within the given epsilon.
     */
    fun isApproximatelyEqual(other: Vector2, epsilon: Float = 0.0001f): Boolean {
        return kotlin.math.abs(x - other.x) < epsilon && kotlin.math.abs(z - other.z) < epsilon
    }
    
    /**
     * Returns true if this vector has zero length within the given epsilon.
     */
    fun isZero(epsilon: Float = 0.0001f): Boolean = lengthSquared() < epsilon * epsilon
    
    /**
     * Clamps the vector components to the given range.
     */
    fun clamp(min: Float, max: Float): Vector2 = Vector2(
        x.coerceIn(min, max),
        z.coerceIn(min, max)
    )
    
    /**
     * Returns a vector with the absolute values of the components.
     */
    fun abs(): Vector2 = Vector2(kotlin.math.abs(x), kotlin.math.abs(z))
    
    override fun toString(): String = "Vector2(x=$x, z=$z)"
}