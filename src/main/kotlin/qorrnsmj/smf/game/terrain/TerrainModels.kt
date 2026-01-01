package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.terrain.custom.FlatTerrain

object TerrainModels {
    lateinit var TERRAIN: TerrainModel

    fun load() {
        TERRAIN = TerrainLoader.loadModel(FlatTerrain(0f, 0f))
    }
}
