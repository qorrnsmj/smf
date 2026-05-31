package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.collision.shape.BoxCollider

class StallEntity : ObjectEntity(
    transform = Transform(position = Vector3f(90f, 34f, 80f)),  // Start above ground
    physicsComponent = DynamicPhysics(
        mass = 50f,           // Heavy stall
        restitution = 0.1f,   // Low bounce
        friction = 0.8f,      // High friction
        drag = 0.05f,         // Some air resistance
        collider = BoxCollider(10f, 5f, 5f, Vector3f(0f, 2.5f, 1.5f))  // Box collider (width, height, depth)
    )
) {
    // Child entities with local positions relative to stall center
    val cover = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "Cover")
    )
    val fruits = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "Fruits")
    )
    val glass1 = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "Glass1")
    )
    val glass2 = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "Glass2")
    )
    val glass3 = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "Glass3")
    )
    val woodPole1 = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "WoodPole1")
    )
    val woodPole2 = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "WoodPole2")
    )
    val woodTable = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "WoodTable")
    )
    val woodTray = ObjectEntity(
        transform = Transform(position = Vector3f(0f, 0f, 0f)),
        model = EntityModels.getModel(STALL, "WoodTray")
    )

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
}
