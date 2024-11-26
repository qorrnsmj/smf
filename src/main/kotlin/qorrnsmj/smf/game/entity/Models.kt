package qorrnsmj.smf.game.entity

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.component.Material
import qorrnsmj.smf.game.entity.component.Mesh
import qorrnsmj.smf.game.entity.component.Model
import qorrnsmj.smf.game.entity.component.Texture

object Models {
    lateinit var EMPTY: Model
    //lateinit var TREE: Map<String, Model>
    lateinit var STALL: Map<String, Model>
    //lateinit var SHIP: Map<String, Model>
    lateinit var NORM_CUBE: Map<String, Model>

    fun load() {
        EMPTY = Model("empty", Mesh(), Material(diffuseTexture = Texture(), specularTexture = Texture(), normalTexture = Texture()))

        //TREE = loadModel("")
        STALL = loadModel("stall.fbx")
        //SHIP = loadModel("")
        NORM_CUBE = loadModel("cube.fbx")
    }

    private fun loadModel(file: String): Map<String, Model> {
        return Loader.loadModel(file)
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key] ?: run {
            Logger.error("Model not found: $key")
            return EMPTY
        }
    }
}
