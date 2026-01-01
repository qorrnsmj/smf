package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.PbrEntity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL

class StallEntity : PbrEntity() {
    val cover = PbrEntity(model = EntityModels.getModel(STALL, "Cover"))
    val fruits = PbrEntity(model = EntityModels.getModel(STALL, "Fruits"))
    val glass1 = PbrEntity(model = EntityModels.getModel(STALL, "Glass1"))
    val glass2 = PbrEntity(model = EntityModels.getModel(STALL, "Glass2"))
    val glass3 = PbrEntity(model = EntityModels.getModel(STALL, "Glass3"))
    val woodPole1 = PbrEntity(model = EntityModels.getModel(STALL, "WoodPole1"))
    val woodPole2 = PbrEntity(model = EntityModels.getModel(STALL, "WoodPole2"))
    val woodTable = PbrEntity(model = EntityModels.getModel(STALL, "WoodTable"))
    val woodTray = PbrEntity(model = EntityModels.getModel(STALL, "WoodTray"))

    init {
        children.addAll(listOf(
            cover, fruits,
            glass1, glass2, glass3,
            woodPole1, woodPole2, woodTable, woodTray
        ))
    }

    // TODO: children.foreachでやるのって良くなくない？
    fun move() {
        children.forEach { it.rotation.y += 0.2f }
    }
}
