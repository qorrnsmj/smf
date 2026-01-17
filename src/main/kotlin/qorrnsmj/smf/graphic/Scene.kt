package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.skybox.Skybox
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.graphic.terrain.Terrains
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.math.Vector3f

data class Scene(
    var camera: Camera = Camera(),
    var terrain: Terrain = Terrains.DEFAULT,
    var skybox: Skybox = Skyboxes.DEFAULT,
    var skyColor: Vector3f = Vector3f(1f, 1f, 1f),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf(),
)
