package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.graphic.light.DirectionalLight
import qorrnsmj.smf.graphic.scene.settings.FogSettings
import qorrnsmj.smf.graphic.skydome.Skydome
import qorrnsmj.smf.graphic.skybox.Skybox
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f

data class SkyEnvironment(
    var skybox: Skybox = Skyboxes.DEFAULT,
    var skyboxEnabled: Boolean = true,
    var skydome: Skydome? = null,
    var skyVisible: Boolean = true,
    var skyColor: Vector3f = Vector3f(1f, 1f, 1f),
    var celestialLight: DirectionalLight? = null,
    val fog: FogSettings = FogSettings(),
)
