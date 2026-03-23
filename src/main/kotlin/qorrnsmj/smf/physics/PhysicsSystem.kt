package qorrnsmj.smf.physics

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f

/**
 * Physics system for handling gravity and collision detection
 */
object PhysicsSystem {
    
    /**
     * Update all entities with physics components
     * @param entities List of entities to update
     * @param player Player instance for collision checks
     * @param delta Delta time for frame-independent physics
     * @param heightProvider Terrain height provider for ground collision
     */
    fun update(entities: List<Entity>, player: Player?, delta: Float, heightProvider: HeightProvider?) {
        // Update entity physics
        for (entity in entities) {
            entity.physics?.let { physics ->
                updateEntityPhysics(entity, physics, delta, heightProvider)
            }
            
            // Recursively update children
            if (entity.children.isNotEmpty()) {
                update(entity.children, player, delta, heightProvider)
            }
        }
        
        // Check collision between player and entities
        player?.let { p ->
            checkPlayerCollisions(p, entities)
        }
    }
    
    /**
     * Update physics for a single entity
     */
    private fun updateEntityPhysics(
        entity: Entity,
        physics: PhysicsComponent,
        delta: Float,
        heightProvider: HeightProvider?
    ) {
        // Apply gravity
        if (physics.useGravity) {
            physics.velocity = physics.velocity - Vector3f(0f, physics.gravity * delta, 0f)
        }
        
        // Update position
        entity.position = entity.position + physics.velocity
        
        // Ground collision
        heightProvider?.let { hp ->
            val groundY = hp.getHeight(entity.position.x, entity.position.z)
            val aabb = physics.getAABB(entity.position, entity.scale)
            val bottomY = aabb.min.y
            
            if (bottomY <= groundY) {
                entity.position.y = groundY + (entity.position.y - bottomY)
                physics.velocity.y = 0f
                physics.grounded = true
            } else {
                physics.grounded = false
            }
        }
    }
    
    /**
     * Check and resolve collisions between player and entities
     */
    private fun checkPlayerCollisions(player: Player, entities: List<Entity>) {
        val playerAABB = getPlayerAABB(player)
        
        for (entity in entities) {
            entity.physics?.let { physics ->
                val entityAABB = physics.getAABB(entity.position, entity.scale)
                
                if (playerAABB.intersects(entityAABB)) {
                    resolveCollision(player, playerAABB, entity, entityAABB)
                }
            }
            
            // Check children
            if (entity.children.isNotEmpty()) {
                checkPlayerCollisions(player, entity.children)
            }
        }
    }
    
    /**
     * Get player AABB based on camera position
     */
    private fun getPlayerAABB(player: Player): AABB {
        val position = player.camera.position
        val halfExtents = Vector3f(0.5f, 1.8f, 0.5f) // Player size
        return AABB.fromCenterAndSize(position, halfExtents)
    }
    
    /**
     * Resolve collision between player and entity
     */
    private fun resolveCollision(
        player: Player,
        playerAABB: AABB,
        entity: Entity,
        entityAABB: AABB
    ) {
        val playerCenter = playerAABB.getCenter()
        val entityCenter = entityAABB.getCenter()
        
        // Calculate overlap on each axis
        val overlapX = (playerAABB.max.x - playerAABB.min.x + entityAABB.max.x - entityAABB.min.x) / 2f - 
                      kotlin.math.abs(playerCenter.x - entityCenter.x)
        val overlapY = (playerAABB.max.y - playerAABB.min.y + entityAABB.max.y - entityAABB.min.y) / 2f - 
                      kotlin.math.abs(playerCenter.y - entityCenter.y)
        val overlapZ = (playerAABB.max.z - playerAABB.min.z + entityAABB.max.z - entityAABB.min.z) / 2f - 
                      kotlin.math.abs(playerCenter.z - entityCenter.z)
        
        // Push player out on the axis with smallest overlap
        if (overlapX < overlapY && overlapX < overlapZ) {
            // Push on X axis
            if (playerCenter.x < entityCenter.x) {
                player.camera.position.x -= overlapX
            } else {
                player.camera.position.x += overlapX
            }
        } else if (overlapY < overlapZ) {
            // Push on Y axis
            if (playerCenter.y < entityCenter.y) {
                player.camera.position.y -= overlapY
            } else {
                player.camera.position.y += overlapY
            }
        } else {
            // Push on Z axis
            if (playerCenter.z < entityCenter.z) {
                player.camera.position.z -= overlapZ
            } else {
                player.camera.position.z += overlapZ
            }
        }
    }
}
