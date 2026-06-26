package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.IPhysicsComponent

abstract class LivingEntity(
    transform: Transform = Transform(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = DynamicPhysics(collider = null),
    override var collisionConfig: CollisionConfig = CollisionConfig(),
) : Entity(
    transform = transform,
    model = model,
    physicsComponent = physicsComponent
), Collidable {
    override fun getCollisionBasePosition(): Vector3f = worldTransform.position
}
