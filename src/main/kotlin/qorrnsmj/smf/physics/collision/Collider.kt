package qorrnsmj.smf.physics.collision

import qorrnsmj.smf.math.Vector3f

/**
 * Base interface for collision shapes
 */
interface Collider {
    fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult?
    fun getCenter(position: Vector3f): Vector3f
    fun getBounds(position: Vector3f): AABB
}

/**
 * Sealed class representing different collision shapes
 */
sealed class ColliderShape {
    data class Sphere(val radius: Float) : ColliderShape()
    data class Box(val width: Float, val height: Float, val depth: Float) : ColliderShape()
}

/**
 * Result of a collision detection
 */
data class CollisionResult(
    val hasCollision: Boolean,
    val penetrationDepth: Float,
    val collisionNormal: Vector3f,
    val contactPoint: Vector3f
)

/**
 * Axis-Aligned Bounding Box for broad-phase collision detection
 */
data class AABB(
    val min: Vector3f,
    val max: Vector3f
) {
    fun intersects(other: AABB): Boolean {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z)
    }
    
    fun contains(point: Vector3f): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }
}