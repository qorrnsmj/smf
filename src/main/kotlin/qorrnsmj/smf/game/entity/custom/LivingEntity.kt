package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.IPhysicsComponent

open class LivingEntity(
    transform: Transform = Transform(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = DynamicPhysics(collider = null),
    override var collisionHalfWidth: Float = 0.5f,
    override var collisionHeight: Float = 1f,
    override var collisionHalfDepth: Float = 0.5f,
    override var groundProbeDistance: Float = 0.05f,
) : Entity(
    transform = transform,
    model = model,
    physicsComponent = physicsComponent
), Collidable {
    override fun getCollisionBasePosition(): Vector3f = worldTransform.position
}
