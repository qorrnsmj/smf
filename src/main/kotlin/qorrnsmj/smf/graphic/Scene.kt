package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.game.terrain.Terrain
import qorrnsmj.smf.graphic.render.effect.Effect

data class Scene(
    var camera: Camera = Camera(),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val terrains: MutableList<Terrain> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf()
)
