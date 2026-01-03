package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.model.component.Model
import qorrnsmj.smf.math.Vector3f

open class Entity(
    var position: Vector3f = Vector3f(0f, 0f, 0f),
    var rotation: Vector3f = Vector3f(0f, 0f, 0f),
    var scale: Vector3f = Vector3f(1f, 1f, 1f),
    val model: Model = EntityModels.EMPTY,
    val children: MutableList<Entity> = mutableListOf()
)
