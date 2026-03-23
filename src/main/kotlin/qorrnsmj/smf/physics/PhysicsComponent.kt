package qorrnsmj.smf.physics

import qorrnsmj.smf.math.Vector3f

/**
 * Physics component for entities with gravity and collision
 */
data class PhysicsComponent(
    var velocity: Vector3f = Vector3f(0f, 0f, 0f),
    var gravity: Float = 0.1f,
    var useGravity: Boolean = true,
    var collisionBounds: Vector3f = Vector3f(1f, 1f, 1f), // Half extents
    var grounded: Boolean = false,
    var mass: Float = 1f
) {
    /**
     * Get AABB for collision detection based on entity position
     */
    fun getAABB(position: Vector3f, scale: Vector3f): AABB {
        val scaledBounds = Vector3f(
            collisionBounds.x * scale.x,
            collisionBounds.y * scale.y,
            collisionBounds.z * scale.z
        )
        return AABB.fromCenterAndSize(position, scaledBounds)
    }
}
