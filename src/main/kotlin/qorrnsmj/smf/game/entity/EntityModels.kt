package qorrnsmj.smf.game.entity

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.Material
import qorrnsmj.smf.game.entity.model.Mesh
import qorrnsmj.smf.game.entity.model.Model
import qorrnsmj.smf.game.entity.model.Texture

object EntityModels {
    lateinit var EMPTY: Model
    lateinit var STALL: Map<String, Model>
    lateinit var NORM_CUBE: Map<String, Model>

    fun load() {
        EMPTY = Model("empty", Mesh(), Material(diffuseTexture = Texture(), specularTexture = Texture(), normalTexture = Texture()))

        STALL = loadModel("stall.fbx")
        NORM_CUBE = loadModel("cube.fbx")
    }

    private fun loadModel(file: String): Map<String, Model> {
        return EntityLoader.loadModel(file)
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key] ?: run {
            Logger.error("Model not found: $key")
            return EMPTY
        }
    }
}
