package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.light.Light

data class Scene(
    var camera: Camera = Camera(),
    val entities: MutableList<Entity> = mutableListOf(),
    val lights: MutableList<Light> = mutableListOf()
)
