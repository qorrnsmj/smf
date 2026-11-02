package qorrnsmj.smf.game.level.custom.meadow

import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f

internal data class MeadowStyle(
    val displayName: String,
    val night: Boolean,
    val sunDirection: Vector3f,
    val horizonColor: Vector3f,
    val zenithColor: Vector3f,
    val cloudColor: Vector3f,
    val lightColor: Vector3f,
    val lightIntensity: Float,
    val ambientColor: Vector3f,
    val ambientIntensity: Float,
    val shadowStrength: Float,
    val fogColor: Vector3f,
    val fogDensity: Float,
    val grassSeed: Int,
    val windDirection: Vector2f,
    val windSpeed: Float,
    val createEffects: () -> List<Effect>,
)
