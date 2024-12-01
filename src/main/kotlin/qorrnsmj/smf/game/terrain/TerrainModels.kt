package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.entity.model.component.Model

object TerrainModels {
    lateinit var TERRAIN: Model

    fun load() {
        TERRAIN = TerrainLoader.loadModel(Terrain(0f, 0f))
    }
}
