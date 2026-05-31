package qorrnsmj.smf.physics.collision.data

import qorrnsmj.smf.math.Vector3f

/**
 * Result of a collision detection
 */
data class CollisionResult(
    val penetrationDepth: Float,
    val collisionNormal: Vector3f,
    val contactPoint: Vector3f
)
