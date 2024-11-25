package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.Models
import qorrnsmj.smf.game.entity.component.Model

class Stall : Entity() {
    override fun getModels(): List<Model> {
        return Models.STALL
    }
}