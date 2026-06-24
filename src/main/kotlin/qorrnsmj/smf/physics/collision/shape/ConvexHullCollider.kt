package qorrnsmj.smf.physics.collision.shape

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.data.CollisionResult
import kotlin.math.abs

data class Plane(
    val normal: Vector3f,
    val distance: Float,
    val texture: String = "",
    val isGhost: Boolean = false,
) {
    fun distanceTo(point: Vector3f): Float {
        return normal.dot(point) - distance
    }
}

class ConvexHullCollider(
    val planes: List<Plane>,
    val vertices: List<Vector3f>,
) : Collider {
    val responsePlanes: List<Plane> = planes.filterNot { it.isGhost }

    fun contains(point: Vector3f): Boolean =
        planes.all { plane ->
            plane.normal.dot(point) - plane.distance <= 0f
        }

    override fun checkCollision(other: Collider, thisPosition: Vector3f, otherPosition: Vector3f): CollisionResult? {
        return when (other) {
            is BoxCollider -> checkBoxCollision(other, thisPosition, otherPosition)
            else -> null
        }
    }

    override fun getCenter(position: Vector3f): Vector3f {
        if (vertices.isEmpty()) {
            return position
        }

        val center = vertices
            .fold(Vector3f()) { acc, vertex -> acc.add(vertex.add(position)) }
            .divide(vertices.size.toFloat())
        return center
    }

    override fun getBounds(position: Vector3f): AABB {
        require(vertices.isNotEmpty()) { "Convex hull must contain at least one vertex" }

        val first = vertices.first().add(position)
        var minX = first.x
        var minY = first.y
        var minZ = first.z
        var maxX = first.x
        var maxY = first.y
        var maxZ = first.z

        for (vertex in vertices.drop(1)) {
            val worldVertex = vertex.add(position)
            minX = minOf(minX, worldVertex.x)
            minY = minOf(minY, worldVertex.y)
            minZ = minOf(minZ, worldVertex.z)
            maxX = maxOf(maxX, worldVertex.x)
            maxY = maxOf(maxY, worldVertex.y)
            maxZ = maxOf(maxZ, worldVertex.z)
        }

        return AABB(
            min = Vector3f(minX, minY, minZ),
            max = Vector3f(maxX, maxY, maxZ),
        )
    }

    private fun checkBoxCollision(
        box: BoxCollider,
        hullPosition: Vector3f,
        boxPosition: Vector3f,
    ): CollisionResult? {
        val boxBounds = box.getBounds(boxPosition)
        val boxCenter = Vector3f(
            (boxBounds.min.x + boxBounds.max.x) * 0.5f,
            (boxBounds.min.y + boxBounds.max.y) * 0.5f,
            (boxBounds.min.z + boxBounds.max.z) * 0.5f,
        )
        val boxHalfExtents = Vector3f(
            (boxBounds.max.x - boxBounds.min.x) * 0.5f,
            (boxBounds.max.y - boxBounds.min.y) * 0.5f,
            (boxBounds.max.z - boxBounds.min.z) * 0.5f,
        )

        var bestNormal: Vector3f? = null
        var bestPenetration = Float.POSITIVE_INFINITY

        for (plane in planes) {
            val shiftedDistance = plane.distance + plane.normal.dot(hullPosition)
            val centerDistance = plane.normal.dot(boxCenter) - shiftedDistance
            val projectedRadius =
                abs(plane.normal.x) * boxHalfExtents.x +
                    abs(plane.normal.y) * boxHalfExtents.y +
                    abs(plane.normal.z) * boxHalfExtents.z

            if (centerDistance > projectedRadius) {
                return null
            }

            if (plane.isGhost) {
                continue
            }

            val penetration = projectedRadius - centerDistance
            if (penetration < bestPenetration) {
                bestPenetration = penetration
                bestNormal = plane.normal
            }
        }

        val normal = bestNormal ?: return null
        return CollisionResult(
            penetrationDepth = bestPenetration,
            collisionNormal = normal,
            contactPoint = boxCenter.subtract(normal.scale(bestPenetration * 0.5f)),
        )
    }
}
