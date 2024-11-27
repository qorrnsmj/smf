package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.entity.model.Model

class Terrain(gridX: Float, gridZ: Float) {
    val x = gridX * SIZE
    val z = gridZ * SIZE
    lateinit var model: Model

    companion object {
        const val SIZE = 800
        const val VERTEX_COUNT = 128
    }
}