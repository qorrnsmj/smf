package qorrnsmj.smf.game.terrain

interface HeightProvider {
    fun getHeight(worldX: Float, worldZ: Float): Float
}
