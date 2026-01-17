package qorrnsmj.smf.state.custom

import qorrnsmj.smf.game.level.test.TestLevel
import qorrnsmj.smf.state.State

class InGameState : State() {
    override fun start() {
        super.start()

        levelManager.loadLevel(TestLevel())
        levelManager.updateTransition()
    }
}
