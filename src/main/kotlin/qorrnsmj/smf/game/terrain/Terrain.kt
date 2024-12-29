package qorrnsmj.smf.game.terrain

data class Terrain(
    val gridX: Float,
    val gridZ: Float
) {
    val x = gridX * SIZE
    val z = gridZ * SIZE

    companion object {
        const val SIZE = 800
        const val VERTEX_COUNT = 128
    }
}
