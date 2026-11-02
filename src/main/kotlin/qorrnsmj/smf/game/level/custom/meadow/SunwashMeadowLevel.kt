package qorrnsmj.smf.game.level.custom.meadow
import qorrnsmj.smf.graphic.scene.terrain.Terrain

import qorrnsmj.smf.graphic.effect.ColorEffect
import qorrnsmj.smf.graphic.effect.ContrastEffect
import qorrnsmj.smf.graphic.scene.terrain.Terrains
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f

internal class SunwashMeadowLevel : MeadowLevel(
    style = STYLE,
    terrainProvider = { Terrains.SUNWASH_MEADOW },
) {
    private companion object {
        val STYLE = MeadowStyle(
            displayName = "Sunwash Meadow",
            night = false,
            sunDirection = Vector3f(0.35f, 0.78f, -0.52f),
            horizonColor = Vector3f(0.82f, 0.91f, 1.00f),
            zenithColor = Vector3f(0.34f, 0.64f, 0.96f),
            cloudColor = Vector3f(0.82f, 0.84f, 0.82f),
            lightColor = Vector3f(1f, 0.95f, 0.82f),
            lightIntensity = 2.80f,
            ambientColor = Vector3f(0.72f, 0.78f, 0.84f),
            ambientIntensity = 0.38f,
            shadowStrength = 0.54f,
            fogColor = Vector3f(0.77f, 0.86f, 0.92f),
            fogDensity = 0.010f,
            grassSeed = 2_048,
            windDirection = Vector2f(1f, 0.28f),
            windSpeed = 1.45f,
            createEffects = {
                listOf(
                    ColorEffect().apply {
                        exposure = 1.14f
                        saturation = 0.92f
                        lift = Vector3f(0.050f, 0.055f, 0.035f)
                        tint = Vector3f(1.04f, 1.025f, 0.98f)
                    },
                    ContrastEffect().apply { contrast = 0.82f },
                )
            },
        )
    }
}
