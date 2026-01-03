package qorrnsmj.smf.game.skybox

import qorrnsmj.smf.game.model.component.Material
import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.game.model.component.Model

// TODO: Skyboxes
object Skyboxes {
    lateinit var NONE: Skybox
    lateinit var SKY1: Skybox

    fun load() {
        NONE = Skybox(Model(Mesh(), Material()))
        SKY1 = SkyboxLoader.loadSkybox("sky1")
    }
}
