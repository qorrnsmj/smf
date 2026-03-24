package qorrnsmj.smf.physics.collision

import qorrnsmj.smf.math.Vector3f

/**
 * Box (AABB) collider implementation
 */
class BoxCollider(val width: Float, val height: Float, val depth: Float) : Collider {
    
    override fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult? {
        return when (other) {
            is BoxCollider -> boxVsBox(this, other, thisPosition, otherPosition)
            is SphereCollider -> SphereCollider.sphereVsBox(other, this, otherPosition, thisPosition)?.let {
                // Reverse the collision normal for proper response
                it.copy(collisionNormal = it.collisionNormal.scale(-1f))
            }
            else -> null
        }
    }
    
    override fun getCenter(position: Vector3f): Vector3f = position
    
    override fun getBounds(position: Vector3f): AABB {
        return AABB(
            min = Vector3f(position.x - width/2, position.y - height/2, position.z - depth/2),
            max = Vector3f(position.x + width/2, position.y + height/2, position.z + depth/2)
        )
    }
    
    companion object {
        /**
         * Box vs Box collision detection using AABB method
         */
        fun boxVsBox(
            box1: BoxCollider,
            box2: BoxCollider,
            pos1: Vector3f,
            pos2: Vector3f
        ): CollisionResult? {
            val bounds1 = box1.getBounds(pos1)
            val bounds2 = box2.getBounds(pos2)
            
            if (!bounds1.intersects(bounds2)) {
                return null
            }
            
            // Calculate overlap on each axis
            val overlapX = minOf(bounds1.max.x - bounds2.min.x, bounds2.max.x - bounds1.min.x)
            val overlapY = minOf(bounds1.max.y - bounds2.min.y, bounds2.max.y - bounds1.min.y)
            val overlapZ = minOf(bounds1.max.z - bounds2.min.z, bounds2.max.z - bounds1.min.z)
            
            // Find the axis with minimum overlap (separation axis)
            val minOverlap = minOf(overlapX, overlapY, overlapZ)
            
            val normal = when (minOverlap) {
                overlapX -> Vector3f(if (pos1.x < pos2.x) -1f else 1f, 0f, 0f)
                overlapY -> Vector3f(0f, if (pos1.y < pos2.y) -1f else 1f, 0f)
                else -> Vector3f(0f, 0f, if (pos1.z < pos2.z) -1f else 1f)
            }
            
            // Contact point is the center of the overlap region
            val contactPoint = Vector3f(
                (maxOf(bounds1.min.x, bounds2.min.x) + minOf(bounds1.max.x, bounds2.max.x)) * 0.5f,
                (maxOf(bounds1.min.y, bounds2.min.y) + minOf(bounds1.max.y, bounds2.max.y)) * 0.5f,
                (maxOf(bounds1.min.z, bounds2.min.z) + minOf(bounds1.max.z, bounds2.max.z)) * 0.5f
            )
            
            return CollisionResult(
                hasCollision = true,
                penetrationDepth = minOverlap,
                collisionNormal = normal,
                contactPoint = contactPoint
            )
        }
    }
}