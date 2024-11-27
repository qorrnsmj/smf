package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.model.Models
import qorrnsmj.smf.game.entity.model.Models.STALL

class StallEntity : Entity() {
    val cover = Entity(model = Models.getModel(STALL, "Cover"))
    val fruits = Entity(model = Models.getModel(STALL, "Fruits"))
    val glass1 = Entity(model = Models.getModel(STALL, "Glass1"))
    val glass2 = Entity(model = Models.getModel(STALL, "Glass2"))
    val glass3 = Entity(model = Models.getModel(STALL, "Glass3"))
    val woodPole1 = Entity(model = Models.getModel(STALL, "WoodPole1"))
    val woodPole2 = Entity(model = Models.getModel(STALL, "WoodPole2"))
    val woodTable = Entity(model = Models.getModel(STALL, "WoodTable"))
    val woodTray = Entity(model = Models.getModel(STALL, "WoodTray"))

    init {
        children.addAll(listOf(
            cover, fruits,
            glass1, glass2, glass3,
            woodPole1, woodPole2, woodTable, woodTray
        ))
    }

    fun move() {
        children.forEach { it.rotation.y += 0.2f }
    }
}
