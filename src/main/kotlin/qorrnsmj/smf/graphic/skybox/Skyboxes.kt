package qorrnsmj.smf.graphic.skybox

import qorrnsmj.smf.graphic.`object`.Material
import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.texture.Textures

object Skyboxes {
    lateinit var DEFAULT: Skybox
    lateinit var SKY1: Skybox

    fun load() {
        DEFAULT = Skybox(Model(Mesh(), Material(
            baseColorTexture = Textures.DEFAULT_000000,
            metallicRoughnessTexture = Textures.DEFAULT_00FF00,
            normalTexture = Textures.DEFAULT_8080FF,
            occlusionTexture = Textures.DEFAULT_FFFFFF,
            emissiveTexture = Textures.DEFAULT_000000,
        )))
        SKY1 = SkyboxLoader.loadSkybox("sky1")
    }
}
