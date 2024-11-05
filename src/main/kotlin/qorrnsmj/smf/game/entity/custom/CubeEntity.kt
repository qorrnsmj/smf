package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.math.Vector3f

class CubeEntity : Entity("cube", "cube") {
    init {
        size = Vector3f(1.0f, 1.0f, 1.0f)
    }
}
