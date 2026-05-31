package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.physics.component.IPhysicsComponent
import qorrnsmj.smf.physics.component.StaticPhysics

open class ObjectEntity(
    transform: Transform = Transform(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = StaticPhysics(),
) : Entity(
    transform = transform,
    model = model,
    physicsComponent = physicsComponent
)

