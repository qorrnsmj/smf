package qorrnsmj.test.t11.game.entity.custom

import qorrnsmj.test.t11.core.model.Model
import qorrnsmj.test.t11.core.model.Models
import qorrnsmj.test.t11.game.entity.Entity

class Tree : Entity() {
    override fun getModel(): Model {
        return Models.TREE_MODEL
    }
}
