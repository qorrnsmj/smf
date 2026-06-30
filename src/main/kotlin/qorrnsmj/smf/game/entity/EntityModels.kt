package qorrnsmj.smf.game.entity

import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.Material
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.texture.Textures

// TODO: Entities
object EntityModels {
    lateinit var EMPTY: Model

    lateinit var STALL: Map<String, Model>
    private var coreLoaded: Boolean = false
    private var stallLoaded: Boolean = false

    fun loadResident() {
        loadCore()
        loadStall()
    }

    fun loadCore() {
        if (coreLoaded) return

        EMPTY = Model(Mesh(), Material(
            baseColorTexture = Textures.DEFAULT_000000,
            metallicRoughnessTexture = Textures.DEFAULT_00FF00,
            normalTexture = Textures.DEFAULT_8080FF,
            occlusionTexture = Textures.DEFAULT_FFFFFF,
            emissiveTexture = Textures.DEFAULT_000000,
        ))
        coreLoaded = true
    }

    fun loadStageModels(modelFiles: List<String>) {
        loadCore()

        modelFiles.forEach { modelFile ->
            when (modelFile) {
                "stall.glb", "stall" -> loadStall()
                else -> Unit
            }
        }
    }

    fun loadStall() {
        loadCore()
        if (stallLoaded) return

        STALL = EntityLoader.loadModel("stall.glb")
        stallLoaded = true
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key]!!
    }
}
