package qorrnsmj.smf.game.terrain.custom

data class FlatTerrain(
    private val gridX: Float,
    private val gridZ: Float,
) {
    val x = gridX * SIZE
    val z = gridZ * SIZE

    companion object {
        const val SIZE = 400
        const val VERTEX_COUNT = 128
    }
}
