package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

data class CollisionConfig(
    val shape: CollisionShape = CollisionShape.AABB,
    val halfWidth: Float = 0.5f,
    val height: Float = 1f,
    val halfDepth: Float = 0.5f,
    val groundProbeDistance: Float = 0.05f,
)

interface Collidable {
    var collisionConfig: CollisionConfig

    fun getCollisionBasePosition(): Vector3f

    fun getCapsuleCollider(): CapsuleCollider {
        return CapsuleCollider(
            radius = minOf(collisionConfig.halfWidth, collisionConfig.halfDepth),
            height = collisionConfig.height,
        )
    }

    fun getCapsuleColliderWithFeetOffset(feetOffset: Float): Pair<CapsuleCollider, Vector3f> {
        return getCapsuleCollider() to getCollisionBasePosition().add(Vector3f(0f, feetOffset, 0f))
    }

    fun getAabb(): AABB {
        val feet = getCollisionBasePosition()
        return AABB(
            min = Vector3f(
                feet.x - collisionConfig.halfWidth,
                feet.y,
                feet.z - collisionConfig.halfDepth
            ),
            max = Vector3f(
                feet.x + collisionConfig.halfWidth,
                feet.y + collisionConfig.height,
                feet.z + collisionConfig.halfDepth
            )
        )
    }

    fun getAabbWithFeetOffset(feetOffset: Float): AABB {
        val feet = getCollisionBasePosition()
        val y = feet.y + feetOffset
        return AABB(
            min = Vector3f(
                feet.x - collisionConfig.halfWidth,
                y,
                feet.z - collisionConfig.halfDepth
            ),
            max = Vector3f(
                feet.x + collisionConfig.halfWidth,
                y + collisionConfig.height,
                feet.z + collisionConfig.halfDepth
            )
        )
    }
}

enum class CollisionShape {
    AABB,
    CAPSULE,
}
