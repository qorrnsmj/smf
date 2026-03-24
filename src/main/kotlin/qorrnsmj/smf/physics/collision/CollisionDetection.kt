package qorrnsmj.smf.physics.collision

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.math.Vector3f

/**
 * Collision detection utility for physics simulation
 * Handles broad-phase and narrow-phase collision detection
 */
object CollisionDetection {
    
    /**
     * Perform collision detection between all entities
     */
    fun detectCollisions(entities: List<Entity>): List<CollisionPair> {
        val collisions = mutableListOf<CollisionPair>()
        
        // Get entities with physics components and colliders
        val physicsEntities = entities.filter { 
            it.physicsComponent != null && it.physicsComponent?.collider != null 
        }
        
        // Broad-phase: Check all pairs
        for (i in physicsEntities.indices) {
            for (j in i + 1 until physicsEntities.size) {
                val entity1 = physicsEntities[i]
                val entity2 = physicsEntities[j]
                
                // Skip static-static collisions
                if (entity1.physicsComponent!!.isStatic && entity2.physicsComponent!!.isStatic) {
                    continue
                }
                
                val collider1 = entity1.physicsComponent!!.collider!!
                val collider2 = entity2.physicsComponent!!.collider!!
                
                // Broad-phase: AABB check
                val bounds1 = collider1.getBounds(entity1.position)
                val bounds2 = collider2.getBounds(entity2.position)
                
                if (bounds1.intersects(bounds2)) {
                    // Narrow-phase: Detailed collision check
                    val result = collider1.checkCollision(collider2, entity1.position, entity2.position)
                    
                    if (result?.hasCollision == true) {
                        collisions.add(CollisionPair(entity1, entity2, result))
                    }
                }
            }
        }
        
        return collisions
    }
    
    /**
     * Resolve collisions by adjusting positions and velocities
     */
    fun resolveCollisions(collisions: List<CollisionPair>) {
        for (collision in collisions) {
            resolveCollision(collision)
        }
    }
    
    /**
     * Resolve a single collision between two entities
     */
    private fun resolveCollision(collision: CollisionPair) {
        val entity1 = collision.entity1
        val entity2 = collision.entity2
        val result = collision.result
        
        val physics1 = entity1.physicsComponent!!
        val physics2 = entity2.physicsComponent!!
        val invMass1 = inverseMass(physics1)
        val invMass2 = inverseMass(physics2)
        val invMassSum = invMass1 + invMass2

        if (invMassSum <= 0f) return
        
        // Calculate relative velocity
        val relativeVelocity = physics2.velocity.subtract(physics1.velocity)
        val velocityAlongNormal = relativeVelocity.dot(result.collisionNormal)
        
        // Don't resolve if objects are separating
        if (velocityAlongNormal > 0) return
        
        // Calculate restitution (bounciness)
        // val restitution = (physics1.restitution + physics2.restitution) * 0.5f

        // Calculate impulse scalar (bounce disabled)
        val impulseScalar = -velocityAlongNormal / invMassSum
        
        // Apply impulse
        val impulse = result.collisionNormal.scale(impulseScalar)
        
        if (!physics1.isStatic && !physics1.isKinematic) {
            physics1.velocity = physics1.velocity.subtract(impulse.scale(1/physics1.mass))
        }
        
        if (!physics2.isStatic && !physics2.isKinematic) {
            physics2.velocity = physics2.velocity.add(impulse.scale(1/physics2.mass))
        }
        
        // Position correction to prevent sinking
        val correctionPercent = 0.8f  // Usually 80%
        val correctionSlop = 0.01f    // Usually 1cm
        
        val correctionMagnitude = maxOf(result.penetrationDepth - correctionSlop, 0f) /
                                invMassSum * correctionPercent
        
        val correction = result.collisionNormal.scale(correctionMagnitude)
        
        if (!physics1.isStatic && !physics1.isKinematic) {
            entity1.position = entity1.position.subtract(correction.scale(invMass1))
        }
        
        if (!physics2.isStatic && !physics2.isKinematic) {
            entity2.position = entity2.position.add(correction.scale(invMass2))
        }
    }

    private fun inverseMass(physics: qorrnsmj.smf.physics.PhysicsComponent): Float {
        if (physics.isStatic || physics.isKinematic) return 0f
        if (physics.mass <= 0f) return 0f
        return 1f / physics.mass
    }
}

/**
 * Represents a collision between two entities
 */
data class CollisionPair(
    val entity1: Entity,
    val entity2: Entity,
    val result: CollisionResult
)
