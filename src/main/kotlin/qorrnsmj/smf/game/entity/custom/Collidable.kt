package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

interface Collidable {
    var collisionShape: CollisionShape
    var collisionHalfWidth: Float
    var collisionHeight: Float
    var collisionHalfDepth: Float
    var groundProbeDistance: Float

    fun getCollisionBasePosition(): Vector3f

    fun getCapsuleCollider(): CapsuleCollider {
        return CapsuleCollider(
            radius = minOf(collisionHalfWidth, collisionHalfDepth),
            height = collisionHeight,
        )
    }

    fun getCapsuleColliderWithFeetOffset(feetOffset: Float): Pair<CapsuleCollider, Vector3f> {
        return getCapsuleCollider() to getCollisionBasePosition().add(Vector3f(0f, feetOffset, 0f))
    }

    fun getAabb(): AABB {
        val feet = getCollisionBasePosition()
        return AABB(
            min = Vector3f(
                feet.x - collisionHalfWidth,
                feet.y,
                feet.z - collisionHalfDepth
            ),
            max = Vector3f(
                feet.x + collisionHalfWidth,
                feet.y + collisionHeight,
                feet.z + collisionHalfDepth
            )
        )
    }

    fun getAabbWithFeetOffset(feetOffset: Float): AABB {
        val feet = getCollisionBasePosition()
        val y = feet.y + feetOffset
        return AABB(
            min = Vector3f(
                feet.x - collisionHalfWidth,
                y,
                feet.z - collisionHalfDepth
            ),
            max = Vector3f(
                feet.x + collisionHalfWidth,
                y + collisionHeight,
                feet.z + collisionHalfDepth
            )
        )
    }
}

enum class CollisionShape {
    AABB,
    CAPSULE,
}
