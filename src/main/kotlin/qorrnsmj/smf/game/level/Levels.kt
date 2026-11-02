package qorrnsmj.smf.game.level

import qorrnsmj.smf.game.level.custom.BaseLevel
import qorrnsmj.smf.game.level.custom.Level
import qorrnsmj.smf.game.level.custom.test.TestLevel
import qorrnsmj.smf.game.level.custom.meadow.MoonveilMeadowLevel
import qorrnsmj.smf.game.level.custom.meadow.SunwashMeadowLevel

object Levels {
    const val TEST_LEVEL = "test"
    const val SUNWASH_MEADOW = "sunwash-meadow"
    const val MOONVEIL_MEADOW = "moonveil-meadow"

    fun create(levelId: String): Level {
        return when (levelId) {
            TEST_LEVEL -> TestLevel()
            SUNWASH_MEADOW -> SunwashMeadowLevel()
            MOONVEIL_MEADOW -> MoonveilMeadowLevel()
            else -> BaseLevel(levelId)
        }
    }
}
