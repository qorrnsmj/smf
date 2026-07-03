package qorrnsmj.smf.graphic.light

import qorrnsmj.smf.math.Vector3f

class PointLight(
    position: Vector3f = Vector3f(),
    ambient: Vector3f = Vector3f(),
    diffuse: Vector3f = Vector3f(),
    specular: Vector3f = Vector3f(),
    shininess: Float = 32.0f,
    intensity: Float = 1.0f,
    constant: Float = 1.0f,
    linear: Float = 0.09f,
    quadratic: Float = 0.032f,
    var castsShadow: Boolean = false,
    var shadowStrength: Float = 0.55f
) : Light(position, ambient, diffuse, specular, shininess, intensity, constant, linear, quadratic)
