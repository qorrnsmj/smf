package qorrnsmj.smf.game.progress

import qorrnsmj.smf.math.Vector3f

class GameProgressManager(
    initialStageName: String,
    private val store: GameProgressStore = GameProgressStore(),
) {
    var progress: GameProgress = GameProgress(currentStageName = initialStageName)
        private set

    fun loadOrDefault(): GameProgress {
        progress = store.loadOrNull() ?: progress
        return progress
    }

    fun loadOrNull(): GameProgress? {
        val loadedProgress = store.loadOrNull() ?: return null
        progress = loadedProgress
        return progress
    }

    fun save(): GameProgress {
        store.save(progress)
        return progress
    }

    fun updatePlayerState(position: Vector3f, facing: Vector3f) {
        progress = progress.copy(
            playerPosition = Vector3f(position.x, position.y, position.z),
            playerFacing = Vector3f(facing.x, facing.y, facing.z),
        )
    }

    fun setCurrentStage(stageName: String) {
        progress = progress.copy(currentStageName = stageName)
    }

    fun addFlag(flag: String) {
        progress = progress.copy(flags = progress.flags + flag)
    }

    fun removeFlag(flag: String) {
        progress = progress.copy(flags = progress.flags - flag)
    }

    fun addInventoryItem(itemId: String) {
        progress = progress.copy(inventory = progress.inventory + itemId)
    }

    fun removeInventoryItem(itemId: String) {
        progress = progress.copy(inventory = progress.inventory - itemId)
    }

    fun markMobDefeated(mobId: String) {
        progress = progress.copy(defeatedMobIds = progress.defeatedMobIds + mobId)
    }

    fun isMobDefeated(mobId: String): Boolean {
        return mobId in progress.defeatedMobIds
    }

    fun getSavePath() = store.getSavePath()
}
