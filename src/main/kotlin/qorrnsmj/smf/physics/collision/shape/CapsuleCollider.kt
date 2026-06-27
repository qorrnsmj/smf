package qorrnsmj.smf.physics.collision.shape

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.data.CollisionResult

/**
 * Vertical capsule collider. The entity position is treated as the capsule feet position.
 */
class CapsuleCollider(
    val radius: Float,
    val height: Float,
    val offset: Vector3f = Vector3f(0f, 0f, 0f),
) : Collider {
    init {
        require(radius > 0f) { "Capsule radius must be positive" }
        require(height >= radius * 2f) { "Capsule height must be at least twice the radius" }
    }

    override fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult? {
        return when (other) {
            is CapsuleCollider -> capsuleVsCapsule(this, other, thisPosition, otherPosition)
            is SphereCollider -> capsuleVsSphere(this, other, thisPosition, otherPosition)
            is BoxCollider -> capsuleVsBox(this, other, thisPosition, otherPosition)
            else -> null
        }
    }

    override fun getCenter(position: Vector3f): Vector3f {
        val base = position.add(offset)
        return Vector3f(base.x, base.y + height * 0.5f, base.z)
    }

    override fun getBounds(position: Vector3f): AABB {
        val base = position.add(offset)
        return AABB(
            min = Vector3f(base.x - radius, base.y, base.z - radius),
            max = Vector3f(base.x + radius, base.y + height, base.z + radius),
        )
    }

    fun getBottomCenter(position: Vector3f): Vector3f {
        val base = position.add(offset)
        return Vector3f(base.x, base.y + radius, base.z)
    }

    fun getTopCenter(position: Vector3f): Vector3f {
        val base = position.add(offset)
        return Vector3f(base.x, base.y + height - radius, base.z)
    }

    fun closestPointOnAxis(point: Vector3f, position: Vector3f): Vector3f {
        val bottom = getBottomCenter(position)
        val top = getTopCenter(position)
        return Vector3f(bottom.x, clamp(point.y, bottom.y, top.y), bottom.z)
    }

    companion object {
        fun capsuleVsSphere(
            capsule: CapsuleCollider,
            sphere: SphereCollider,
            capsulePosition: Vector3f,
            spherePosition: Vector3f,
        ): CollisionResult? {
            val sphereCenter = sphere.getCenter(spherePosition)
            val closest = capsule.closestPointOnAxis(sphereCenter, capsulePosition)
            val direction = sphereCenter.subtract(closest)
            val distance = direction.length()
            val combinedRadius = capsule.radius + sphere.radius

            if (distance >= combinedRadius) {
                return null
            }

            val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
            val penetration = combinedRadius - distance
            return CollisionResult(
                penetrationDepth = penetration,
                collisionNormal = normal,
                contactPoint = closest.add(normal.scale(capsule.radius - penetration * 0.5f)),
            )
        }

        fun capsuleVsBox(
            capsule: CapsuleCollider,
            box: BoxCollider,
            capsulePosition: Vector3f,
            boxPosition: Vector3f,
        ): CollisionResult? {
            val correction = getCapsuleBoxCorrection(capsule, capsulePosition, box.getBounds(boxPosition)) ?: return null
            val normal = correction.normalize()
            return CollisionResult(
                penetrationDepth = correction.length(),
                collisionNormal = normal,
                contactPoint = capsule.getCenter(capsulePosition).subtract(normal.scale(capsule.radius)),
            )
        }

        fun capsuleVsCapsule(
            capsule1: CapsuleCollider,
            capsule2: CapsuleCollider,
            position1: Vector3f,
            position2: Vector3f,
        ): CollisionResult? {
            val closest = closestPointsOnVerticalSegments(capsule1, position1, capsule2, position2)
            val direction = closest.second.subtract(closest.first)
            val distance = direction.length()
            val combinedRadius = capsule1.radius + capsule2.radius

            if (distance >= combinedRadius) {
                return null
            }

            val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
            val penetration = combinedRadius - distance
            return CollisionResult(
                penetrationDepth = penetration,
                collisionNormal = normal,
                contactPoint = closest.first.add(normal.scale(capsule1.radius - penetration * 0.5f)),
            )
        }

        fun getCapsuleBoxCorrection(capsule: CapsuleCollider, capsulePosition: Vector3f, boxBounds: AABB): Vector3f? {
            val bottom = capsule.getBottomCenter(capsulePosition)
            val top = capsule.getTopCenter(capsulePosition)
            val axisX = bottom.x
            val axisZ = bottom.z

            val closestX = clamp(axisX, boxBounds.min.x, boxBounds.max.x)
            val closestZ = clamp(axisZ, boxBounds.min.z, boxBounds.max.z)
            val (axisY, boxY) = closestYBetweenIntervals(bottom.y, top.y, boxBounds.min.y, boxBounds.max.y)

            val capsulePoint = Vector3f(axisX, axisY, axisZ)
            val boxPoint = Vector3f(closestX, boxY, closestZ)
            val direction = capsulePoint.subtract(boxPoint)
            val distance = direction.length()

            if (distance >= capsule.radius) {
                return null
            }

            if (distance > 0f) {
                return direction.divide(distance).scale(capsule.radius - distance)
            }

            return getInsideBoxFallbackCorrection(capsule.getBounds(capsulePosition), boxBounds)
        }

        private fun closestPointsOnVerticalSegments(
            capsule1: CapsuleCollider,
            position1: Vector3f,
            capsule2: CapsuleCollider,
            position2: Vector3f,
        ): Pair<Vector3f, Vector3f> {
            val bottom1 = capsule1.getBottomCenter(position1)
            val top1 = capsule1.getTopCenter(position1)
            val bottom2 = capsule2.getBottomCenter(position2)
            val top2 = capsule2.getTopCenter(position2)
            val (y1, y2) = closestYBetweenIntervals(bottom1.y, top1.y, bottom2.y, top2.y)
            return Vector3f(bottom1.x, y1, bottom1.z) to Vector3f(bottom2.x, y2, bottom2.z)
        }

        private fun closestYBetweenIntervals(min1: Float, max1: Float, min2: Float, max2: Float): Pair<Float, Float> {
            if (max1 < min2) {
                return max1 to min2
            }
            if (max2 < min1) {
                return min1 to max2
            }

            val y = (maxOf(min1, min2) + minOf(max1, max2)) * 0.5f
            return y to y
        }

        private fun getInsideBoxFallbackCorrection(capsuleBounds: AABB, boxBounds: AABB): Vector3f? {
            if (!capsuleBounds.intersects(boxBounds)) {
                return null
            }

            val overlapX = minOf(capsuleBounds.max.x - boxBounds.min.x, boxBounds.max.x - capsuleBounds.min.x)
            val overlapY = minOf(capsuleBounds.max.y - boxBounds.min.y, boxBounds.max.y - capsuleBounds.min.y)
            val overlapZ = minOf(capsuleBounds.max.z - boxBounds.min.z, boxBounds.max.z - capsuleBounds.min.z)

            if (overlapX <= 0f || overlapY <= 0f || overlapZ <= 0f) {
                return null
            }

            val capsuleCenter = Vector3f(
                (capsuleBounds.min.x + capsuleBounds.max.x) * 0.5f,
                (capsuleBounds.min.y + capsuleBounds.max.y) * 0.5f,
                (capsuleBounds.min.z + capsuleBounds.max.z) * 0.5f,
            )
            val boxCenter = Vector3f(
                (boxBounds.min.x + boxBounds.max.x) * 0.5f,
                (boxBounds.min.y + boxBounds.max.y) * 0.5f,
                (boxBounds.min.z + boxBounds.max.z) * 0.5f,
            )

            return when (minOf(overlapX, minOf(overlapY, overlapZ))) {
                overlapX -> Vector3f(if (capsuleCenter.x >= boxCenter.x) overlapX else -overlapX, 0f, 0f)
                overlapY -> Vector3f(0f, if (capsuleCenter.y >= boxCenter.y) overlapY else -overlapY, 0f)
                else -> Vector3f(0f, 0f, if (capsuleCenter.z >= boxCenter.z) overlapZ else -overlapZ)
            }
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
