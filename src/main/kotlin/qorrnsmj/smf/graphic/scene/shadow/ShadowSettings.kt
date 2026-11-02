package qorrnsmj.smf.graphic.scene.shadow

data class ShadowSettings(
    val mapSize: Int = 2048,
    val halfSize: Float = 96f,
    val nearPlane: Float = 1f,
    val farPlane: Float = 800f,
    val lightDistance: Float = 350f,
    val targetHeight: Float = 30f,
    val filterRadius: Float = 1f,
    val receiverMinBias: Float = 0.00004f,
    val receiverSlopeBias: Float = 0.00018f,
    val polygonOffsetFactor: Float = 0.1f,
    val polygonOffsetUnits: Float = 0.25f,
) {
    init {
        require(mapSize in 256..8192 && mapSize and (mapSize - 1) == 0) {
            "Shadow map size must be a power of two from 256 to 8192"
        }
        require(halfSize > 0f)
        require(nearPlane > 0f && farPlane > nearPlane)
        require(lightDistance > 0f)
        require(filterRadius in 0.25f..3f)
        require(receiverMinBias >= 0f && receiverSlopeBias >= 0f)
    }
}
