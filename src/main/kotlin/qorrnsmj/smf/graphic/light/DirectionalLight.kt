package qorrnsmj.smf.graphic.light

import qorrnsmj.smf.math.Vector3f

open class DirectionalLight(
    direction: Vector3f = Vector3f(-0.35f, -1f, -0.25f),
    var color: Vector3f = Vector3f(1f, 0.92f, 0.78f),
    var intensity: Float = 3.2f,
    var ambientColor: Vector3f = Vector3f(0.38f, 0.45f, 0.55f),
    var ambientIntensity: Float = 0.18f,
    var shadowStrength: Float = 0.68f,
) {
    var direction: Vector3f = direction.normalize()
        set(value) {
            field = value.normalize()
        }
}