package qorrnsmj.smf.physics

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.custom.Collidable
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.KinematicPhysics
import qorrnsmj.smf.physics.component.StaticPhysics
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.CollisionDetection
import qorrnsmj.smf.physics.collision.shape.SphereCollider

/**
 * Main physics simulation manager for the SMF engine
 * Handles physics updates, collision detection, and force calculations
 * Follows SMF's singleton pattern like AudioManager
 */
object PhysicsWorld {
    // Physics constants
    const val GRAVITY_STRENGTH = 9.8f
    const val GRAVITY_EFFECT_SCALE = 0.01f
    val GRAVITY_VECTOR = Vector3f(0f, -GRAVITY_STRENGTH, 0f)

    private const val COLLISION_SKIN = 0.001f
    private const val HORIZONTAL_PRIORITY_BIAS = 0.01f
    private const val PENETRATION_EPSILON = 0.0005f

    // Performance metrics
    private var lastUpdateTime = 0L
    private var physicsUpdateCount = 0L

    /**
     * Update the physics simulation
     */
    fun update(entities: List<Entity>, heightProvider: HeightProvider?, delta: Float) {
        val startTime = System.nanoTime()
        
        if (entities.isNotEmpty()) {
            resetGroundedState(entities)

            applyForces(entities)
            
            integrateMotion(entities, delta)
            
            handleEntityCollisions(entities)

            resolveCollidableEntityCollisions(entities)
            
            if (heightProvider != null) {
                handleTerrainCollisions(entities, heightProvider)
            }
            
            clearForces(entities)
        }
        
        // Update performance metrics
        lastUpdateTime = System.nanoTime() - startTime
        physicsUpdateCount++
    }

    private fun resetGroundedState(entities: List<Entity>) {
        for (entity in entities) {
            val physics = entity.physicsComponent
            if (physics !is StaticPhysics) {
                physics.isGrounded = false
            }
        }
    }
    
    /**
     * Apply forces to all physics entities
     */
    private fun applyForces(entities: List<Entity>) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            if (physics !is DynamicPhysics) continue

            // Apply gravity
            physics.applyForce(GRAVITY_VECTOR.scale(physics.mass * GRAVITY_EFFECT_SCALE))
            
