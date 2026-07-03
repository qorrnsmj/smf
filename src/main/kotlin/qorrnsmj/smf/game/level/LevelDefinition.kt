package qorrnsmj.smf.game.level

data class LevelDefinition(
    val name: String,
    val resourcePath: String,
    val glbPath: String?,
    val className: String?,
    val renderProfile: String?,
    val entityModels: List<String>,
)
