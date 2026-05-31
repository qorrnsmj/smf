package qorrnsmj.smf.physics.component

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.Collider

/**
 * Common physics component interface used by entities and the physics system.
 */
interface IPhysicsComponent {
    var mass: Float
    var velocity: Vector3f
    var acceleration: Vector3f

    var restitution: Float
    var friction: Float
    var drag: Float

    var angularVelocity: Vector3f
    var angularAcceleration: Vector3f

    var isGrounded: Boolean
    var groundNormal: Vector3f

    var collider: Collider?

    fun applyForce(force: Vector3f)
    fun applyImpulse(impulse: Vector3f)
    fun stop()
}
