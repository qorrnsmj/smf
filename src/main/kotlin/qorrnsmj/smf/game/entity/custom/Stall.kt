package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.component.Entity
import qorrnsmj.smf.game.entity.model.Models
import qorrnsmj.smf.game.entity.model.component.Model

class Stall : Entity() {
    override fun getModel(): Model {
        return Models.STALL
    }
}