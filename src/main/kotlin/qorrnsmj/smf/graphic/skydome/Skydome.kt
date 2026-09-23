package qorrnsmj.smf.graphic.skydome

import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.math.Vector3f

data class Skydome(
    var enabled: Boolean = true,
    var cloudBaseTexture: TextureBufferObject? = null,
    var cloudDetailTexture: TextureBufferObject? = null,
    var sunTexture: TextureBufferObject? = null,
    var moonTexture: TextureBufferObject? = null,
    var sunDirection: Vector3f = Vector3f(-1f, 0f, 0f),
    var sunColor: Vector3f = Vector3f(1f, 0.85f, 0.55f),
    var sunAngularSize: Float = 0.14f,
    var sunHorizonGlowStrength: Float = 0.78f,
    var moonColor: Vector3f = Vector3f(0.78f, 0.86f, 1f),
    var moonAngularSize: Float = 0.11f,
    var time: Float = 0f,
    var animationSpeed: Float = 0.012f,
    var cloudScale: Float = 1.75f,
    var cloudCoverage: Float = 0.42f,
    var cloudSoftness: Float = 0.24f,
    var horizonColor: Vector3f = Vector3f(0.58f, 0.74f, 0.96f),
    var zenithColor: Vector3f = Vector3f(0.16f, 0.38f, 0.78f),
    var cloudColor: Vector3f = Vector3f(1f, 0.96f, 0.88f),
) {
    fun update(delta: Float) {
        time += delta * animationSpeed
    }
}
