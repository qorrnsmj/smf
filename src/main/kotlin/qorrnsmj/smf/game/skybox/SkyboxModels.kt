package qorrnsmj.smf.game.skybox

import qorrnsmj.smf.game.entity.model.component.Material
import qorrnsmj.smf.game.entity.model.component.Mesh
import qorrnsmj.smf.game.entity.model.component.Model

object SkyboxModels {
    lateinit var SKY1: Model
    lateinit var NONE: Model

    fun load() {
        SKY1 = SkyboxLoader.loadModel("sky1")
        NONE = Model("none", Mesh(), Material())
    }
}
