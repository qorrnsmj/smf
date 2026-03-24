package qorrnsmj.smf.game.entity

import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent

open class Entity(
    var position: Vector3f = Vector3f(0f, 0f, 0f),
    var rotation: Vector3f = Vector3f(0f, 0f, 0f),
    var scale: Vector3f = Vector3f(1f, 1f, 1f),
    val model: Model = EntityModels.EMPTY,
    val children: MutableList<Entity> = mutableListOf(),
    var physicsComponent: PhysicsComponent? = null  // Optional physics component
)
