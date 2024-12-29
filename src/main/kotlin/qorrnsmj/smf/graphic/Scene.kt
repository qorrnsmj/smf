package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.game.terrain.FlatTerrain
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.math.Vector3f

data class Scene(
    var camera: Camera = Camera(),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val terrains: MutableList<FlatTerrain> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf(),
    val skyColor: Vector3f = Vector3f(0.3f, 0.6f, 1f)
)
