package qorrnsmj.smf.game.skybox

import qorrnsmj.smf.game.model.component.Material
import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.game.model.component.Model

object Skyboxes {
    lateinit var DEFAULT: Skybox
    lateinit var SKY1: Skybox

    fun load() {
        DEFAULT = Skybox(Model(Mesh(), Material()))
        SKY1 = SkyboxLoader.loadSkybox("sky1")
    }
}
