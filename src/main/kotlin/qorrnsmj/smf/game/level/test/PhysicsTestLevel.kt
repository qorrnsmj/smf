package qorrnsmj.smf.game.level.test

import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.Terrains
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent

/**
 * Test level for physics: gravity and collision detection
 */
class PhysicsTestLevel : Level() {
    
    override fun load() {
        // Setup player
        player = Player().apply {
            camera.position = Vector3f(0f, 50f, 10f)
        }
        scene.camera = player.camera
        
        // Create falling boxes to test gravity
        createFallingBox(Vector3f(5f, 30f, 0f))
        createFallingBox(Vector3f(-5f, 40f, 0f))
        createFallingBox(Vector3f(0f, 35f, -5f))
        
        // Create static walls/obstacles to test collision
        createWall(Vector3f(0f, 2f, -10f), Vector3f(5f, 2f, 1f))
        createWall(Vector3f(10f, 2f, 0f), Vector3f(1f, 2f, 5f))
        createWall(Vector3f(-10f, 2f, 0f), Vector3f(1f, 2f, 5f))
        
        // Create a platform
        createPlatform(Vector3f(0f, 10f, 0f), Vector3f(3f, 0.5f, 3f))
        
        // Setup lighting
        scene.lights.add(PointLight().apply {
            position = Vector3f(0f, 50f, 0f)
            ambient = Vector3f(0.3f, 0.3f, 0.3f)
            diffuse = Vector3f(1f, 1f, 1f)
            specular = Vector3f(1f, 1f, 1f)
            constant = 1f
            linear = 0.01f
            quadratic = 0.001f
        })
        
        // Setup environment
        scene.terrain = Terrains.PLANE
        scene.skybox = Skyboxes.SKY1
    }
    
    override fun input(delta: Float) {
        player.handleInput(SMF.window, delta)
    }
    
    override fun update(delta: Float) {
        player.update(delta, scene.terrain)
        // Physics is updated in parent Level.update()
        super.update(delta)
    }
    
    /**
     * Create a box that falls with gravity
     */
    private fun createFallingBox(position: Vector3f) {
        val box = Entity(
            position = position,
            scale = Vector3f(1f, 1f, 1f),
            model = EntityModels.EMPTY,
            physics = PhysicsComponent(
                velocity = Vector3f(0f, 0f, 0f),
                gravity = 0.15f,
                useGravity = true,
                collisionBounds = Vector3f(0.5f, 0.5f, 0.5f)
            )
        )
        scene.entities.add(box)
    }
    
    /**
     * Create a static wall (no gravity)
     */
    private fun createWall(position: Vector3f, size: Vector3f) {
        val wall = Entity(
            position = position,
            scale = size,
            model = EntityModels.EMPTY,
            physics = PhysicsComponent(
                velocity = Vector3f(0f, 0f, 0f),
                useGravity = false,
                collisionBounds = Vector3f(1f, 1f, 1f)
            )
        )
        scene.entities.add(wall)
    }
    
    /**
     * Create a platform (no gravity)
     */
    private fun createPlatform(position: Vector3f, size: Vector3f) {
        val platform = Entity(
            position = position,
            scale = size,
            model = EntityModels.EMPTY,
            physics = PhysicsComponent(
                velocity = Vector3f(0f, 0f, 0f),
                useGravity = false,
                collisionBounds = Vector3f(1f, 1f, 1f)
            )
        )
        scene.entities.add(platform)
    }
    
    override fun unload() {
        scene.entities.clear()
        scene.lights.clear()
    }
}
