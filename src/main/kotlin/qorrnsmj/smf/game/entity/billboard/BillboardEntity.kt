package qorrnsmj.smf.game.entity.billboard

import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.Transform
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
