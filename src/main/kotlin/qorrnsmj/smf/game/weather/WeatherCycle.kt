package qorrnsmj.smf.game.weather

import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.scene.SceneEnvironment
import qorrnsmj.smf.math.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WeatherCycle(
    private val environment: SceneEnvironment,
    private val presets: List<WeatherPreset>,
    private val phaseDurationSeconds: Float,
) {
    private var elapsedSeconds = 0f
    private var directionalLightIntensity = 0f
    private var directionalShadowStrength = 0f

    init {
        require(presets.size >= 2) { "Weather cycle requires at least two presets." }
        require(phaseDurationSeconds > 0f) { "Weather phase duration must be positive." }
        applyWeather(presets.first(), presets[1], 0f)
        updateCelestialLighting()
    }

    fun update(deltaSeconds: Float) {
        val totalDuration = phaseDurationSeconds * presets.size
        elapsedSeconds = (elapsedSeconds + deltaSeconds) % totalDuration

        val phase = (elapsedSeconds / phaseDurationSeconds).toInt()
        val nextPhase = (phase + 1) % presets.size
        val linearProgress = (elapsedSeconds % phaseDurationSeconds) / phaseDurationSeconds
        val smoothProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
        applyWeather(presets[phase], presets[nextPhase], smoothProgress)
        updateCelestialLighting()
    }

    private fun applyWeather(from: WeatherPreset, to: WeatherPreset, progress: Float) {
        val horizonColor = from.horizonColor.lerp(to.horizonColor, progress)
        environment.skyColor = horizonColor

        environment.skydome?.apply {
            this.horizonColor = horizonColor
            zenithColor = from.zenithColor.lerp(to.zenithColor, progress)
            cloudColor = from.cloudColor.lerp(to.cloudColor, progress)
            cloudCoverage = lerp(from.cloudCoverage, to.cloudCoverage, progress)
        }

        val light = environment.celestialLight
            ?: DirectionalLight().also { environment.celestialLight = it }
        light.color = from.directionalLightColor.lerp(to.directionalLightColor, progress)
        directionalLightIntensity = lerp(
            from.directionalLightIntensity,
            to.directionalLightIntensity,
            progress,
        )
        directionalShadowStrength = lerp(
            from.directionalShadowStrength,
            to.directionalShadowStrength,
            progress,
        )
        light.ambientColor = from.ambientColor.lerp(to.ambientColor, progress)
        light.ambientIntensity = lerp(from.ambientIntensity, to.ambientIntensity, progress)
        environment.skydome?.sunColor = from.sunGlowColor.lerp(to.sunGlowColor, progress)

        environment.fog.enabled = true
        environment.fog.color = from.fogColor.lerp(to.fogColor, progress)
        environment.fog.distanceDensity = lerp(
            from.fogDistanceDensity,
            to.fogDistanceDensity,
            progress,
        )
    }

    private fun updateCelestialLighting() {
        val totalDuration = phaseDurationSeconds * presets.size
        val angle = elapsedSeconds / totalDuration * (PI.toFloat() * 2f)
        val directionToSun = Vector3f(-cos(angle), sin(angle), 0f)
        val directionToActiveLight = if (directionToSun.y >= 0f) {
            directionToSun
        } else {
            directionToSun.negate()
        }
        val horizonVisibility = smoothstep(0f, 0.18f, kotlin.math.abs(directionToSun.y))
        val shadowVisibility = smoothstep(0.035f, 0.22f, kotlin.math.abs(directionToSun.y))
        val light = environment.celestialLight
            ?: DirectionalLight().also { environment.celestialLight = it }
        light.direction = directionToActiveLight.negate()
        light.intensity = directionalLightIntensity * horizonVisibility
        light.shadowStrength = directionalShadowStrength * shadowVisibility
        environment.skydome?.sunDirection = directionToSun
    }

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val progress = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return progress * progress * (3f - 2f * progress)
    }

    private fun lerp(from: Float, to: Float, progress: Float): Float =
        from + (to - from) * progress
}
