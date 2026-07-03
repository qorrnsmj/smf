package qorrnsmj.smf.graphic.light

import qorrnsmj.smf.math.Vector3f

class SunLight(
    direction: Vector3f = Vector3f(-0.35f, -1f, -0.25f),
    color: Vector3f = Vector3f(1f, 0.92f, 0.78f),
    intensity: Float = 3.2f,
    ambientColor: Vector3f = Vector3f(0.38f, 0.45f, 0.55f),
    ambientIntensity: Float = 0.18f,
    shadowStrength: Float = 0.68f,
) : DirectionalLight(
    direction = direction,
    color = color,
    intensity = intensity,
    ambientColor = ambientColor,
    ambientIntensity = ambientIntensity,
    shadowStrength = shadowStrength,
)