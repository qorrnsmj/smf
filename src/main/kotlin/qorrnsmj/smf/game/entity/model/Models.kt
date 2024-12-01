package qorrnsmj.smf.game.entity.model

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.component.Material
import qorrnsmj.smf.game.entity.model.component.Mesh
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.game.entity.model.custom.PlaneModel

object Models {
    lateinit var EMPTY: Model
    lateinit var PLANE: Model
    lateinit var STALL: Map<String, Model>
    lateinit var NORM_CUBE: Map<String, Model>

    fun load() {
        EMPTY = Model("empty", Mesh(), Material(diffuseTexture = TextureBufferObject(), specularTexture = TextureBufferObject(), normalTexture = TextureBufferObject()))
        PLANE = PlaneModel("plane", "test_plane.png")

        STALL = loadModel("stall.fbx")
        NORM_CUBE = loadModel("cube.fbx")
    }

    private fun loadModel(file: String): Map<String, Model> {
        return ModelLoader.loadModel(file)
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key] ?: run {
            Logger.error("Model not found: $key")
            return EMPTY
        }
    }
}
