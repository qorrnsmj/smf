package qorrnsmj.smf.game.entity.mob

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.CollisionConfig
import qorrnsmj.smf.game.entity.custom.LivingEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.IPhysicsComponent

abstract class MobEntity(
    transform: Transform = Transform(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = DynamicPhysics(collider = null),
    collisionConfig: CollisionConfig = CollisionConfig(),
) : LivingEntity(
    transform = transform,
    model = model,
    physicsComponent = physicsComponent,
    collisionConfig = collisionConfig,
)
