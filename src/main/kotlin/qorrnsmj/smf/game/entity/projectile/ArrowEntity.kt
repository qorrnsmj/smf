package qorrnsmj.smf.game.entity.projectile

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.component.DynamicPhysics

class ArrowEntity(
    position: Vector3f,
    direction: Vector3f,
    speed: Float = 7f,
) : ProjectileEntity(
    transform = Transform(
        position = position,
        scale = Vector3f(8f, 8f, 8f),
    ),
    model = EntityModels.getModel(STALL, "WoodPole1"),
    physicsComponent = DynamicPhysics(
        mass = 1f,
        velocity = direction.normalize().scale(speed),
        restitution = 0.05f,
        friction = 0.4f,
        drag = 0f,
        collider = BoxCollider(width = 8f, height = 8f, depth = 36f),
    ),
    maxLifeTime = 6f,
)
