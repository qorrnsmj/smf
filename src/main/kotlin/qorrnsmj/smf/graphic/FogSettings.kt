package qorrnsmj.smf.graphic

import qorrnsmj.smf.math.Vector3f

data class FogSettings(
    var enabled: Boolean = false,
    var color: Vector3f = Vector3f(0.32f, 0.38f, 0.40f),
    var distanceDensity: Float = 0.00014f,
    var distanceGradient: Float = 1.35f,
    var heightDensity: Float = 0.34f,
    var bottomY: Float = 0f,
    var topY: Float = 280f,
    var heightFalloff: Float = 2.4f,
)
