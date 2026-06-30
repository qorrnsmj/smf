package qorrnsmj.smf.game.level

import qorrnsmj.smf.util.ResourceUtils

object LevelDefinitionLoader {
    fun load(levelName: String): LevelDefinition {
        val resourcePath = toResourcePath(levelName)
        val root = GlbJson.parse(ResourceUtils.getResourceAsStream(resourcePath).readAllBytes().toString(Charsets.UTF_8))
            as? Map<*, *>
            ?: error("Level definition root must be a JSON object: $resourcePath")

        return LevelDefinition(
            name = root.string("name") ?: levelName.removeSuffix(".json").substringAfterLast('/'),
            resourcePath = resourcePath,
            glbPath = root.string("glb") ?: root.string("glbPath"),
            className = root.string("class") ?: root.string("className"),
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
