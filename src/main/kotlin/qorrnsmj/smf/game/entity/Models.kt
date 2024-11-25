package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.entity.component.Model

object Models {
    lateinit var TREE: List<Model>
    lateinit var STALL: List<Model>
    lateinit var SHIP: List<Model>
    lateinit var NORM_CUBE: List<Model>

    fun load() {
        TREE = loadModel("tree.obj")
        STALL = loadModel("stall.fbx")
        SHIP = loadModel("ship.obj")
        NORM_CUBE = loadModel("cube.fbx")
    }

    private fun loadModel(model: String): List<Model> {
        return Loader.loadModel(model)
    }
}
