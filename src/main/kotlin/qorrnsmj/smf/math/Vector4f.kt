package qorrnsmj.smf.math

import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * This class represents a (x,y,z,w)-Vector. GLSL equivalent to vec4.
 *
 * @author Heiko Brumme
 */
class Vector4f {
    var x: Float
    var y: Float
    var z: Float
    var w: Float

    /**
     * Creates a default 4-tuple vector with all values set to 0.
     */
    constructor() {
        this.x = 0f
        this.y = 0f
        this.z = 0f
        this.w = 0f
    }

    /**
     * Creates a 4-tuple vector with specified values.
     *
     * @param x x value
     * @param y y value
     * @param z z value
     * @param w w value
     */
    constructor(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    /**
     * Calculates the squared length of the vector.
     *
     * @return Squared length of this vector
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z + w * w
    }

    /**
     * Calculates the length of the vector.
     *
     * @return Length of this vector
     */
    fun length(): Float {
        return sqrt(lengthSquared().toDouble()).toFloat()
    }

    /**
     * Normalizes the vector.
     *
     * @return Normalized vector
     */
    fun normalize(): Vector4f {
        val length = length()
        return divide(length)
    }

    /**
     * Adds this vector to another vector.
     *
     * @param other The other vector
     *
     * @return Sum of this + other
     */
    fun add(other: Vector4f): Vector4f {
        val x = this.x + other.x
        val y = this.y + other.y
        val z = this.z + other.z
        val w = this.w + other.w
        return Vector4f(x, y, z, w)
    }

    /**
     * Negates this vector.
     *
     * @return Negated vector
     */
    fun negate(): Vector4f {
        return scale(-1f)
    }

    /**
     * Subtracts this vector from another vector.
     *
     * @param other The other vector
     *
     * @return Difference of this - other
     */
    fun subtract(other: Vector4f): Vector4f {
        return this.add(other.negate())
    }

    /**
     * Multiplies a vector by a scalar.
     *
     * @param scalar Scalar to multiply
     *
     * @return Scalar product of this * scalar
     */
    fun scale(scalar: Float): Vector4f {
        val x = this.x * scalar
        val y = this.y * scalar
        val z = this.z * scalar
        val w = this.w * scalar
        return Vector4f(x, y, z, w)
    }

    /**
     * Divides a vector by a scalar.
     *
     * @param scalar Scalar to multiply
     *
     * @return Scalar quotient of this / scalar
     */
    fun divide(scalar: Float): Vector4f {
        return scale(1f / scalar)
    }

    /**
     * Calculates the dot product of this vector with another vector.
     *
     * @param other The other vector
     *
     * @return Dot product of this * other
     */
    fun dot(other: Vector4f): Float {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w
    }

    /**
     * Calculates a linear interpolation between this vector with another
     * vector.
     *
     * @param other The other vector
     * @param alpha The alpha value, must be between 0.0 and 1.0
     *
     * @return Linear interpolated vector
     */
    fun lerp(other: Vector4f, alpha: Float): Vector4f {
        return scale(1f - alpha).add(other.scale(alpha))
    }

    /**
     * Stores the vector in a given Buffer.
     *
     * @param buffer The buffer to store the vector data
     */
    fun toBuffer(buffer: FloatBuffer) {
        buffer.put(x).put(y).put(z).put(w)
        buffer.flip()
    }
}
