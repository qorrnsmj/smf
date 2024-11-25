package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.Models
import qorrnsmj.smf.game.entity.component.Model

class Ship : Entity() {
    override fun getModels(): List<Model> {
        return Models.SHIP
    }
}