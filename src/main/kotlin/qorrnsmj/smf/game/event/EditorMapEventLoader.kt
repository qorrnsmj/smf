package qorrnsmj.smf.game.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import java.nio.file.Files
import java.nio.file.Paths

object EditorMapEventLoader {
    private val objectMapper = ObjectMapper()

    @Suppress("UNCHECKED_CAST")
    fun loadInto(
        scene: Scene,
        player: Player,
        path: String,
        onAreaTriggerEvent: (EventAreaDefinition) -> Unit = {},
    ): List<Task> {
        val root = readJson(path) ?: return emptyList()
        val event = root["event"] as? Map<String, Any?>
        val context = EventLoadContext(scene, player, onAreaTriggerEvent)
        if (event != null) {
            return loadCategory(event, "spawn_points", context) +
                loadCategory(event, "area_triggers", context)
        }

        val legacyAreas = root["event_areas"] as? List<Map<String, Any?>> ?: emptyList()
        return legacyAreas.flatMap { item ->
            EventAreaTypes.load("area_triggers", eventAreaFromJson(item), context)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadCategory(
        event: Map<String, Any?>,
        category: String,
        context: EventLoadContext,
    ): List<Task> {
        val areas = event[category] as? List<Map<String, Any?>> ?: return emptyList()
        return areas.flatMap { item ->
            EventAreaTypes.load(category, eventAreaFromJson(item), context)
        }
    }

    private fun eventAreaFromJson(item: Map<String, Any?>): EventAreaDefinition {
        return EventAreaDefinition(
            name = item["name"] as? String ?: "EventArea",
            id = item["id"] as? String ?: item["name"] as? String ?: "event_area",
            folder = item["folder"] as? String ?: "",
            position = vectorFromJson(item["pos"] ?: item["center"], Vector3f()),
            rotation = vectorFromJson(item["rot"], Vector3f()),
            size = vectorFromJson(item["size"], Vector3f(2f, 1f, 2f)),
            properties = stringMapFromJson(item["properties"]),
        )
    }

    private fun stringMapFromJson(value: Any?): Map<String, String> {
        val map = value as? Map<*, *> ?: return emptyMap()
        return map.mapNotNull { (key, item) ->
            val name = key as? String ?: return@mapNotNull null
            name to item.toString()
        }.toMap()
    }

    private fun vectorFromJson(value: Any?, default: Vector3f): Vector3f {
        val list = value as? List<*> ?: return default
        return Vector3f(
            (list.getOrNull(0) as? Number)?.toFloat() ?: default.x,
            (list.getOrNull(1) as? Number)?.toFloat() ?: default.y,
            (list.getOrNull(2) as? Number)?.toFloat() ?: default.z,
        )
    }

    private fun readJson(path: String): Map<*, *>? {
        val filePath = Paths.get(path)
        return if (Files.isRegularFile(filePath)) {
            objectMapper.readValue(filePath.toFile(), Map::class.java)
        } else {
            val stream = ClassLoader.getSystemResourceAsStream(path)
            if (stream == null) {
                Logger.warn("Editor map event json not found: {}", path)
                null
            } else {
                stream.use { objectMapper.readValue(it, Map::class.java) }
            }
        }
    }
}
