package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.model.Models
import qorrnsmj.smf.game.entity.model.component.Model

class Tree : Entity() {
    override fun getModel(): Model {
        return Models.TREE
    }
}