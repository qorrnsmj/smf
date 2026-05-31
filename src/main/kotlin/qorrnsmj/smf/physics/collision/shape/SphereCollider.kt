package qorrnsmj.smf.physics.collision.shape

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.data.CollisionResult

/**
 * Sphere collider implementation
 */
class SphereCollider(
    val radius: Float,
    val offset: Vector3f = Vector3f(0f, 0f, 0f)
) : Collider {
    
    override fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult? {
        return when (other) {
            is SphereCollider -> sphereVsSphere(this, other, thisPosition, otherPosition)
            is BoxCollider -> sphereVsBox(this, other, thisPosition, otherPosition)
            else -> null
        }
    }
    
    override fun getCenter(position: Vector3f): Vector3f = position.add(offset)
    
    override fun getBounds(position: Vector3f): AABB {
        val center = getCenter(position)
        return AABB(
            min = Vector3f(center.x - radius, center.y - radius, center.z - radius),
            max = Vector3f(center.x + radius, center.y + radius, center.z + radius)
        )
    }
    
    companion object {
        /**
         * Sphere vs Sphere collision detection
         */
        fun sphereVsSphere(
            sphere1: SphereCollider, 
            sphere2: SphereCollider, 
            pos1: Vector3f, 
            pos2: Vector3f
        ): CollisionResult? {
            val center1 = sphere1.getCenter(pos1)
            val center2 = sphere2.getCenter(pos2)
            val distance = center1.distanceTo(center2)
            val combinedRadius = sphere1.radius + sphere2.radius
            
            if (distance <= combinedRadius) {
                val penetrationDepth = combinedRadius - distance
                val direction = center2.subtract(center1)
                val normal = if (direction.length() > 0f) direction.normalize() else Vector3f(0f, 1f, 0f)
                val contactPoint = center1.add(normal.scale(sphere1.radius - penetrationDepth * 0.5f))
                
                return CollisionResult(
                    penetrationDepth = penetrationDepth,
                    collisionNormal = normal,
                    contactPoint = contactPoint
                )
            }
            
            return null
        }
        
        /**
         * Sphere vs Box collision detection (simplified)
         */
        fun sphereVsBox(
            sphere: SphereCollider,
            box: BoxCollider,
            spherePos: Vector3f,
            boxPos: Vector3f
        ): CollisionResult? {
            val sphereCenter = sphere.getCenter(spherePos)
            val boxCenter = box.getCenter(boxPos)

            // Find the closest point on the box to the sphere center
            val closestPoint = Vector3f(
                clamp(sphereCenter.x, boxCenter.x - box.width/2, boxCenter.x + box.width/2),
                clamp(sphereCenter.y, boxCenter.y - box.height/2, boxCenter.y + box.height/2),
                clamp(sphereCenter.z, boxCenter.z - box.depth/2, boxCenter.z + box.depth/2)
            )
            
            val distance = sphereCenter.distanceTo(closestPoint)
            
            if (distance <= sphere.radius) {
                val penetrationDepth = sphere.radius - distance
                val direction = sphereCenter.subtract(closestPoint)
                val normal = if (direction.length() > 0f) direction.normalize() else Vector3f(0f, 1f, 0f)
                
                return CollisionResult(
                    penetrationDepth = penetrationDepth,
                    collisionNormal = normal,
                    contactPoint = closestPoint
                )
            }
            
            return null
        }
        
        private fun clamp(value: Float, min: Float, max: Float): Float {
            return when {
                value < min -> min
                value > max -> max
                else -> value
            }
        }
    }
}
