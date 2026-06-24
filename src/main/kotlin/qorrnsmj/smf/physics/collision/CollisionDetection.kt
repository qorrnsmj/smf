package qorrnsmj.smf.physics.collision

import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.Collidable
import qorrnsmj.smf.physics.collision.data.CollisionPair
import qorrnsmj.smf.physics.component.IPhysicsComponent
import qorrnsmj.smf.physics.component.StaticPhysics

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

        // Get entities that have colliders
        val physicsEntities = entities.filter { it.physicsComponent.collider != null && it !is Collidable }

        // Broad-phase: Check all pairs
        for (i in physicsEntities.indices) {
            for (j in i + 1 until physicsEntities.size) {
                val entity1 = physicsEntities[i]
                val entity2 = physicsEntities[j]

                // Skip static-static collisions
                if (entity1.physicsComponent is StaticPhysics && entity2.physicsComponent is StaticPhysics) {
                    continue
                }

                // Skip parent-child and sibling collisions
                if (entity1.parent == entity2 || entity2.parent == entity1) {
                    continue
                }
                if (entity1.parent != null && entity1.parent == entity2.parent) {
                    continue
                }

                val collider1 = entity1.physicsComponent.collider!!
                val collider2 = entity2.physicsComponent.collider!!

                // Broad-phase: AABB check
                val position1 = entity1.worldTransform.position
                val position2 = entity2.worldTransform.position
                val bounds1 = collider1.getBounds(position1)
                val bounds2 = collider2.getBounds(position2)

                if (bounds1.intersects(bounds2)) {
                    // Narrow-phase: Detailed collision check
                    val result = collider1.checkCollision(collider2, position1, position2)
                    result?.let { collisions.add(CollisionPair(entity1, entity2, it)) }
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

        val physics1 = entity1.physicsComponent
        val physics2 = entity2.physicsComponent
        val invMass1 = inverseMass(physics1)
        val invMass2 = inverseMass(physics2)
        val invMassSum = invMass1 + invMass2

        if (invMassSum <= 0f) return

        // Resolve overlap first so objects do not remain interpenetrated.
        val correctionPercent = 1.0f
        val correctionSlop = 0f
        val skin = 0.001f
        val correctionMagnitude = (maxOf(result.penetrationDepth - correctionSlop, 0f) + skin) /
            invMassSum * correctionPercent
        val correction = result.collisionNormal.scale(correctionMagnitude)

        if (physics1 !is StaticPhysics) {
            entity1.localTransform = entity1.localTransform.copy(position = entity1.localTransform.position.subtract(correction.scale(invMass1)))
        }

        if (physics2 !is StaticPhysics) {
            entity2.localTransform = entity2.localTransform.copy(position = entity2.localTransform.position.add(correction.scale(invMass2)))
        }

        // Calculate relative velocity
        val relativeVelocity = physics2.velocity.subtract(physics1.velocity)
        val velocityAlongNormal = relativeVelocity.dot(result.collisionNormal)

        // Skip impulse when objects are already separating.
        if (velocityAlongNormal > 0) return

        // Calculate impulse scalar (bounce disabled)
        val impulseScalar = -velocityAlongNormal / invMassSum

        // Apply impulse
        val impulse = result.collisionNormal.scale(impulseScalar)

        if (physics1 !is StaticPhysics) {
            physics1.velocity = physics1.velocity.subtract(impulse.scale(invMass1))
        }

        if (physics2 !is StaticPhysics) {
            physics2.velocity = physics2.velocity.add(impulse.scale(invMass2))
        }
    }

    private fun inverseMass(physics: IPhysicsComponent): Float {
        if (physics is StaticPhysics) return 0f
        return 1f
    }
}
