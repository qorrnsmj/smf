package qorrnsmj.smf.game.map

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.StaticPhysics
import kotlin.math.abs
import kotlin.math.roundToInt

object MapCollisionBuilder {
    private const val WALKABLE_NORMAL_Y = 0.5f
    private const val EDGE_EPSILON = 0.05f

    fun createCollisionEntities(gameMap: GameMap): List<ObjectEntity> {
        val ghostFaces = findGhostFaces(gameMap.brushes)

        return gameMap.brushes.map { brush ->
            ObjectEntity(
                transform = Transform(),
                model = EntityModels.EMPTY,
                physicsComponent = StaticPhysics(
                    collider = MapGeometryBuilder.buildConvexCollider(brush, ghostFaces[brush].orEmpty())
                )
            )
        }
    }

    private fun findGhostFaces(brushes: List<MapBrush>): Map<MapBrush, Set<MapBrushFace>> {
        val records = brushes.flatMap { brush ->
            val planesByFace = brush.faces.zip(MapGeometryBuilder.buildOrientedPlanes(brush.faces)).toMap()
            brush.faces.mapNotNull { face ->
                val vertices = MapGeometryBuilder.buildFaceVertices(brush, face)
                if (vertices.size < 3) {
                    null
                } else {
                    FaceRecord(
                        brush = brush,
                        face = face,
                        vertices = vertices,
                        normal = planesByFace.getValue(face).normal
                    )
                }
            }
        }

        val walkableEdges = records
            .filter { it.normal.y >= WALKABLE_NORMAL_Y }
            .flatMap { record -> record.edges().map { it to record.brush } }

        val ghosts = mutableMapOf<MapBrush, MutableSet<MapBrushFace>>()
        for (record in records) {
            if (record.normal.y >= WALKABLE_NORMAL_Y) {
                continue
            }

            val maxY = record.vertices.maxOf { it.y }
            val hasSharedTopWalkableEdge = record.edges()
                .filter { edge -> edge.a.isAtY(maxY) && edge.b.isAtY(maxY) }
                .any { edge ->
                    walkableEdges.any { (walkableEdge, brush) ->
                        brush !== record.brush && walkableEdge == edge
                    }
                }

            if (hasSharedTopWalkableEdge) {
                ghosts.getOrPut(record.brush) { mutableSetOf() }.add(record.face)
            }
        }

        return ghosts
    }

    private data class FaceRecord(
        val brush: MapBrush,
        val face: MapBrushFace,
        val vertices: List<Vector3f>,
        val normal: Vector3f,
    ) {
        fun edges(): List<EdgeKey> {
            return vertices.indices.map { index ->
                EdgeKey(vertices[index], vertices[(index + 1) % vertices.size])
            }
        }
    }

    private data class EdgeKey(val a: PointKey, val b: PointKey) {
        constructor(first: Vector3f, second: Vector3f) : this(minOf(PointKey.from(first), PointKey.from(second)), maxOf(PointKey.from(first), PointKey.from(second)))
    }

    private data class PointKey(val x: Int, val y: Int, val z: Int) : Comparable<PointKey> {
        override fun compareTo(other: PointKey): Int {
            if (x != other.x) return x.compareTo(other.x)
            if (y != other.y) return y.compareTo(other.y)
            return z.compareTo(other.z)
        }

        fun isAtY(value: Float): Boolean = abs(y * EDGE_EPSILON - value) <= EDGE_EPSILON

        companion object {
            fun from(point: Vector3f): PointKey {
                return PointKey(
                    (point.x / EDGE_EPSILON).roundToInt(),
                    (point.y / EDGE_EPSILON).roundToInt(),
                    (point.z / EDGE_EPSILON).roundToInt(),
                )
            }
        }
    }
}
