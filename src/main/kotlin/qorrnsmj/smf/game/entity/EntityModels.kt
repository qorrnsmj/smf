package qorrnsmj.smf.game.entity

import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.Material
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.texture.Textures

// TODO: Entities
object EntityModels {
    lateinit var EMPTY: Model

    lateinit var STALL: Map<String, Model>

    fun load() {
        EMPTY = Model(Mesh(), Material(
            baseColorTexture = Textures.DEFAULT_000000,
            metallicRoughnessTexture = Textures.DEFAULT_00FF00,
            normalTexture = Textures.DEFAULT_8080FF,
            occlusionTexture = Textures.DEFAULT_FFFFFF,
            emissiveTexture = Textures.DEFAULT_000000,
        ))

        STALL = EntityLoader.loadModel("stall.glb")
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key]!!
    }
}
