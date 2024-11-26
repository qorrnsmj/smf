package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.entity.Models

class Terrain(gridX: Float, gridZ: Float) {
    private val x = gridX * SIZE
    private val z = gridZ * SIZE
    private val model = Models.EMPTY

    companion object {
        const val SIZE = 800
        const val VERTEX_COUNT = 128
    }
}