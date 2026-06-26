package qorrnsmj.smf.graphic

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.map.GameMap
import qorrnsmj.smf.graphic.light.Light
import qorrnsmj.smf.graphic.skybox.Skybox
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.effect.Effect
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.graphic.terrain.Terrain
import qorrnsmj.smf.math.Vector3f

data class Scene(
    var camera: Camera = Camera(),
    var map: GameMap? = null,
    var terrain: Terrain? = null,
    var terrainHeightProvider: HeightProvider? = null,
    var skybox: Skybox = Skyboxes.DEFAULT,
    var skyColor: Vector3f = Vector3f(1f, 1f, 1f),
    val lights: MutableList<Light> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val effects: MutableList<Effect> = mutableListOf(),
    val textElements: MutableList<TextElement> = mutableListOf(),
    val cinematicOverlay: CinematicOverlay = CinematicOverlay(),
)
