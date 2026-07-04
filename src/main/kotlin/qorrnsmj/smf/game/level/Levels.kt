package qorrnsmj.smf.game.level

import qorrnsmj.smf.game.level.custom.BaseLevel
import qorrnsmj.smf.game.level.custom.Level
import qorrnsmj.smf.game.level.custom.TestLevel

object Levels {
    private val levels: MutableMap<String, LevelEntry> = mutableMapOf()

    val TEST_LEVEL = defineLevel("test", ::TestLevel)

    fun create(levelId: String): Level {
        return levels[levelId]?.create() ?: BaseLevel(levelId)
    }

    private fun defineLevel(levelId: String, create: () -> Level): LevelEntry {
        val entry = LevelEntry(levelId, create)
        levels[levelId] = entry
        return entry
    }

    data class LevelEntry(
        val id: String,
        val create: () -> Level,
    )
}
