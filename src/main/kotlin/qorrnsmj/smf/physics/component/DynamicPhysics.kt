package qorrnsmj.smf.physics.component

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.Collider

/**
 * Dynamic physics: full simulation (gravity, forces, collisions)
 */
class DynamicPhysics(
    override var mass: Float = 1.0f,
    override var velocity: Vector3f = Vector3f(0f, 0f, 0f),
    override var acceleration: Vector3f = Vector3f(0f, 0f, 0f),
    private val forces: MutableList<Vector3f> = mutableListOf(),

    override var restitution: Float = 0.3f,
    override var friction: Float = 0.5f,
    override var drag: Float = 0.01f,

    override var angularVelocity: Vector3f = Vector3f(0f, 0f, 0f),
    override var angularAcceleration: Vector3f = Vector3f(0f, 0f, 0f),

    override var isGrounded: Boolean = false,
    override var groundNormal: Vector3f = Vector3f(0f, 1f, 0f),

    override var collider: Collider? = null
) : IPhysicsComponent {
    override fun applyForce(force: Vector3f) {
        forces.add(force)
    }

    override fun applyImpulse(impulse: Vector3f) {
        velocity = velocity.add(impulse.scale(1f / mass))
    }

    internal fun getTotalForce(): Vector3f {
        return forces.fold(Vector3f(0f, 0f, 0f)) { acc, f -> acc.add(f) }
    }

    internal fun clearForces() {
        forces.clear()
        acceleration = Vector3f(0f, 0f, 0f)
    }

    internal fun calculateAcceleration() {
        if (mass > 0f) {
            acceleration = getTotalForce().scale(1f / mass)
        }
    }

    override fun stop() {
        velocity = Vector3f(0f, 0f, 0f)
    }
}
