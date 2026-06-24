package qorrnsmj.smf.game.map

import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.math.Vector3f

data class ParsedMap(
    val brushes: List<MapBrush>,
    val entities: List<MapEntity>,
)

data class GameMap(
    val parsedMap: ParsedMap,
    val meshesByTexture: Map<String, Mesh>,
) {
    val brushes: List<MapBrush> = parsedMap.brushes
    val entities: List<MapEntity> = parsedMap.entities
}

data class MapBrush(
    val faces: List<MapBrushFace>,
) {
    val vertices: List<Vector3f> = MapGeometryBuilder.buildVertices(faces)
    val bounds: MapBounds = MapBounds.fromPoints(vertices)
}

data class MapBrushFace(
    val points: List<Vector3f>,
    val texture: String,
    val textureShiftX: Float,
    val textureShiftY: Float,
    val textureRotation: Float,
    val textureScaleX: Float,
    val textureScaleY: Float,
)

data class MapEntity(
    val properties: Map<String, String>,
) {
    val classname: String = properties["classname"].orEmpty()
}

data class MapBounds(
    val min: Vector3f,
    val max: Vector3f,
) {
    val center: Vector3f
        get() = Vector3f(
            (min.x + max.x) * 0.5f,
            (min.y + max.y) * 0.5f,
            (min.z + max.z) * 0.5f,
        )

    val size: Vector3f
        get() = Vector3f(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z,
        )

    companion object {
        fun fromPoints(points: List<Vector3f>): MapBounds {
            require(points.isNotEmpty()) { "Brush must contain at least one point" }

            var minX = points.first().x
            var minY = points.first().y
            var minZ = points.first().z
            var maxX = minX
            var maxY = minY
            var maxZ = minZ

            for (point in points.drop(1)) {
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                minZ = minOf(minZ, point.z)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
                maxZ = maxOf(maxZ, point.z)
            }

            return MapBounds(
                min = Vector3f(minX, minY, minZ),
                max = Vector3f(maxX, maxY, maxZ),
            )
        }
    }
}
