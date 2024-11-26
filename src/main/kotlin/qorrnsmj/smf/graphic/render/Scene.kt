package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.light.Light
import qorrnsmj.smf.game.terrain.Terrain

data class Scene(
    var camera: Camera = Camera(),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val terrains: MutableList<Terrain> = mutableListOf()
)
