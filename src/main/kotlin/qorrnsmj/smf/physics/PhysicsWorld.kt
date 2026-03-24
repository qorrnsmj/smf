package qorrnsmj.smf.physics

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.CollisionDetection

/**
 * Main physics simulation manager for the SMF engine
 * Handles physics updates, collision detection, and force calculations
 * Follows SMF's singleton pattern like AudioManager
 */
object PhysicsWorld {
    
    // Physics constants
    const val GRAVITY_STRENGTH = 9.81f
    val GRAVITY_VECTOR = Vector3f(0f, -GRAVITY_STRENGTH, 0f)
    
    private var isInitialized = false
    private var deltaTime = 0f
    
    // Performance metrics
    private var lastUpdateTime = 0L
    private var physicsUpdateCount = 0L
    
    // Physics entities tracking
    private val physicsEntities = mutableListOf<Entity>()
    
    /**
     * Initialize the physics world
     */
    fun initialize() {
        if (isInitialized) {
            Logger.warn("PhysicsWorld already initialized")
            return
        }
        
        Logger.info("Initializing PhysicsWorld...")
        isInitialized = true
        Logger.info("PhysicsWorld initialized successfully")
    }
    
    /**
     * Update the physics simulation
     */
    fun update(entities: List<Entity>, heightProvider: HeightProvider?, delta: Float) {
        if (!isInitialized) {
            Logger.warn("PhysicsWorld not initialized")
            return
        }
        
        deltaTime = delta
        val startTime = System.nanoTime()
        
        // Filter entities with physics components
        val activePhysicsEntities = entities.filter { it.physicsComponent != null }
        
        if (activePhysicsEntities.isNotEmpty()) {
            // Step 1: Apply forces (gravity, drag, etc.)
            applyForces(activePhysicsEntities)
            
            // Step 2: Integrate motion (velocity and position)
            integrateMotion(activePhysicsEntities, delta)
            
            // Step 3: Handle entity-entity collisions
            handleEntityCollisions(activePhysicsEntities)
            
            // Step 4: Handle terrain collisions
            if (heightProvider != null) {
                handleTerrainCollisions(activePhysicsEntities, heightProvider)
            }
            
            // Step 5: Clear accumulated forces
            clearForces(activePhysicsEntities)
        }
        
        // Update performance metrics
        lastUpdateTime = System.nanoTime() - startTime
        physicsUpdateCount++
    }
    
    /**
     * Apply forces to all physics entities
     */
    private fun applyForces(entities: List<Entity>) {
        for (entity in entities) {
            val physics = entity.physicsComponent ?: continue
            
            // Skip static and kinematic objects for force application
            if (physics.isStatic || physics.isKinematic) continue
            
            // Apply gravity
            if (physics.useGravity) {
                physics.applyForce(GRAVITY_VECTOR.scale(physics.mass))
            }
            
            // Apply drag force (air resistance)
            if (physics.drag > 0f && physics.velocity.length() > 0f) {
                val dragForce = physics.velocity.normalize().scale(-physics.drag * physics.velocity.length() * physics.velocity.length())
                physics.applyForce(dragForce)
            }
        }
    }
    
    /**
     * Integrate motion using semi-implicit Euler method
     */
    private fun integrateMotion(entities: List<Entity>, delta: Float) {
        for (entity in entities) {
            val physics = entity.physicsComponent ?: continue
            
            // Skip static objects
            if (physics.isStatic) continue
            
            // Calculate acceleration from forces
            physics.calculateAcceleration()
            
            // Update velocity: v = v + a * dt
            if (!physics.isKinematic) {
                physics.velocity = physics.velocity.add(physics.acceleration.scale(delta))
            }
            
            // Update position: p = p + v * dt
            entity.position = entity.position.add(physics.velocity.scale(delta))
            
            // Update angular motion (basic rotation)
            entity.rotation = entity.rotation.add(physics.angularVelocity.scale(delta))
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
            val physics = entity.physicsComponent ?: continue
            
            val groundHeight = heightProvider.getHeight(entity.position.x, entity.position.z)
            
            // Check if entity is below ground
            if (entity.position.y <= groundHeight) {
                // Position correction
                entity.position.y = groundHeight
                
                // Ground contact response
                physics.isGrounded = true
                
                // Apply restitution (bounce) if moving downward
                if (physics.velocity.y < 0f) {
                    physics.velocity.y = -physics.velocity.y * physics.restitution
                    
                    // Stop very small bounces to prevent jittering
                    if (kotlin.math.abs(physics.velocity.y) < 0.1f) {
                        physics.velocity.y = 0f
                    }
                }
                
                // Apply friction to horizontal movement
                if (physics.isGrounded) {
                    val horizontalVelocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                    val frictionMagnitude = physics.friction * kotlin.math.min(1f, horizontalVelocity.length())
                    
                    if (horizontalVelocity.length() > 0f) {
                        val frictionForce = horizontalVelocity.normalize().scale(-frictionMagnitude)
                        physics.velocity.x += frictionForce.x * deltaTime
                        physics.velocity.z += frictionForce.z * deltaTime
                    }
                }
            } else {
                physics.isGrounded = false
            }
        }
    }
    
    /**
     * Clear forces from all physics entities
     */
    private fun clearForces(entities: List<Entity>) {
        entities.forEach { it.physicsComponent?.clearForces() }
    }
    
    /**
     * Add an entity to physics simulation tracking
     */
    fun addEntity(entity: Entity) {
        if (entity.physicsComponent != null && !physicsEntities.contains(entity)) {
            physicsEntities.add(entity)
        }
    }
    
    /**
     * Remove an entity from physics simulation
     */
    fun removeEntity(entity: Entity) {
        physicsEntities.remove(entity)
    }
    
    /**
     * Get physics performance statistics
     */
    fun getStats(): PhysicsStats {
        return PhysicsStats(
            updateCount = physicsUpdateCount,
            lastUpdateTimeNs = lastUpdateTime,
            activeEntities = physicsEntities.size,
            isInitialized = isInitialized
        )
    }
    
    /**
     * Cleanup physics world resources
     */
    fun cleanup() {
        Logger.info("Cleaning up PhysicsWorld...")
        physicsEntities.clear()
        isInitialized = false
        physicsUpdateCount = 0L
        lastUpdateTime = 0L
        Logger.info("PhysicsWorld cleaned up")
    }
    
    /**
     * Check if physics world is ready
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Data class for physics performance statistics
     */
    data class PhysicsStats(
        val updateCount: Long,
        val lastUpdateTimeNs: Long,
        val activeEntities: Int,
        val isInitialized: Boolean
    ) {
        val lastUpdateTimeMs: Float get() = lastUpdateTimeNs / 1_000_000f
    }
}