package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent
import qorrnsmj.smf.physics.collision.BoxCollider

class StallEntity : Entity(
    localPosition = Vector3f(80f, 35f, 80f),  // Start above ground
    physicsComponent = PhysicsComponent(
        mass = 50f,           // Heavy stall
        useGravity = true,    // Enable gravity
        restitution = 0.1f,   // Low bounce
        friction = 0.8f,      // High friction
        drag = 0.05f,         // Some air resistance
        collider = BoxCollider(2f, 3f, 2f)  // Box collider (width, height, depth)
    )
) {
    // Keep imported model-origin local transform (0,0,0) for all stall children.
    val cover = Entity(model = EntityModels.getModel(STALL, "Cover"))
    val fruits = Entity(model = EntityModels.getModel(STALL, "Fruits"))
    val glass1 = Entity(model = EntityModels.getModel(STALL, "Glass1"))
    val glass2 = Entity(model = EntityModels.getModel(STALL, "Glass2"))
    val glass3 = Entity(model = EntityModels.getModel(STALL, "Glass3"))
    val woodPole1 = Entity(model = EntityModels.getModel(STALL, "WoodPole1"))
    val woodPole2 = Entity(model = EntityModels.getModel(STALL, "WoodPole2"))
    val woodTable = Entity(model = EntityModels.getModel(STALL, "WoodTable"))
    val woodTray = Entity(model = EntityModels.getModel(STALL, "WoodTray"))

    init {
        // Use new addChild method for proper parent-child relationship
        addChild(cover)
        addChild(fruits)
        addChild(glass1)
        addChild(glass2)
        addChild(glass3)
        addChild(woodPole1)
        addChild(woodPole2)
        addChild(woodTable)
        addChild(woodTray)
    }

    /**
     * Rotate all child components in their local space
     * This now uses local coordinates instead of direct manipulation
     */
    fun move() {
        // Rotate each child around its local Y axis
        children.forEach { child ->
            val currentRotation = child.localRotation
            child.localRotation = Vector3f(currentRotation.x, currentRotation.y + 0.2f, currentRotation.z)
        }
    }
    
    /**
     * Example: Add cargo to the stall (dynamic entity grouping)
     */
    fun addCargo(cargo: Entity, localPos: Vector3f = Vector3f(0f, 1f, 0f)) {
        cargo.localPosition = localPos
        addChild(cargo)
    }
    
    /**
     * Example: Remove cargo from the stall
     */
    fun removeCargo(cargo: Entity) {
        removeChild(cargo)
    }
}
