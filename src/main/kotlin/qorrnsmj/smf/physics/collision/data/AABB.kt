package qorrnsmj.smf.physics.collision.data

import qorrnsmj.smf.math.Vector3f

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
