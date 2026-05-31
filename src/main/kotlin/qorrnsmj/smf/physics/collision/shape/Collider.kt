package qorrnsmj.smf.physics.collision.shape

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.data.CollisionResult

/**
 * Base interface for collision shapes
 */
interface Collider {
    fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult?
    fun getCenter(position: Vector3f): Vector3f
    fun getBounds(position: Vector3f): AABB
}
