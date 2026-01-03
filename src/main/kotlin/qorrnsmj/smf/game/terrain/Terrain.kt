package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.terrain.component.TerrainModel
import qorrnsmj.smf.math.Vector3f

open class Terrain(
    val position: Vector3f = Vector3f(0f, 0f, 0f),
    val rotation: Vector3f = Vector3f(0f, 0f, 0f),
    val scale: Vector3f = Vector3f(1f, 1f, 1f),
    val model: TerrainModel,
)
