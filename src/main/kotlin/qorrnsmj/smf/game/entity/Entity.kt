package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.entity.model.EntityModels
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.math.Vector3f

open class Entity(
    var position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var rotation: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
    val model: Model = EntityModels.EMPTY,
    val children: MutableList<Entity> = mutableListOf()
)
