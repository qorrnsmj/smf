package qorrnsmj.smf.graphic.scene.terrain

interface HeightProvider {
    fun getHeight(worldX: Float, worldZ: Float): Float
}
