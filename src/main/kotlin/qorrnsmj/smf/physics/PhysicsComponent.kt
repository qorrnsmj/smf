package qorrnsmj.smf.physics

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.Collider

/**
 * Physics component for entities that participate in physics simulation
 * Contains physical properties like mass, velocity, and forces
 */
data class PhysicsComponent(
    // Basic physical properties
    var mass: Float = 1.0f,
    var velocity: Vector3f = Vector3f(0f, 0f, 0f),
    var acceleration: Vector3f = Vector3f(0f, 0f, 0f),
    
    // Forces applied to the entity
    private val forces: MutableList<Vector3f> = mutableListOf(),
    
    // Physics behavior flags
    var useGravity: Boolean = true,
    var isStatic: Boolean = false,
    var isKinematic: Boolean = false,
    
    // Physical material properties
    var restitution: Float = 0.3f,  // Bounce factor (0 = no bounce, 1 = perfect bounce)
    var friction: Float = 0.5f,     // Friction coefficient
    var drag: Float = 0.01f,        // Air resistance
    
    // Rotation properties
    var angularVelocity: Vector3f = Vector3f(0f, 0f, 0f),
    var angularAcceleration: Vector3f = Vector3f(0f, 0f, 0f),
    
    // State tracking
    var isGrounded: Boolean = false,
    var groundNormal: Vector3f = Vector3f(0f, 1f, 0f),
    
    // Collision detection
    var collider: Collider? = null
) {
    
    /**
     * Apply a force to this physics component
     */
    fun applyForce(force: Vector3f) {
        if (!isStatic && !isKinematic) {
            forces.add(force)
        }
    }
    
    /**
     * Apply an impulse (immediate velocity change)
     */
    fun applyImpulse(impulse: Vector3f) {
        if (!isStatic && !isKinematic) {
            velocity = velocity.add(impulse.scale(1f / mass))
        }
    }
    
    /**
     * Get the total force acting on this component
     */
    fun getTotalForce(): Vector3f {
        return forces.fold(Vector3f(0f, 0f, 0f)) { acc, force -> acc.add(force) }
    }
    
    /**
     * Clear all accumulated forces
     */
    fun clearForces() {
        forces.clear()
        acceleration = Vector3f(0f, 0f, 0f)
    }
    
    /**
     * Calculate acceleration from current forces (F = ma)
     */
    fun calculateAcceleration() {
        if (!isStatic && !isKinematic && mass > 0f) {
            acceleration = getTotalForce().scale(1f / mass)
        }
    }
    
    /**
     * Set velocity directly (useful for kinematic objects)
     */
    fun setVelocity(newVelocity: Vector3f) {
        velocity = newVelocity
    }
    
    /**
     * Add velocity (useful for impulses)
     */
    fun addVelocity(deltaVelocity: Vector3f) {
        if (!isStatic && !isKinematic) {
            velocity = velocity.add(deltaVelocity)
        }
    }
    
    /**
     * Check if the physics component has significant movement
     */
    fun isMoving(threshold: Float = 0.01f): Boolean {
        return velocity.length() > threshold
    }
    
    /**
     * Stop all movement (set velocity to zero)
     */
    fun stop() {
        velocity = Vector3f(0f, 0f, 0f)
        angularVelocity = Vector3f(0f, 0f, 0f)
    }
}