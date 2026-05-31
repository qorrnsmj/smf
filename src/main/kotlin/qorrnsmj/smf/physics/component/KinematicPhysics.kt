package qorrnsmj.smf.physics.component

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.Collider

/**
 * Kinematic physics: no gravity/force integration; velocity controlled externally
 */
class KinematicPhysics(
    override var mass: Float = 1.0f,
    override var velocity: Vector3f = Vector3f(0f, 0f, 0f),
    override var acceleration: Vector3f = Vector3f(0f, 0f, 0f),

    override var restitution: Float = 0f,
    override var friction: Float = 0f,
    override var drag: Float = 0f,

    override var angularVelocity: Vector3f = Vector3f(0f, 0f, 0f),
    override var angularAcceleration: Vector3f = Vector3f(0f, 0f, 0f),

    override var isGrounded: Boolean = false,
    override var groundNormal: Vector3f = Vector3f(0f, 1f, 0f),

    override var collider: Collider? = null
) : IPhysicsComponent {
    override fun applyForce(force: Vector3f) { /* no-op */ }
    override fun applyImpulse(impulse: Vector3f) { velocity = velocity.add(impulse) }
    override fun stop() { velocity = Vector3f(0f, 0f, 0f) }
}
