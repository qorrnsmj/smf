package qorrnsmj.test.t11.game.entity.custom

import qorrnsmj.test.t11.core.model.Model
import qorrnsmj.test.t11.core.model.Models
import qorrnsmj.test.t11.game.entity.Entity

class Ship3 : Entity() {
    override fun getModel(): Model {
        return Models.SHIP3_MODEL
    }
}