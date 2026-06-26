package qorrnsmj.smf.game.entity.mob

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.EntityModels.STALL
import qorrnsmj.smf.game.entity.custom.CollisionConfig
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.component.DynamicPhysics
import kotlin.math.sin

class SlimeEntity(
    spawnPosition: Vector3f,
) : MobEntity(
    transform = Transform(
        position = spawnPosition,
        scale = Vector3f(12f, 12f, 12f),
    ),
    model = EntityModels.getModel(STALL, "Fruits"),
    physicsComponent = DynamicPhysics(
        mass = 8f,
        friction = 0.7f,
        drag = 0.05f,
        collider = BoxCollider(width = 28f, height = 24f, depth = 28f, offset = Vector3f(0f, 12f, 0f)),
    ),
    collisionConfig = CollisionConfig(
        halfWidth = 14f,
        height = 24f,
        halfDepth = 14f,
    ),
) {
    private val origin = spawnPosition
    private var elapsedTime = 0f

    override fun update(deltaTime: Float) {
        elapsedTime += deltaTime
        val patrolVelocity = sin(elapsedTime).toFloat() * 0.6f
        physicsComponent.velocity = Vector3f(patrolVelocity, physicsComponent.velocity.y, 0f)

        if (worldTransform.position.distanceTo(origin) > 80f) {
            localTransform = localTransform.copy(position = origin)
            physicsComponent.stop()
        }

        super.update(deltaTime)
    }
}
