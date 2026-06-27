package qorrnsmj.smf.game.map

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.util.ResourceUtils

object MapLoader {
    private val propertyRegex = Regex("^\"([^\"]+)\"\\s+\"([^\"]*)\"$")
    private val faceRegex = Regex(
        "^\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*\\)\\s+" +
            "\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*\\)\\s+" +
            "\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*\\)\\s+" +
            "(\\S+)\\s+" +
            "([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s+" +
            "([-+]?\\d+(?:\\.\\d+)?)\\s+([-+]?\\d+(?:\\.\\d+)?).*$"
    )

    fun load(resourcePath: String): GameMap {
        val parsedMap = parse(resourcePath)
        val meshesByTexture = MapMeshLoader.load(parsedMap.brushes)
        Logger.info("Map loaded: {} brushes, {} entities", parsedMap.brushes.size, parsedMap.entities.size)
        return GameMap(parsedMap = parsedMap, meshesByTexture = meshesByTexture)
    }

    fun parse(resourcePath: String): ParsedMap {
        val lines = ResourceUtils.getResourceAsStream(resourcePath)
            .bufferedReader()
            .readLines()

        val entities = mutableListOf<MapEntity>()
        val worldBrushes = mutableListOf<MapBrush>()
        var index = 0

        while (index < lines.size) {
            val line = lines[index].trim()
            if (line == "{") {
                val result = parseEntity(lines, index + 1)
                if (result.entity.classname == "worldspawn") {
                    worldBrushes.addAll(result.brushes)
                } else {
                    entities.add(result.entity)
                }
                index = result.nextIndex
            } else {
                index++
            }
        }

        return ParsedMap(
            brushes = worldBrushes,
            entities = entities,
        )
    }

    private fun parseEntity(lines: List<String>, startIndex: Int): EntityParseResult {
        val properties = mutableMapOf<String, String>()
        val brushes = mutableListOf<MapBrush>()
        var index = startIndex

        while (index < lines.size) {
            val line = lines[index].trim()

            when {
                line == "}" -> return EntityParseResult(
                    entity = MapEntity(properties),
                    brushes = brushes,
                    nextIndex = index + 1,
                )
                line == "{" -> {
                    val brushResult = parseBrush(lines, index + 1)
                    brushes.add(brushResult.brush)
                    index = brushResult.nextIndex
                }
                propertyRegex.matches(line) -> {
                    val match = propertyRegex.matchEntire(line)!!
                    properties[match.groupValues[1]] = match.groupValues[2]
                    index++
                }
                else -> index++
            }
        }

        error("Unexpected end of map while parsing entity")
    }

    private fun parseBrush(lines: List<String>, startIndex: Int): BrushParseResult {
        val faces = mutableListOf<MapBrushFace>()
        var index = startIndex

        while (index < lines.size) {
            val line = lines[index].trim()

            if (line == "}") {
                return BrushParseResult(
                    brush = MapBrush(faces),
                    nextIndex = index + 1,
                )
            }

            val match = faceRegex.matchEntire(line)
            if (match != null) {
                faces.add(
                    MapBrushFace(
                        points = listOf(
                            mapToEngine(match.groupValues[1], match.groupValues[2], match.groupValues[3]),
                            mapToEngine(match.groupValues[4], match.groupValues[5], match.groupValues[6]),
                            mapToEngine(match.groupValues[7], match.groupValues[8], match.groupValues[9]),
                        ),
                        texture = match.groupValues[10],
                        textureShiftX = match.groupValues[11].toFloat(),
                        textureShiftY = match.groupValues[12].toFloat(),
                        textureRotation = match.groupValues[13].toFloat(),
                        textureScaleX = match.groupValues[14].toFloat(),
                        textureScaleY = match.groupValues[15].toFloat(),
                    )
                )
            }

            index++
        }

        error("Unexpected end of map while parsing brush")
    }

    fun parseOrigin(value: String): qorrnsmj.smf.math.Vector3f {
        val parts = value.trim().split(Regex("\\s+"))
        require(parts.size == 3) { "Invalid origin: $value" }
        return mapToEngine(parts[0], parts[1], parts[2])
    }

    private fun mapToEngine(x: String, y: String, z: String): qorrnsmj.smf.math.Vector3f {
        return qorrnsmj.smf.math.Vector3f(
            x.toFloat(),
            z.toFloat(),
            y.toFloat(),
        )
    }

    private data class EntityParseResult(
        val entity: MapEntity,
        val brushes: List<MapBrush>,
        val nextIndex: Int,
    )

    private data class BrushParseResult(
        val brush: MapBrush,
        val nextIndex: Int,
    )
}
