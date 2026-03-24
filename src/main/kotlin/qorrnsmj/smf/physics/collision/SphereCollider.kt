package qorrnsmj.smf.physics.collision

import qorrnsmj.smf.math.Vector3f
import kotlin.math.sqrt

/**
 * Sphere collider implementation
 */
class SphereCollider(val radius: Float) : Collider {
    
    override fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult? {
        return when (other) {
            is SphereCollider -> sphereVsSphere(this, other, thisPosition, otherPosition)
            is BoxCollider -> sphereVsBox(this, other, thisPosition, otherPosition)
            else -> null
        }
    }
    
    override fun getCenter(position: Vector3f): Vector3f = position
    
    override fun getBounds(position: Vector3f): AABB {
        return AABB(
            min = Vector3f(position.x - radius, position.y - radius, position.z - radius),
            max = Vector3f(position.x + radius, position.y + radius, position.z + radius)
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
            val distance = pos1.distanceTo(pos2)
            val combinedRadius = sphere1.radius + sphere2.radius
            
            if (distance <= combinedRadius) {
                val penetrationDepth = combinedRadius - distance
                val direction = pos2.subtract(pos1)
                val normal = if (direction.length() > 0f) direction.normalize() else Vector3f(0f, 1f, 0f)
                val contactPoint = pos1.add(normal.scale(sphere1.radius - penetrationDepth * 0.5f))
                
                return CollisionResult(
                    hasCollision = true,
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
            // Find the closest point on the box to the sphere center
            val closestPoint = Vector3f(
                clamp(spherePos.x, boxPos.x - box.width/2, boxPos.x + box.width/2),
                clamp(spherePos.y, boxPos.y - box.height/2, boxPos.y + box.height/2),
                clamp(spherePos.z, boxPos.z - box.depth/2, boxPos.z + box.depth/2)
            )
            
            val distance = spherePos.distanceTo(closestPoint)
            
            if (distance <= sphere.radius) {
                val penetrationDepth = sphere.radius - distance
                val direction = spherePos.subtract(closestPoint)
                val normal = if (direction.length() > 0f) direction.normalize() else Vector3f(0f, 1f, 0f)
                
                return CollisionResult(
                    hasCollision = true,
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