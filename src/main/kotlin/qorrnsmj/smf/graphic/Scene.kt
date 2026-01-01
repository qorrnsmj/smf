package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.PbrEntity
import qorrnsmj.smf.game.model.component.Model
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.game.skybox.SkyboxModels
import qorrnsmj.smf.game.terrain.Terrain
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.math.Vector3f

data class Scene(
    var camera: Camera = Camera(),
    var skybox: Model = SkyboxModels.NONE,
    var skyColor: Vector3f = Vector3f(1f, 1f, 1f),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<PbrEntity> = mutableListOf(),
    val terrains: MutableList<Terrain> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf()
)
