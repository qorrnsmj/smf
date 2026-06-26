package qorrnsmj.smf.game.entity.projectile

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.IPhysicsComponent

abstract class ProjectileEntity(
    transform: Transform = Transform(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = DynamicPhysics(collider = null),
    private val maxLifeTime: Float = 5f,
) : ObjectEntity(
    transform = transform,
    model = model,
    physicsComponent = physicsComponent,
) {
    var isExpired: Boolean = false
        private set

    private var lifeTime = 0f

    override fun update(deltaTime: Float) {
        lifeTime += deltaTime
        if (lifeTime >= maxLifeTime) {
            isExpired = true
            physicsComponent.stop()
        }

        super.update(deltaTime)
    }
}
