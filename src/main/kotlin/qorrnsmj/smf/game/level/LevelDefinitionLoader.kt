package qorrnsmj.smf.game.level

import com.fasterxml.jackson.databind.ObjectMapper
import qorrnsmj.smf.util.ResourceUtils

object LevelDefinitionLoader {
    private val objectMapper = ObjectMapper()

    fun load(levelName: String): LevelDefinition {
        val resourcePath = toResourcePath(levelName)
        val root = ResourceUtils.getResourceAsStream(resourcePath).use { stream ->
            objectMapper.readValue(stream, Map::class.java)
        }

        return LevelDefinition(
            name = root.string("name") ?: levelName.removeSuffix(".json").substringAfterLast('/'),
            resourcePath = resourcePath,
            renderProfile = root.string("renderProfile"),
            entityModels = root.stringList("entityModels"),
        )
    }

    fun toResourcePath(levelName: String): String {
        val normalized = levelName.replace('\\', '/')
        return when {
            normalized.endsWith(".json") -> normalized
            normalized.startsWith("assets/level/") -> "$normalized.json"
            else -> "assets/level/$normalized.json"
        }
    }

    private fun Map<*, *>.string(key: String): String? = this[key] as? String

    private fun Map<*, *>.stringList(key: String): List<String> {
        return (this[key] as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
    }
}
