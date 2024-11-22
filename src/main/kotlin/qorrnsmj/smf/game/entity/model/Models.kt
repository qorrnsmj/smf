package qorrnsmj.smf.game.entity.model

import qorrnsmj.smf.game.entity.model.component.Model

object Models {
    lateinit var TREE: Model
    lateinit var STALL: Model
    lateinit var SHIP: Model

    fun load() {
        TREE = loadModel("tree.obj", "tree.png")
        STALL = loadModel("stall.obj", "stall.png")
        SHIP = loadModel("ship.obj", "white.png")
    }

    private fun loadModel(model: String, texture: String): Model {
        return Model(Loader.loadMesh(model), Loader.loadTexture(texture))
    }
}
