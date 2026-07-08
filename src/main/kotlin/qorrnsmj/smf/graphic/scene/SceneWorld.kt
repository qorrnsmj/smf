package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.graphic.text.TextElement

data class SceneWorld(
    var camera: Camera = Camera(),
    var terrain: Terrain? = null,
    var terrainHeightProvider: HeightProvider? = null,

    val entities: MutableList<Entity> = mutableListOf(),
    val lights: MutableList<Light> = mutableListOf(),
    val textElements: MutableList<TextElement> = mutableListOf(),
)
