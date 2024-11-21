package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.game.entity.component.Entity
import qorrnsmj.smf.graphic.render.camera.Camera
import qorrnsmj.smf.graphic.render.light.Light

data class Scene(
    var camera: Camera = Camera(),
    val entities: MutableList<Entity> = mutableListOf(),
    val lights: MutableList<Light> = mutableListOf()
)
