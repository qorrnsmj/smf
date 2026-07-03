package qorrnsmj.smf.graphic.light

import qorrnsmj.smf.math.Vector3f

class SpotLight(
    position: Vector3f = Vector3f(),
    ambient: Vector3f = Vector3f(),
    diffuse: Vector3f = Vector3f(),
    specular: Vector3f = Vector3f(),
    shininess: Float = 32.0f,
    intensity: Float = 1.0f,
    constant: Float = 1.0f,
    linear: Float = 0.09f,
    quadratic: Float = 0.032f,
    direction: Vector3f = Vector3f(0f, -1f, 0f),
    var innerCutOffDegrees: Float = 16f,
    var outerCutOffDegrees: Float = 24f,
    var castsShadow: Boolean = false,
    var shadowStrength: Float = 0.65f,
) : Light(position, ambient, diffuse, specular, shininess, intensity, constant, linear, quadratic) {
    var direction: Vector3f = direction.normalize()
        set(value) {
            field = value.normalize()
        }

    val innerCutOff: Float
        get() = kotlin.math.cos(Math.toRadians(innerCutOffDegrees.toDouble())).toFloat()

    val outerCutOff: Float
        get() = kotlin.math.cos(Math.toRadians(outerCutOffDegrees.toDouble())).toFloat()
}