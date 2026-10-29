package qorrnsmj.smf.game.entity.billboard

import qorrnsmj.smf.game.entity.base.Entity
import qorrnsmj.smf.game.entity.base.Transform
import qorrnsmj.smf.graphic.billboard.Billboard
import qorrnsmj.smf.physics.component.IPhysicsComponent
import qorrnsmj.smf.physics.component.StaticPhysics

open class BillboardEntity(
    transform: Transform,
    val billboard: Billboard,
    physicsComponent: IPhysicsComponent = StaticPhysics(),
) : Entity(
    transform = transform,
    physicsComponent = physicsComponent,
)
