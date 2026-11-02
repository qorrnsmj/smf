package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.LightEntity
import qorrnsmj.smf.graphic.scene.light.Light
import qorrnsmj.smf.graphic.scene.grass.GrassPatch
import qorrnsmj.smf.graphic.text.TextBoxElement
import qorrnsmj.smf.graphic.scene.terrain.HeightProvider
import qorrnsmj.smf.graphic.scene.terrain.Terrain
import qorrnsmj.smf.graphic.text.TextElement

data class SceneWorld(
    var camera: Camera = Camera(),
    var terrain: Terrain? = null,
    var terrainHeightProvider: HeightProvider? = null,

    val entities: MutableList<Entity> = mutableListOf(),
    val lights: MutableList<Light> = mutableListOf(),
    val textElements: MutableList<TextElement> = mutableListOf(),
    val debugTextElements: MutableList<TextElement> = mutableListOf(),
    val textBoxes: MutableList<TextBoxElement> = mutableListOf(),
    val grassPatches: MutableList<GrassPatch> = mutableListOf(),
) {
    val activeLights: List<Light>
        get() = buildList {
            addAll(lights)
            entities.forEach { collectLights(it) }
        }

    private fun MutableList<Light>.collectLights(entity: Entity) {
        if (entity is LightEntity) add(entity.resolveLight())
        entity.children.forEach { collectLights(it) }
    }
}
