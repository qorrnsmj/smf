package qorrnsmj.smf.game.progress

import qorrnsmj.smf.math.Vector3f

data class GameProgress(
    val currentStageName: String,
    val playerPosition: Vector3f = Vector3f(),
    val playerFacing: Vector3f = Vector3f(0f, 0f, -1f),
    val flags: Set<String> = emptySet(),
    val inventory: List<String> = emptyList(),
    val defeatedMobIds: Set<String> = emptySet(),
)
