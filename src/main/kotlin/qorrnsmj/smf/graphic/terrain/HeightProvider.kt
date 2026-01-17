package qorrnsmj.smf.graphic.terrain

interface HeightProvider {
    fun getHeight(worldX: Float, worldZ: Float): Float
}
