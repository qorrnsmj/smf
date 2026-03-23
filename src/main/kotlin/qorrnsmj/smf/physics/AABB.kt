package qorrnsmj.smf.physics

import qorrnsmj.smf.math.Vector3f

/**
 * Axis-Aligned Bounding Box for collision detection
 */
data class AABB(
    val min: Vector3f,
    val max: Vector3f
) {
    /**
     * Check if this AABB intersects with another AABB
     */
    fun intersects(other: AABB): Boolean {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z)
    }

    /**
     * Get the center point of the AABB
     */
    fun getCenter(): Vector3f {
        return Vector3f(
            (min.x + max.x) / 2f,
            (min.y + max.y) / 2f,
            (min.z + max.z) / 2f
        )
    }

    /**
     * Get the size of the AABB
     */
    fun getSize(): Vector3f {
        return Vector3f(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z
        )
    }

    companion object {
        /**
         * Create AABB from center position and half extents
         */
        fun fromCenterAndSize(center: Vector3f, halfExtents: Vector3f): AABB {
            return AABB(
                min = Vector3f(
                    center.x - halfExtents.x,
                    center.y - halfExtents.y,
                    center.z - halfExtents.z
                ),
                max = Vector3f(
                    center.x + halfExtents.x,
                    center.y + halfExtents.y,
                    center.z + halfExtents.z
                )
            )
        }
    }
}
