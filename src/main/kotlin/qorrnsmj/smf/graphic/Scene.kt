package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.game.skybox.SkyboxModels
import qorrnsmj.smf.game.terrain.Terrain
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.math.Vector3f

data class Scene(
    var camera: Camera = Camera(),
    var skybox: Model = SkyboxModels.NONE,
    var skyColor: Vector3f = Vector3f(0f, 0f, 0f),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val terrains: MutableList<Terrain> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf()
)
