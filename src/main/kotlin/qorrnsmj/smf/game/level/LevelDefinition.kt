package qorrnsmj.smf.game.level

import java.nio.file.Path

data class LevelDefinition(
    val name: String,
    val resourcePath: String,
    val filePath: Path?,
    val workspaceRoot: Path?,
    val renderProfile: String?,
    val entityModels: List<String>,
)
