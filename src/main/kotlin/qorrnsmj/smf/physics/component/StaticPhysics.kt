package qorrnsmj.smf.physics.component

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.Collider

/**
 * Static physics: immovable objects
 */
class StaticPhysics(
    override var collider: Collider? = null
) : IPhysicsComponent {
    override var mass: Float = Float.POSITIVE_INFINITY
    override var velocity: Vector3f = Vector3f(0f, 0f, 0f)
    override var acceleration: Vector3f = Vector3f(0f, 0f, 0f)

    override var restitution: Float = 0f
    override var friction: Float = 1f
    override var drag: Float = 0f

    override var angularVelocity: Vector3f = Vector3f(0f, 0f, 0f)
    override var angularAcceleration: Vector3f = Vector3f(0f, 0f, 0f)

    override var isGrounded: Boolean = true
    override var groundNormal: Vector3f = Vector3f(0f, 1f, 0f)

    override fun applyForce(force: Vector3f) { /* no-op */ }
    override fun applyImpulse(impulse: Vector3f) { /* no-op */ }
    override fun stop() { /* no-op */ }
}
