package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.graphic.scene.light.DirectionalLight
import qorrnsmj.smf.graphic.scene.sky.Skydome
import qorrnsmj.smf.graphic.scene.sky.Skybox
import qorrnsmj.smf.graphic.scene.sky.Skyboxes
import qorrnsmj.smf.math.Vector3f

data class SceneEnvironment(
    val sky: SkySettings = SkySettings(),
    val fog: FogSettings = FogSettings(),
    var celestialLight: DirectionalLight? = null,
)

data class SkySettings(
    var skybox: Skybox = Skyboxes.DEFAULT,
    var skyboxEnabled: Boolean = true,
    var skydome: Skydome? = null,
    var visible: Boolean = true,
    var color: Vector3f = Vector3f(1f, 1f, 1f),
)

data class FogSettings(
    var enabled: Boolean = false,
    var color: Vector3f = Vector3f(0.32f, 0.38f, 0.40f),
    var distanceDensity: Float = 0.00014f,
    var distanceGradient: Float = 1.35f,
    var heightDensity: Float = 0.34f,
    var bottomY: Float = 0f,
    var topY: Float = 280f,
    var heightFalloff: Float = 2.4f,
    var heightDistanceStart: Float = 12f,
    var heightDistanceEnd: Float = 180f,
)
