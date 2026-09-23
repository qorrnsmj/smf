package qorrnsmj.smf.game.weather

import qorrnsmj.smf.math.Vector3f

data class WeatherPreset(
    val name: String,
    val horizonColor: Vector3f,
    val zenithColor: Vector3f,
    val cloudColor: Vector3f,
    val cloudCoverage: Float,
    val directionalLightColor: Vector3f,
    val directionalLightIntensity: Float,
    val directionalShadowStrength: Float,
    val sunGlowColor: Vector3f,
    val ambientColor: Vector3f,
    val ambientIntensity: Float,
    val fogColor: Vector3f,
    val fogDistanceDensity: Float,
)

object WeatherPresets {
    val MORNING = WeatherPreset(
        name = "Morning",
        horizonColor = Vector3f(0.20f, 0.24f, 0.44f),
        zenithColor = Vector3f(0.10f, 0.18f, 0.42f),
        cloudColor = Vector3f(1f, 0.86f, 0.72f),
        cloudCoverage = 0.42f,
        directionalLightColor = Vector3f(1f, 0.62f, 0.34f),
        directionalLightIntensity = 0.15f,
        directionalShadowStrength = 0.20f,
        sunGlowColor = Vector3f(1f, 0.34f, 0.12f),
        ambientColor = Vector3f(0.48f, 0.52f, 0.62f),
        ambientIntensity = 0.28f,
        fogColor = Vector3f(0.82f, 0.64f, 0.50f),
        fogDistanceDensity = 0.00010f,
    )

    val NOON = WeatherPreset(
        name = "Noon",
        horizonColor = Vector3f(0.56f, 0.75f, 0.95f),
        zenithColor = Vector3f(0.10f, 0.36f, 0.82f),
        cloudColor = Vector3f(1f, 0.98f, 0.92f),
        cloudCoverage = 0.34f,
        directionalLightColor = Vector3f(1f, 0.95f, 0.82f),
        directionalLightIntensity = 2.3f,
        directionalShadowStrength = 0.68f,
        sunGlowColor = Vector3f(1f, 0.88f, 0.62f),
        ambientColor = Vector3f(0.48f, 0.56f, 0.68f),
        ambientIntensity = 0.25f,
        fogColor = Vector3f(0.62f, 0.76f, 0.92f),
        fogDistanceDensity = 0.00008f,
    )

    val EVENING = WeatherPreset(
        name = "Evening",
        horizonColor = Vector3f(0.12f, 0.13f, 0.30f),
        zenithColor = Vector3f(0.055f, 0.05f, 0.18f),
        cloudColor = Vector3f(0.68f, 0.34f, 0.42f),
        cloudCoverage = 0.36f,
        directionalLightColor = Vector3f(1f, 0.42f, 0.20f),
        directionalLightIntensity = 0.15f,
        directionalShadowStrength = 0.20f,
        sunGlowColor = Vector3f(1f, 0.20f, 0.08f),
        ambientColor = Vector3f(0.34f, 0.24f, 0.42f),
        ambientIntensity = 0.22f,
        fogColor = Vector3f(0.44f, 0.22f, 0.34f),
        fogDistanceDensity = 0.00016f,
    )

    val NIGHT = WeatherPreset(
        name = "Night",
        horizonColor = Vector3f(0.07f, 0.08f, 0.18f),
        zenithColor = Vector3f(0.015f, 0.025f, 0.09f),
        cloudColor = Vector3f(0.16f, 0.14f, 0.25f),
        cloudCoverage = 0.30f,
        directionalLightColor = Vector3f(0.48f, 0.62f, 1f),
        directionalLightIntensity = 0.28f,
        directionalShadowStrength = 0.24f,
        sunGlowColor = Vector3f(0.42f, 0.20f, 0.14f),
        ambientColor = Vector3f(0.12f, 0.16f, 0.30f),
        ambientIntensity = 0.13f,
        fogColor = Vector3f(0.07f, 0.08f, 0.16f),
        fogDistanceDensity = 0.00020f,
    )
}
