package qorrnsmj.smf.game.map

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.ConvexHullCollider
import qorrnsmj.smf.physics.collision.shape.Plane
import kotlin.math.abs
import kotlin.math.atan2

object MapGeometryBuilder {
    private const val EPSILON = 0.05f

    fun buildVertices(faces: List<MapBrushFace>): List<Vector3f> {
        val planes = buildOrientedPlanes(faces)
        val vertices = mutableListOf<Vector3f>()

        for (i in planes.indices) {
            for (j in i + 1 until planes.size) {
                for (k in j + 1 until planes.size) {
                    val vertex = intersect(planes[i], planes[j], planes[k]) ?: continue
                    if (!isInsideBrush(vertex, planes)) continue
                    if (vertices.none { it.distanceTo(vertex) <= EPSILON }) {
                        vertices.add(vertex)
                    }
                }
            }
        }

        return vertices
    }

    fun buildFaceVertices(brush: MapBrush, face: MapBrushFace): List<Vector3f> {
        val plane = toOrientedPlane(face, getBrushReferencePoint(brush.faces))
        val vertices = brush.vertices
            .filter { abs(plane.distanceTo(it)) <= EPSILON }
            .let { sortFaceVertices(it, plane.normal) }

        if (vertices.size < 3) {
            return emptyList()
        }

        val triangleNormal = vertices[1]
            .subtract(vertices[0])
            .cross(vertices[2].subtract(vertices[0]))

        return if (triangleNormal.dot(plane.normal) < 0f) {
            vertices.reversed()
        } else {
            vertices
        }
    }

    fun toPlane(face: MapBrushFace): Plane {
        val p0 = face.points[0]
        val p1 = face.points[1]
        val p2 = face.points[2]
        val normal = p1.subtract(p0).cross(p2.subtract(p0)).normalize()
        return Plane(normal = normal, distance = normal.dot(p0), texture = face.texture)
    }

    fun buildConvexCollider(brush: MapBrush, ghostFaces: Set<MapBrushFace> = emptySet()): ConvexHullCollider {
        val planes = buildOrientedPlanes(brush.faces, ghostFaces)
        val vertices = brush.faces
            .flatMap { buildFaceVertices(brush, it) }
            .distinctBy { Triple(it.x, it.y, it.z) }
        return ConvexHullCollider(planes, vertices)
    }

    fun buildOrientedPlanes(faces: List<MapBrushFace>, ghostFaces: Set<MapBrushFace> = emptySet()): List<Plane> {
        val referencePoint = getBrushReferencePoint(faces)
        return faces.map { toOrientedPlane(it, referencePoint, ghostFaces.contains(it)) }
    }

    private fun toOrientedPlane(face: MapBrushFace, referencePoint: Vector3f, isGhost: Boolean = false): Plane {
        val plane = toPlane(face)
        return if (plane.distanceTo(referencePoint) > 0f) {
            Plane(
                normal = plane.normal.scale(-1f),
                distance = -plane.distance,
                texture = plane.texture,
                isGhost = isGhost,
            )
        } else {
            plane.copy(isGhost = isGhost)
        }
    }

    private fun getBrushReferencePoint(faces: List<MapBrushFace>): Vector3f {
        val points = faces.flatMap { it.points }
        require(points.isNotEmpty()) { "Brush must contain at least one plane point" }
        return points
            .fold(Vector3f()) { acc, point -> acc.add(point) }
            .divide(points.size.toFloat())
    }

    private fun intersect(a: Plane, b: Plane, c: Plane): Vector3f? {
        val bCrossC = b.normal.cross(c.normal)
        val cCrossA = c.normal.cross(a.normal)
        val aCrossB = a.normal.cross(b.normal)
        val denominator = a.normal.dot(bCrossC)

        if (abs(denominator) < 0.0001f) {
            return null
        }

        return bCrossC.scale(a.distance)
            .add(cCrossA.scale(b.distance))
            .add(aCrossB.scale(c.distance))
            .divide(denominator)
    }

    private fun isInsideBrush(point: Vector3f, planes: List<Plane>): Boolean {
        return planes.all { it.distanceTo(point) <= EPSILON }
    }

    private fun sortFaceVertices(vertices: List<Vector3f>, normal: Vector3f): List<Vector3f> {
        if (vertices.size <= 2) {
            return vertices
        }

        val center = vertices
            .fold(Vector3f()) { acc, vertex -> acc.add(vertex) }
            .divide(vertices.size.toFloat())
        val tangent = pickTangent(normal)
        val bitangent = normal.cross(tangent).normalize()

        return vertices.sortedBy { vertex ->
            val relative = vertex.subtract(center)
            atan2(relative.dot(bitangent), relative.dot(tangent))
        }
    }

    private fun pickTangent(normal: Vector3f): Vector3f {
        val up = Vector3f(0f, 1f, 0f)
        val right = Vector3f(1f, 0f, 0f)
        val base = if (abs(normal.dot(up)) < 0.9f) up else right
        return base.cross(normal).normalize()
    }
}
