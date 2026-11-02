package qorrnsmj.smf.game.level.custom.meadow
import qorrnsmj.smf.graphic.scene.terrain.Terrain

import qorrnsmj.smf.graphic.effect.ColorEffect
import qorrnsmj.smf.graphic.effect.ContrastEffect
import qorrnsmj.smf.graphic.scene.terrain.Terrains
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f

internal class MoonveilMeadowLevel : MeadowLevel(
    style = STYLE,
    terrainProvider = { Terrains.MOONVEIL_MEADOW },
) {
    private companion object {
        val STYLE = MeadowStyle(
            displayName = "Moonveil Meadow",
            night = true,
            sunDirection = Vector3f(-0.28f, -0.62f, 0.74f),
            horizonColor = Vector3f(0.10f, 0.14f, 0.28f),
            zenithColor = Vector3f(0.02f, 0.045f, 0.14f),
            cloudColor = Vector3f(0.30f, 0.35f, 0.50f),
            lightColor = Vector3f(0.58f, 0.74f, 1f),
            lightIntensity = 1.18f,
            ambientColor = Vector3f(0.28f, 0.38f, 0.64f),
            ambientIntensity = 0.62f,
            shadowStrength = 0.34f,
            fogColor = Vector3f(0.075f, 0.095f, 0.19f),
            fogDensity = 0.014f,
            grassSeed = 4_096,
            windDirection = Vector2f(0.65f, -0.5f),
            windSpeed = 1.10f,
            createEffects = {
                listOf(
                    ColorEffect().apply {
                        exposure = 1.10f
                        saturation = 0.78f
                        lift = Vector3f(0.018f, 0.032f, 0.070f)
                        tint = Vector3f(0.82f, 0.92f, 1.12f)
                    },
                    ContrastEffect().apply { contrast = 1.03f },
                )
            },
        )
    }
}