            // Drag is disabled in simple physics mode.
            // if (physics.drag > 0f && physics.velocity.length() > 0f) {
            //     val dragForce = physics.velocity.normalize().scale(-physics.drag * physics.velocity.length() * physics.velocity.length())
            //     physics.applyForce(dragForce)
            // }
        }
    }
    
    /**
     * Integrate motion using semi-implicit Euler method
     * Now supports hierarchical physics propagation
     */
    private fun integrateMotion(entities: List<Entity>, delta: Float) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            when (physics) {
                is StaticPhysics -> continue

                is DynamicPhysics -> {
                    // Calculate acceleration from forces
                    physics.calculateAcceleration()

                    // Update velocity: v = v + a * dt
                    physics.velocity = physics.velocity.add(physics.acceleration.scale(delta))

                    // Update position: p = p + v * dt (root entities only)
                    if (entity.parent == null) {
                        val newPosition = entity.localTransform.position.add(physics.velocity.scale(delta))
                        entity.localTransform = entity.localTransform.copy(position = newPosition)

                        // Propagate physics result to all children
                        propagatePhysicsToChildren(entity, physics.velocity, delta)
                    }
                }

                is KinematicPhysics -> {
                    if (entity.parent == null) {
                        val newPosition = entity.localTransform.position.add(physics.velocity.scale(delta))
                        entity.localTransform = entity.localTransform.copy(position = newPosition)
                        propagatePhysicsToChildren(entity, physics.velocity, delta)
                    }
                }
            }
            
            // Angular motion is disabled in simple physics mode.
            // entity.rotation = entity.rotation.add(physics.angularVelocity.scale(delta))
        }
    }
    
    /**
     * Propagate parent's physics motion to all child entities recursively
     */
    private fun propagatePhysicsToChildren(parent: Entity, parentVelocity: Vector3f, delta: Float) {
        for (child in parent.children) {
            val childPhysics = child.physicsComponent

            if (childPhysics !is StaticPhysics) {
                // Child inherits parent velocity plus its own relative motion.
                val inheritedVelocity = parentVelocity.add(childPhysics.velocity)

                // Update child's world position based on inherited motion
                val childWorldPos = child.worldTransform.position
                val newChildWorldPos = childWorldPos.add(inheritedVelocity.scale(delta))
                val parentWorld = child.parent?.worldTransform
                val newLocalPos = if (parentWorld != null) {
                    newChildWorldPos.subtract(parentWorld.position).divide(parentWorld.scale)
                } else {
                    newChildWorldPos
                }
                child.localTransform = child.localTransform.copy(position = newLocalPos)
            }

            // Recursively propagate to grandchildren
            propagatePhysicsToChildren(child, parentVelocity, delta)
        }
    }
    
    /**
     * Handle entity-entity collisions
     */
    private fun handleEntityCollisions(entities: List<Entity>) {
        val collisions = CollisionDetection.detectCollisions(entities)
        CollisionDetection.resolveCollisions(collisions)
    }
    
    /**
     * Handle terrain collisions for all entities
     */
    private fun handleTerrainCollisions(entities: List<Entity>, heightProvider: HeightProvider) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            if (physics is StaticPhysics) continue

            val entityWorldPos = entity.worldTransform.position
            val groundHeight = heightProvider.getHeight(entityWorldPos.x, entityWorldPos.z)
            
            // Check if entity is below ground
            if (entityWorldPos.y <= groundHeight) {
                // Position correction
                entity.localTransform = entity.localTransform.copy(position = Vector3f(entityWorldPos.x, groundHeight, entityWorldPos.z))
                
                // Ground contact response
                physics.isGrounded = true
                
                // Ground stop only in simple physics mode.
                if (physics.velocity.y < 0f) {
                    physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                }
            }
        }
    }

    private fun resolveCollidableEntityCollisions(entities: List<Entity>) {
        for (collidable in entities.filterIsInstance<Collidable>()) {
            val entity = collidable as Entity
            val physics = entity.physicsComponent
            if (physics is StaticPhysics) continue

            var groundedByCollider = false

            repeat(3) {
                var corrected = false
                var bounds = collidable.getAabb()

                for (obstacle in entities) {
                    if (obstacle === entity) continue

                    val collider = obstacle.physicsComponent.collider ?: continue
                    val obstaclePosition = obstacle.worldTransform.position

                    val correction = when (collider) {
                        is BoxCollider -> computeBoxCorrection(bounds, collider.getBounds(obstaclePosition))
                        is SphereCollider -> computeSphereCorrection(bounds, collider.getCenter(obstaclePosition), collider.radius)
                        else -> null
                    }

                    if (correction != null) {
                        entity.localTransform = entity.localTransform.copy(position = entity.localTransform.position.add(correction))
                        corrected = true
                        bounds = collidable.getAabb()

                        if (correction.y > 0f) {
                            groundedByCollider = true
                            if (physics.velocity.y < 0f) {
                                physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                            }
                        }
                    }
                }

                if (!corrected) return@repeat
            }

            if (groundedByCollider || hasGroundSupport(entity, collidable, entities)) {
                physics.isGrounded = true
            }
        }
    }

    private fun hasGroundSupport(entity: Entity, collidable: Collidable, entities: List<Entity>): Boolean {
        val probeBounds = collidable.getAabbWithFeetOffset(-collidable.groundProbeDistance)

        for (obstacle in entities) {
            if (obstacle === entity) continue

            val collider = obstacle.physicsComponent.collider ?: continue
            val obstaclePosition = obstacle.worldTransform.position

            val correction = when (collider) {
                is BoxCollider -> computeBoxCorrection(probeBounds, collider.getBounds(obstaclePosition))
                is SphereCollider -> computeSphereCorrection(probeBounds, collider.getCenter(obstaclePosition), collider.radius)
                else -> null
            }

            if (correction != null && correction.y > 0f) {
                return true
            }
        }

        return false
    }

    private fun computeBoxCorrection(entityBounds: AABB, obstacleBounds: AABB): Vector3f? {
        if (!entityBounds.intersects(obstacleBounds)) {
            return null
        }

        val overlapX = minOf(entityBounds.max.x - obstacleBounds.min.x, obstacleBounds.max.x - entityBounds.min.x)
        val overlapY = minOf(entityBounds.max.y - obstacleBounds.min.y, obstacleBounds.max.y - entityBounds.min.y)
        val overlapZ = minOf(entityBounds.max.z - obstacleBounds.min.z, obstacleBounds.max.z - entityBounds.min.z)

        if (overlapX <= 0f || overlapY <= 0f || overlapZ <= 0f) {
            return null
        }

        val minOverlap = minOf(overlapX, minOf(overlapY, overlapZ))
        if (minOverlap <= PENETRATION_EPSILON) {
            return null
        }

        val center = Vector3f(
            (entityBounds.min.x + entityBounds.max.x) * 0.5f,
            (entityBounds.min.y + entityBounds.max.y) * 0.5f,
            (entityBounds.min.z + entityBounds.max.z) * 0.5f
        )
        val obstacleCenter = Vector3f(
            (obstacleBounds.min.x + obstacleBounds.max.x) * 0.5f,
            (obstacleBounds.min.y + obstacleBounds.max.y) * 0.5f,
            (obstacleBounds.min.z + obstacleBounds.max.z) * 0.5f
        )

        val minHorizontalOverlap = minOf(overlapX, overlapZ)
        val resolveHorizontally = minHorizontalOverlap <= overlapY + HORIZONTAL_PRIORITY_BIAS

        return if (resolveHorizontally) {
            if (overlapX <= overlapZ) {
                Vector3f(if (center.x >= obstacleCenter.x) 1f else -1f, 0f, 0f).scale(overlapX + COLLISION_SKIN)
            } else {
                Vector3f(0f, 0f, if (center.z >= obstacleCenter.z) 1f else -1f).scale(overlapZ + COLLISION_SKIN)
            }
        } else {
            Vector3f(0f, if (center.y >= obstacleCenter.y) 1f else -1f, 0f).scale(overlapY + COLLISION_SKIN)
        }
    }

    private fun computeSphereCorrection(entityBounds: AABB, sphereCenter: Vector3f, sphereRadius: Float): Vector3f? {
        val closest = Vector3f(
            clamp(sphereCenter.x, entityBounds.min.x, entityBounds.max.x),
            clamp(sphereCenter.y, entityBounds.min.y, entityBounds.max.y),
            clamp(sphereCenter.z, entityBounds.min.z, entityBounds.max.z)
        )

        val direction = closest.subtract(sphereCenter)
        val distance = direction.length()
        if (distance >= sphereRadius) {
            return null
        }

        val penetration = sphereRadius - distance
        val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
        return normal.scale(penetration + COLLISION_SKIN)
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * Clear forces from all physics entities
     */
    private fun clearForces(entities: List<Entity>) {
        entities.forEach { entity ->
            val physics = entity.physicsComponent
            if (physics is DynamicPhysics) {
                physics.clearForces()
            }
        }
    }
    
    /**
     * Get physics performance statistics
     */
    fun getStats(): PhysicsStats {
        return PhysicsStats(
            updateCount = physicsUpdateCount,
            lastUpdateTimeNs = lastUpdateTime
        )
    }

    /**
     * Data class for physics performance statistics
     */
    data class PhysicsStats(
        val updateCount: Long,
        val lastUpdateTimeNs: Long
    ) {
        val lastUpdateTimeMs: Float get() = lastUpdateTimeNs / 1_000_000f
    }
}
