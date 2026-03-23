package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent

class StallEntity : Entity() {
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
        children.addAll(listOf(
            cover, fruits,
            glass1, glass2, glass3,
            woodPole1, woodPole2, woodTable, woodTray
        ))
        
        // Add collision to stall (static object, no gravity)
        physics = PhysicsComponent(
            useGravity = false,
            collisionBounds = Vector3f(2f, 2f, 2f)
        )
    }

    // TODO: children.foreachでやるのって良くなくない？
    fun move() {
        children.forEach { it.rotation.y += 0.2f }
    }
}
