package qorrnsmj.smf.state.custom

import qorrnsmj.smf.game.level.Levels
import qorrnsmj.smf.state.State

class InGameState : State() {
    override fun start() {
        super.start()

        levelManager.loadLevel(Levels.TEST_LEVEL)
    }
}
