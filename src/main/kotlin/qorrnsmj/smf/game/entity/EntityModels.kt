package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.game.model.component.Material
import qorrnsmj.smf.game.model.component.Model

// TODO: Entities
object EntityModels {
    lateinit var EMPTY: Model

    lateinit var STALL: Map<String, Model>

    fun load() {
        EMPTY = Model(Mesh(), Material())

        STALL = EntityLoader.loadModel("stall.glb")
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key]!!
    }
}
